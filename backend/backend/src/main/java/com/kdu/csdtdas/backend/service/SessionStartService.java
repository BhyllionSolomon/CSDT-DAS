package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.StudentLevelHistoryRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Starting a session moves every active student up exactly one level, and graduates final-year students.
 * A student whose recorded session is more than one session behind, or missing, is never moved: that
 * session record is wrong, and it has to be corrected first.
 */
@Service
@Transactional
public class SessionStartService {

    private static final Logger log = LoggerFactory.getLogger(SessionStartService.class);
    private static final Pattern YEAR = Pattern.compile("^\\s*(\\d{4})");

    public record Row(
            Long studentId, String matricNumber, String fullName, String programme, String fromSession,
            String fromLevel, String toLevel, int toLevelNumber, int steps,
            String action, int outstanding, String note
    ) {}

    public record Stored(String session, int students, int steps, boolean blocked) {}

    public record Preview(
            Long sessionId, String sessionName, int alreadyOnSession, int toMove, int toGraduate, int blocked,
            List<Stored> stored, List<String> warnings, List<Row> rows
    ) {}

    public record Result(int moved, int graduated, int skipped, int blocked, String message) {}

    public record CorrectPreview(String from, String to, int students) {}

    public record Message(String message) {}

    private final StudentRepository studentRepository;
    private final LevelRepository levelRepository;
    private final AcademicSessionRepository sessionRepository;
    private final StudentLevelHistoryRepository historyRepository;
    private final StudentLevelHistoryService historyService;
    private final ResultCalculationService resultCalculationService;
    private final UserRepository userRepository;
    private final SecurityAuditService auditService;

    public SessionStartService(
            StudentRepository studentRepository,
            LevelRepository levelRepository,
            AcademicSessionRepository sessionRepository,
            StudentLevelHistoryRepository historyRepository,
            StudentLevelHistoryService historyService,
            ResultCalculationService resultCalculationService,
            UserRepository userRepository,
            SecurityAuditService auditService
    ) {
        this.studentRepository = studentRepository;
        this.levelRepository = levelRepository;
        this.sessionRepository = sessionRepository;
        this.historyRepository = historyRepository;
        this.historyService = historyService;
        this.resultCalculationService = resultCalculationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ---------------------------------------------------------------- preview

    @Transactional(readOnly = true)
    public Preview preview(Long sessionId) {

        AcademicSession target = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        List<AcademicSession> sessions = ordered();
        Map<Integer, Level> levels = levelsByNumber();

        List<Row> rows = new ArrayList<>();
        Map<String, int[]> stored = new LinkedHashMap<>(); // students, steps, blocked
        int already = 0, toMove = 0, toGraduate = 0, blocked = 0;

        for (Student s : studentRepository.findAll()) {

            if (!"ACTIVE".equalsIgnoreCase(s.getStatus())) continue;
            if (s.getLevel() == null || s.getLevel().getLevelNumber() == null) continue;

            AcademicSession from = s.getAcademicSession();

            if (from != null && from.getId().equals(target.getId())) {
                already++;
                continue;
            }

            int steps = from == null ? 0 : stepsBetween(from, target, sessions);
            if (from != null && steps <= 0) continue; // recorded in a later session than this one: not part of this start

            String fromName = from == null ? "No session recorded" : from.getName();
            String programme = s.getProgramme() != null ? s.getProgramme().getName() : "";
            int current = s.getLevel().getLevelNumber();

            String action, toLevel = "—", note = "";
            int toNumber = 0, outstanding = 0;

            if (from == null) {
                action = "BLOCKED";
                note = "No session is recorded for this student. Place them in their current session first.";
            } else if (steps > 1) {
                action = "BLOCKED";
                note = "Recorded under " + from.getName() + ", which is " + steps + " sessions before " + target.getName()
                        + ". Correct their session first.";
            } else {
                int projected = current + 100;
                boolean graduating = projected > 400;
                Level next = graduating ? null : levels.get(projected);

                if (!graduating && next == null) continue;

                if (graduating) {
                    action = "GRADUATE";
                    toLevel = "Graduated";
                    outstanding = outstandingCount(s.getId());
                    note = outstanding > 0
                            ? "Has " + outstanding + " outstanding course(s): left unticked, so they stay in final year until cleared."
                            : "Finishes the programme.";
                } else {
                    action = "MOVE";
                    toLevel = next.getName();
                    toNumber = projected;
                }
            }

            int[] group = stored.computeIfAbsent(fromName, k -> new int[]{0, steps, 0});
            group[0]++;
            if ("BLOCKED".equals(action)) { group[2] = 1; blocked++; }
            else if ("GRADUATE".equals(action)) toGraduate++;
            else toMove++;

            rows.add(new Row(s.getId(), s.getMatricNumber(), s.getFullName(), programme, fromName,
                    s.getLevel().getName(), toLevel, toNumber, steps, action, outstanding, note));
        }

        rows.sort(Comparator.comparing(Row::programme).thenComparing(Row::fullName));

        List<Stored> storedList = new ArrayList<>();
        stored.forEach((name, g) -> storedList.add(new Stored(name, g[0], g[1], g[2] == 1)));
        storedList.sort(Comparator.comparingInt(Stored::steps));

        return new Preview(target.getId(), target.getName(), already, toMove, toGraduate, blocked,
                storedList, dateWarnings(sessions), rows);
    }

    // ------------------------------------------------------------------ apply

    public Result apply(Long sessionId, List<Long> excludeStudentIds, String actor, String ip) {

        User user = requireHod(actor);

        AcademicSession target = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        Preview preview = preview(sessionId);
        if (!preview.warnings().isEmpty()) {
            throw new IllegalArgumentException("Fix the session dates first. " + String.join(" ", preview.warnings()));
        }

        Set<Long> exclude = excludeStudentIds == null ? Set.of() : new HashSet<>(excludeStudentIds);
        Map<Integer, Level> levels = levelsByNumber();

        int moved = 0, graduated = 0, skipped = 0, blocked = 0;

        for (Row row : preview.rows()) {

            if ("BLOCKED".equals(row.action())) {
                blocked++;
                continue;
            }
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

        String message = moved + " student(s) moved into " + target.getName() + ", " + graduated + " graduated"
                + (skipped > 0 ? ", " + skipped + " left out" : "")
                + (blocked > 0 ? ", " + blocked + " could not be moved because their recorded session is wrong" : "") + ".";

        log.warn("SESSION START for {}: {}", target.getName(), message);
        audit(user, ip, "SESSION_START", target.getName() + ": " + message);

        return new Result(moved, graduated, skipped, blocked, message);
    }

    // ---------------------------------------------------------- correct session

    @Transactional(readOnly = true)
    public CorrectPreview correctPreview(Long fromId, Long toId) {
        AcademicSession from = session(fromId);
        AcademicSession to = session(toId);
        if (from.getId().equals(to.getId())) throw new IllegalArgumentException("Choose two different sessions.");

        int count = (int) studentRepository.findAll().stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .filter(s -> s.getAcademicSession() != null && s.getAcademicSession().getId().equals(from.getId()))
                .count();
        return new CorrectPreview(from.getName(), to.getName(), count);
    }

    /**
     * Fixes a wrong recorded session. The level is never changed. The student's saved record for the old session
     * moves with them, so there is no stray record left behind.
     */
    public Message correctApply(Long fromId, Long toId, String actor, String ip) {
        User user = requireHod(actor);
        AcademicSession from = session(fromId);
        AcademicSession to = session(toId);
        if (from.getId().equals(to.getId())) throw new IllegalArgumentException("Choose two different sessions.");

        int students = 0, records = 0;
        for (Student s : studentRepository.findAll()) {
            if (!"ACTIVE".equalsIgnoreCase(s.getStatus())) continue;
            if (s.getAcademicSession() == null || !s.getAcademicSession().getId().equals(from.getId())) continue;

            s.setAcademicSession(to);
            studentRepository.save(s);
            students++;

            var old = historyRepository.findByStudentIdAndAcademicSessionId(s.getId(), from.getId());
            if (old.isPresent() && historyRepository.findByStudentIdAndAcademicSessionId(s.getId(), to.getId()).isEmpty()) {
                var h = old.get();
                h.setAcademicSession(to);
                h.setNote("Session corrected from " + from.getName() + " to " + to.getName() + " by " + user.getUsername() + ".");
                historyRepository.save(h);
                records++;
            }
        }

        String message = students + " student(s) were moved from " + from.getName() + " to " + to.getName()
                + " as their recorded session (levels unchanged), and " + records + " saved record(s) moved with them.";
        audit(user, ip, "SESSION_CORRECT", message);
        return new Message(message);
    }

    // ---------------------------------------------------------------- helpers

    private List<AcademicSession> ordered() {
        return sessionRepository.findAll().stream()
                .sorted(Comparator.comparing(AcademicSession::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** A session's start date should fall in the year its name begins with, for example 2025/2026 starts in 2025. */
    private List<String> dateWarnings(List<AcademicSession> sessions) {
        List<String> warnings = new ArrayList<>();
        for (AcademicSession s : sessions) {
            if (s.getStartDate() == null) {
                warnings.add("Session " + s.getName() + " has no start date.");
                continue;
            }
            Matcher m = YEAR.matcher(s.getName() == null ? "" : s.getName());
            if (m.find() && Integer.parseInt(m.group(1)) != s.getStartDate().getYear()) {
                warnings.add("The start date of " + s.getName() + " is " + s.getStartDate()
                        + ", which does not match its name. Correct it on the Academic Sessions page.");
            }
        }
        return warnings;
    }

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

    /** How many sessions have started after the student's recorded session, up to and including the new one. */
    private int stepsBetween(AcademicSession from, AcademicSession target, List<AcademicSession> sessions) {
        if (from.getStartDate() == null || target.getStartDate() == null) return 0;

        int steps = 0;
        for (AcademicSession s : sessions) {
            if (s.getStartDate() == null) continue;
            if (s.getStartDate().isAfter(from.getStartDate()) && !s.getStartDate().isAfter(target.getStartDate())) steps++;
        }
        return steps;
    }

    private AcademicSession session(Long id) {
        return sessionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Academic session not found."));
    }

    private void audit(User user, String ip, String action, String detail) {
        auditService.record(user.getUsername(), String.valueOf(user.getRole()).toUpperCase(), ip, action, detail);
    }

    private User requireHod(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found."));
        String role = String.valueOf(user.getRole()).toUpperCase();
        if (!role.equals("HOD") && !role.equals("ADMIN")) throw new IllegalArgumentException("Only the H.O.D or an admin can do this.");
        return user;
    }
}