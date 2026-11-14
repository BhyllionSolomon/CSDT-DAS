package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.DirectEntryProfile;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.StudentLevelHistory;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.DirectEntryProfileRepository;
import com.kdu.csdtdas.backend.repository.StudentLevelHistoryRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps one record per student per academic session. Records are only ever added, or filled in where empty.
 * Nothing here deletes or rewrites another session's record, except undoing estimates, which only removes
 * rows marked ESTIMATED.
 */
@Service
@Transactional
public class SessionRecordsService {

    public record SessionLine(Long sessionId, String name, int studentsInSession, int records, int missing) {}

    public record Check(
            int students, int active, int graduated, int noSession, int noLevel, int noProgramme,
            List<SessionLine> sessions, List<String> findings
    ) {}

    public record SyncReport(int created, int updated, int unchanged, int skipped, Map<String, Integer> reasons) {}

    public record EstimateLine(String session, int rows) {}

    public record EstimateReport(boolean applied, int rows, List<EstimateLine> sessions, List<String> notes) {}

    public record Row(
            Long studentId, String matricNumber, String fullName, String department,
            String programme, String level, String status, String source, String note
    ) {}

    public record Message(String message) {}

    @PersistenceContext
    private EntityManager em;

    private final StudentRepository studentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final StudentLevelHistoryRepository historyRepository;
    private final DirectEntryProfileRepository directRepository;
    private final UserRepository userRepository;
    private final SecurityAuditService auditService;

    public SessionRecordsService(
            StudentRepository studentRepository,
            AcademicSessionRepository sessionRepository,
            StudentLevelHistoryRepository historyRepository,
            DirectEntryProfileRepository directRepository,
            UserRepository userRepository,
            SecurityAuditService auditService
    ) {
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.historyRepository = historyRepository;
        this.directRepository = directRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ check

    @Transactional(readOnly = true)
    public Check check(String username) {
        requireHod(username);

        List<Student> students = studentRepository.findAllWithDetails();
        List<StudentLevelHistory> rows = historyRepository.findAllWithDetails();
        Set<String> have = rows.stream().map(h -> key(h.getStudent().getId(), h.getAcademicSession().getId())).collect(Collectors.toSet());

        int noSession = 0, noLevel = 0, noProgramme = 0, active = 0, graduated = 0;
        for (Student s : students) {
            if (s.getAcademicSession() == null) noSession++;
            if (s.getLevel() == null) noLevel++;
            if (s.getProgramme() == null) noProgramme++;
            if ("ACTIVE".equalsIgnoreCase(s.getStatus())) active++;
            if ("GRADUATED".equalsIgnoreCase(s.getStatus())) graduated++;
        }

        List<SessionLine> lines = new ArrayList<>();
        int missingTotal = 0;
        for (AcademicSession session : sessionsNewestFirst()) {
            int inSession = 0, missing = 0;
            for (Student s : students) {
                if (s.getAcademicSession() == null || !s.getAcademicSession().getId().equals(session.getId())) continue;
                inSession++;
                if (!have.contains(key(s.getId(), session.getId()))) missing++;
            }
            int records = (int) rows.stream().filter(h -> h.getAcademicSession().getId().equals(session.getId())).count();
            missingTotal += missing;
            lines.add(new SessionLine(session.getId(), session.getName(), inSession, records, missing));
        }

        List<String> findings = new ArrayList<>();
        if (students.isEmpty()) {
            findings.add("There are no students in the database yet. Upload a roster first.");
        } else {
            if (noSession > 0) findings.add(noSession + " student(s) have no academic session, so they cannot appear in any session. Use step 2.");
            if (noLevel > 0) findings.add(noLevel + " student(s) have no level. Fix them on the All Students page.");
            if (noProgramme > 0) findings.add(noProgramme + " student(s) have no programme. Fix them on the All Students page.");
            if (missingTotal > 0) findings.add(missingTotal + " student(s) have a current session but no saved record yet. Run step 3.");
            if (findings.isEmpty()) findings.add("Everything is in order. Every student has a session, a level, a programme and a saved record.");
        }

        return new Check(students.size(), active, graduated, noSession, noLevel, noProgramme, lines, findings);
    }

    // --------------------------------------------------------- assign session

    /** Gives a session to students that have none. Students who already have one are never changed. */
    public Message assignSession(String username, String ip, Long sessionId) {
        User user = requireHod(username);
        AcademicSession session = sessionRepository.findById(sessionId).orElseThrow(() -> bad("Academic session not found."));

        int changed = 0;
        for (Student s : studentRepository.findAllWithDetails()) {
            if (s.getAcademicSession() != null) continue;
            s.setAcademicSession(session);
            studentRepository.save(s);
            changed++;
        }

        audit(user, ip, "SESSION_ASSIGN", changed + " student(s) without a session were placed in " + session.getName());
        return new Message(changed + " student(s) were placed in " + session.getName() + ". Now save their session records (step 3).");
    }

    // ------------------------------------------------------------------- sync

    public SyncReport sync(String username, String ip) {
        User user = requireHod(username);

        Map<String, StudentLevelHistory> existing = new HashMap<>();
        for (StudentLevelHistory h : historyRepository.findAllWithDetails()) {
            existing.put(key(h.getStudent().getId(), h.getAcademicSession().getId()), h);
        }

        int created = 0, updated = 0, unchanged = 0, skipped = 0;
        Map<String, Integer> reasons = new LinkedHashMap<>();

        for (Student s : studentRepository.findAllWithDetails()) {
            String why = null;
            if (s.getAcademicSession() == null) why = "No academic session";
            else if (s.getLevel() == null) why = "No level";
            else if (s.getProgramme() == null) why = "No programme";

            if (why != null) {
                skipped++;
                reasons.merge(why, 1, Integer::sum);
                continue;
            }

            StudentLevelHistory h = existing.get(key(s.getId(), s.getAcademicSession().getId()));

            if (h == null) {
                h = new StudentLevelHistory();
                h.setStudent(s);
                h.setAcademicSession(s.getAcademicSession());
                h.setLevel(s.getLevel());
                h.setProgramme(s.getProgramme());
                h.setStatus(s.getStatus());
                h.setSource("SYNCED");
                historyRepository.save(h);
                created++;
                continue;
            }

            boolean same = h.getLevel().getId().equals(s.getLevel().getId())
                    && h.getProgramme().getId().equals(s.getProgramme().getId())
                    && Objects.equals(h.getStatus(), s.getStatus());

            if (same) {
                unchanged++;
            } else {
                h.setLevel(s.getLevel());
                h.setProgramme(s.getProgramme());
                h.setStatus(s.getStatus());
                if (h.getSource() == null || "ESTIMATED".equals(h.getSource())) h.setSource("SYNCED");
                historyRepository.save(h);
                updated++;
            }
        }

        audit(user, ip, "SESSION_SYNC", created + " created, " + updated + " updated, " + unchanged + " unchanged, " + skipped + " skipped");
        return new SyncReport(created, updated, unchanged, skipped, reasons);
    }

    // --------------------------------------------------------------- estimate

    /**
     * Works out earlier sessions for students whose current record is known: one session back means one level
     * down. It only uses sessions that exist and follow each other, never goes below a direct-entry student's
     * entry level, and marks every row it creates as ESTIMATED. With apply=false nothing is written.
     */
    public EstimateReport estimate(String username, String ip, boolean apply) {
        User user = requireHod(username);

        List<AcademicSession> ordered = sessionRepository.findAll().stream()
                .sorted(Comparator.comparing(AcademicSession::getStartDate)).toList();
        Map<Long, Integer> position = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) position.put(ordered.get(i).getId(), i);

        Map<Integer, Level> levels = new HashMap<>();
        for (Level l : em.createQuery("SELECT l FROM Level l", Level.class).getResultList()) {
            if (l.getLevelNumber() != null) levels.putIfAbsent(l.getLevelNumber(), l);
        }

        Map<Long, Integer> entry = new HashMap<>();
        for (DirectEntryProfile d : directRepository.findAll()) entry.put(d.getStudent().getId(), d.getEntryLevelNumber());

        List<StudentLevelHistory> rows = historyRepository.findAllWithDetails();
        Set<String> have = rows.stream().map(h -> key(h.getStudent().getId(), h.getAcademicSession().getId())).collect(Collectors.toCollection(HashSet::new));

        Map<String, Integer> perSession = new LinkedHashMap<>();
        List<StudentLevelHistory> toCreate = new ArrayList<>();
        int blocked = 0;

        for (StudentLevelHistory h : rows) {
            if ("ESTIMATED".equals(h.getSource())) continue;
            Integer levelNumber = h.getLevel().getLevelNumber();
            Integer index = position.get(h.getAcademicSession().getId());
            if (levelNumber == null || index == null) continue;

            int floor = Math.max(100, entry.getOrDefault(h.getStudent().getId(), 100));
            int startYear = h.getAcademicSession().getStartDate().getYear();

            for (int back = 1; ; back++) {
                int targetLevel = levelNumber - 100 * back;
                if (targetLevel < floor) break;

                int targetIndex = index - back;
                boolean exists = targetIndex >= 0
                        && ordered.get(targetIndex).getStartDate().getYear() == startYear - back;
                if (!exists) { blocked++; break; }

                AcademicSession target = ordered.get(targetIndex);
                String k = key(h.getStudent().getId(), target.getId());
                if (have.contains(k)) continue;

                Level level = levels.get(targetLevel);
                if (level == null) { blocked++; break; }

                StudentLevelHistory est = new StudentLevelHistory();
                est.setStudent(h.getStudent());
                est.setAcademicSession(target);
                est.setLevel(level);
                est.setProgramme(h.getProgramme());
                est.setSource("ESTIMATED");
                est.setNote("Estimated from " + h.getAcademicSession().getName() + " (" + h.getLevel().getName() + "), assuming no repeat or break.");
                toCreate.add(est);
                have.add(k);
                perSession.merge(target.getName(), 1, Integer::sum);
            }
        }

        if (apply && !toCreate.isEmpty()) {
            historyRepository.saveAll(toCreate);
            audit(user, ip, "SESSION_ESTIMATE", toCreate.size() + " estimated session record(s) created");
        }

        List<String> notes = new ArrayList<>();
        notes.add("These are estimates. They assume each student moved up one level every session, with no repeat, deferment or break.");
        if (blocked > 0) notes.add(blocked + " earlier record(s) could not be worked out because the earlier session does not exist, or does not follow on. Create the missing sessions on the Academic Sessions page and run this again.");

        List<EstimateLine> lines = perSession.entrySet().stream()
                .map(e -> new EstimateLine(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(EstimateLine::session))
                .toList();

        return new EstimateReport(apply, toCreate.size(), lines, notes);
    }

    /** Removes the estimated rows of one session. Records that came from live data are never touched. */
    public Message undoEstimate(String username, String ip, Long sessionId) {
        User user = requireHod(username);
        AcademicSession session = sessionRepository.findById(sessionId).orElseThrow(() -> bad("Academic session not found."));

        List<StudentLevelHistory> estimated = historyRepository.findByAcademicSessionId(sessionId).stream()
                .filter(h -> "ESTIMATED".equals(h.getSource())).toList();
        historyRepository.deleteAll(estimated);

        audit(user, ip, "SESSION_ESTIMATE_UNDO", estimated.size() + " estimated record(s) removed from " + session.getName());
        return new Message(estimated.size() + " estimated record(s) were removed from " + session.getName() + ".");
    }

    // ------------------------------------------------------------------- view

    @Transactional(readOnly = true)
    public List<Row> forSession(String username, Long sessionId) {
        requireHod(username);

        return historyRepository.findByAcademicSessionId(sessionId).stream().map(h -> new Row(
                h.getStudent().getId(), h.getStudent().getMatricNumber(), h.getStudent().getFullName(),
                h.getStudent().getDepartment() != null ? h.getStudent().getDepartment().getName() : "",
                h.getProgramme().getName(), h.getLevel().getName(), h.getStatus(),
                h.getSource() == null ? "SYNCED" : h.getSource(), h.getNote()
        )).toList();
    }

    // ---------------------------------------------------------------- helpers

    private List<AcademicSession> sessionsNewestFirst() {
        return sessionRepository.findAll().stream()
                .sorted(Comparator.comparing(AcademicSession::getStartDate).reversed()).toList();
    }

    private String key(Long studentId, Long sessionId) {
        return studentId + "-" + sessionId;
    }

    private void audit(User user, String ip, String action, String detail) {
        auditService.record(user.getUsername(), String.valueOf(user.getRole()).toUpperCase(), ip, action, detail);
    }

    private User requireHod(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        String role = String.valueOf(user.getRole()).toUpperCase();
        if (!role.equals("HOD") && !role.equals("ADMIN")) throw bad("Only the H.O.D or an admin can manage session records.");
        return user;
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}