package com.kdu.csdtdas.backend.service;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * One-time, guarded repair of the audited CSDT student-session baseline.
 * Reassigns the 201 existing students from 2026/2027 to 2025/2026 without
 * changing their current level, programme, department or identity.
 * Future promotion remains an explicit administrator action.
 */
@Component
@ConditionalOnProperty(name = "app.baseline-repair.enabled", havingValue = "true", matchIfMissing = true)
public class AcademicSessionBaselineRepairRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AcademicSessionBaselineRepairRunner.class);
    private static final String BASELINE = "2025/2026";
    private static final String MISASSIGNED = "2026/2027";
    private static final String ESTIMATED = "2024/2025";
    private static final String MARKER = "BASELINE_REPAIR_2025_2026_COMPLETED";
    private static final long EXPECTED = 201L;

    private final EntityManager em;
    private final TransactionTemplate transaction;

    public AcademicSessionBaselineRepairRunner(EntityManager em, PlatformTransactionManager manager) {
        this.em = em;
        this.transaction = new TransactionTemplate(manager);
    }

    @Override
    public void run(ApplicationArguments args) {
        transaction.executeWithoutResult(status -> repair());
    }

    private void repair() {
        Long baselineId = sessionId(BASELINE);
        Long misplacedId = sessionId(MISASSIGNED);
        Long estimatedId = sessionId(ESTIMATED);

        long markerCount = scalar(
                "SELECT COUNT(*) FROM student_level_history WHERE academic_session_id = :sid AND note = :marker",
                baselineId, MARKER);

        if (markerCount == EXPECTED) {
            log.info("Academic baseline repair already completed; no changes made.");
            return;
        }
        if (markerCount != 0) {
            throw new IllegalStateException("Partial baseline-repair marker detected. No changes made.");
        }

        long baselineStudents = countStudents(baselineId);
        long misplacedStudents = countStudents(misplacedId);
        long baselineHistory = countHistory(baselineId);
        long misplacedHistory = countHistory(misplacedId);
        long distinctMisplaced = scalar(
                "SELECT COUNT(DISTINCT student_id) FROM student_level_history WHERE academic_session_id = :sid",
                misplacedId, null);
        long blankSource = scalar(
                "SELECT COUNT(*) FROM student_level_history WHERE academic_session_id = :sid " +
                "AND (source IS NULL OR TRIM(source) = '')", misplacedId, null);
        long mismatches = scalar(
                "SELECT COUNT(*) FROM student_level_history h JOIN students s ON s.id = h.student_id " +
                "WHERE h.academic_session_id = :sid AND (h.level_id <> s.level_id OR h.programme_id <> s.programme_id)",
                misplacedId, null);

        if (baselineStudents != 0 || misplacedStudents != EXPECTED
                || baselineHistory != 0 || misplacedHistory != EXPECTED
                || distinctMisplaced != EXPECTED || blankSource != EXPECTED || mismatches != 0) {
            throw new IllegalStateException(
                    "Repair stopped safely: database does not match audited state. " +
                    "2025/2026 students=" + baselineStudents + ", 2026/2027 students=" + misplacedStudents +
                    ", 2025/2026 history=" + baselineHistory + ", 2026/2027 history=" + misplacedHistory +
                    ", distinct history students=" + distinctMisplaced + ", blank source=" + blankSource +
                    ", level/programme mismatches=" + mismatches);
        }

        int estimatedDeleted = em.createNativeQuery(
                "DELETE FROM student_level_history WHERE academic_session_id = :sid " +
                "AND UPPER(TRIM(COALESCE(source, ''))) = 'ESTIMATED'")
                .setParameter("sid", estimatedId).executeUpdate();

        int oldHistoryDeleted = em.createNativeQuery(
                "DELETE FROM student_level_history WHERE academic_session_id = :sid")
                .setParameter("sid", misplacedId).executeUpdate();
        if (oldHistoryDeleted != EXPECTED) {
            throw new IllegalStateException("Expected to delete 201 misplaced history rows; deleted " +
                    oldHistoryDeleted + ". Rolling back.");
        }

        int reassigned = em.createNativeQuery(
                "UPDATE students SET academic_session_id = :baseline WHERE academic_session_id = :misplaced")
                .setParameter("baseline", baselineId).setParameter("misplaced", misplacedId).executeUpdate();
        if (reassigned != EXPECTED) {
            throw new IllegalStateException("Expected to reassign 201 students; reassigned " +
                    reassigned + ". Rolling back.");
        }

        int snapshots = em.createNativeQuery(
                "INSERT INTO student_level_history " +
                "(student_id, academic_session_id, level_id, programme_id, recorded_at, source, status, note) " +
                "SELECT id, academic_session_id, level_id, programme_id, CURRENT_TIMESTAMP, 'SYNCED', status, :marker " +
                "FROM students WHERE academic_session_id = :baseline")
                .setParameter("baseline", baselineId).setParameter("marker", MARKER).executeUpdate();

        if (snapshots != EXPECTED || countStudents(baselineId) != EXPECTED
                || countStudents(misplacedId) != 0 || countHistory(baselineId) != EXPECTED
                || countHistory(misplacedId) != 0) {
            throw new IllegalStateException("Final baseline verification failed. Transaction rolled back.");
        }

        log.info("BASELINE REPAIR COMPLETE: reassigned={}, snapshots={}, old history deleted={}, " +
                "2024/2025 estimated history deleted={}", reassigned, snapshots, oldHistoryDeleted, estimatedDeleted);
    }

    private Long sessionId(String name) {
        Object value = em.createNativeQuery("SELECT id FROM academic_sessions WHERE name = :name")
                .setParameter("name", name).getSingleResult();
        return ((Number) value).longValue();
    }

    private long countStudents(Long id) {
        return scalar("SELECT COUNT(*) FROM students WHERE academic_session_id = :sid", id, null);
    }

    private long countHistory(Long id) {
        return scalar("SELECT COUNT(*) FROM student_level_history WHERE academic_session_id = :sid", id, null);
    }

    private long scalar(String sql, Long id, String marker) {
        var query = em.createNativeQuery(sql).setParameter("sid", id);
        if (marker != null) query.setParameter("marker", marker);
        return ((Number) query.getSingleResult()).longValue();
    }
}
