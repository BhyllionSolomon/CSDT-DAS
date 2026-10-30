package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class SessionStartService {

    private static final Logger log = LoggerFactory.getLogger(SessionStartService.class);

    public record Row(
            Long studentId, String matricNumber, String fullName, String programme,
            String fromLevel, String toLevel, int toLevelNumber, int steps,
            String action, int outstanding, String note
    ) {}

    public record Preview(
            Long sessionId, String sessionName,
            int alreadyOnSession, int toMove, int toGraduate, int noSessionRecorded,
            List<Row> rows
    ) {}

    public record Result(int moved, int graduated, int skipped, String message) {}

    private final StudentRepository studentRepository;
    private final LevelRepository levelRepository;
    private final AcademicSessionRepository sessionRepository;
    private final StudentLevelHistoryService historyService;
    private final ResultCalculationService resultCalculationService;

    public SessionStartService(
            StudentRepository studentRepository,
            LevelRepository levelRepository,
            AcademicSessionRepository sessionRepository,
            StudentLevelHistoryService historyService,
            ResultCalculationService resultCalculationService
    ) {
        this.studentRepository = studentRepository;
        this.levelRepository = levelRepository;
        this.sessionRepository = sessionRepository;
        this.historyService = historyService;
        this.resultCalculationService = resultCalculationService;
    }

    @Transactional(readOnly = true)
    public Preview preview(Long sessionId) {

        AcademicSession target = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        List<AcademicSession> sessions = sessionRepository.findAll();
        Map<Integer, Level> levels = levelsByNumber();

        List<Row> rows = new ArrayList<>();
        int already = 0, none = 0, toMove = 0, toGraduate = 0;

        for (Student s : studentRepository.findAll()) {

            if (!"ACTIVE".equalsIgnoreCase(s.getStatus())) continue;
            if (s.getLevel() == null || s.getLevel().getLevelNumber() == null) continue;

            AcademicSession from = s.getAcademicSession();

            if (from != null && from.getId().equals(target.getId())) {
                already++;
                continue;
            }

            int steps = stepsBetween(from, target, sessions);
            if (steps <= 0) continue; // the stored session is not earlier than this one

            int current = s.getLevel().getLevelNumber();
            int projected = current + 100 * steps;
            boolean graduating = projected > 400;

            Level nextLevel = graduating ? null : levels.get(projected);
            if (!graduating && nextLevel == null) continue;

            int outstanding = graduating ? outstandingCount(s.getId()) : 0;

            String note = "";
            if (from == null) {
                none++;
                note = "No session on record: assumed one year behind. Check this one.";
            } else if (graduating) {
                note = outstanding > 0
                        ? "Has " + outstanding + " outstanding course(s): left unticked, so they stay in final year until cleared."
                        : "Finishes the programme.";
            } else if (steps > 1) {
                note = "Stored at " + from.getName() + ": moved " + steps + " levels.";
            }

            if (graduating) toGraduate++; else toMove++;

            rows.add(new Row(
                    s.getId(), s.getMatricNumber(), s.getFullName(),
                    s.getProgramme() != null ? s.getProgramme().getName() : "",
                    s.getLevel().getName(),
                    graduating ? "Graduated" : nextLevel.getName(),
                    graduating ? 0 : projected,
                    steps,
                    graduating ? "GRADUATE" : "MOVE",
                    outstanding,
                    note
            ));
        }

        rows.sort(Comparator.comparing(Row::programme).thenComparing(Row::fullName));

        return new Preview(target.getId(), target.getName(), already, toMove, toGraduate, none, rows);
    }

    public Result apply(Long sessionId, List<Long> excludeStudentIds, String actor) {

        AcademicSession target = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        Set<Long> exclude = excludeStudentIds == null ? Set.of() : new HashSet<>(excludeStudentIds);
        Map<Integer, Level> levels = levelsByNumber();

        int moved = 0, graduated = 0, skipped = 0;

        for (Row row : preview(sessionId).rows()) {

            if (exclude.contains(row.studentId())) {
                skipped++;
                continue;
            }

            Student s = studentRepository.findById(row.studentId()).orElse(null);
            if (s == null) {
                skipped++;
                continue;
            }

            historyService.recordSnapshot(s);          // keeps the previous session's level on record

            if ("GRADUATE".equals(row.action())) {
                s.setStatus("GRADUATED");
                studentRepository.save(s);
                graduated++;
                continue;
            }

            Level next = levels.get(row.toLevelNumber());
            if (next == null) {
                skipped++;
                continue;
            }

            s.setLevel(next);
            s.setAcademicSession(target);
            studentRepository.save(s);

            historyService.recordSnapshot(s);          // records the new level for the new session
            moved++;
        }

        log.warn("SESSION START for {}: {} moved, {} graduated, {} left out, by {}",
                target.getName(), moved, graduated, skipped, actor);

        return new Result(moved, graduated, skipped,
                moved + " student(s) moved into " + target.getName() + ", " + graduated + " graduated"
                        + (skipped > 0 ? ", " + skipped + " left out." : "."));
    }

    // ---------------------------------------------------------------- helpers

    private int outstandingCount(Long studentId) {
        try {
            return resultCalculationService.getOutstandingCourses(studentId).size();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private Map<Integer, Level> levelsByNumber() {
        Map<Integer, Level> map = new HashMap<>();
        for (Level l : levelRepository.findAll()) {
            if (l.getLevelNumber() != null) map.putIfAbsent(l.getLevelNumber(), l);
        }
        return map;
    }

    /** How many sessions have started after the student's stored session, up to and including the new one. */
    private int stepsBetween(AcademicSession from, AcademicSession target, List<AcademicSession> sessions) {

        if (from == null || from.getStartDate() == null || target.getStartDate() == null) return 1;

        int steps = 0;
        for (AcademicSession s : sessions) {
            if (s.getStartDate() == null) continue;
            if (s.getStartDate().isAfter(from.getStartDate()) && !s.getStartDate().isAfter(target.getStartDate())) {
                steps++;
            }
        }
        return steps;
    }
}