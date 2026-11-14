package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
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
 * Moves the students recorded in one session into the next session, one level up. 400-level students graduate.
 * You choose both sessions, so nothing depends on how old a student's record is.
 */
@Service
@Transactional
public class PromotionService {

    private static final Pattern YEAR = Pattern.compile("^\\s*(\\d{4})");

    public record Line(String programme, String fromLevel, String toLevel, String action, int count) {}

    public record Row(
            Long studentId, String matricNumber, String fullName, String programme,
            String fromLevel, String toLevel, int toLevelNumber, String action, int outstanding, String note
    ) {}

    public record Preview(
            String from, String to, int total, int toMove, int toGraduate,
            List<Line> lines, List<Row> rows, List<String> warnings, List<String> elsewhere
    ) {}

    public record Result(int moved, int graduated, int skipped, String message) {}

    private final StudentRepository studentRepository;
    private final LevelRepository levelRepository;
    private final AcademicSessionRepository sessionRepository;
    private final StudentLevelHistoryService historyService;
    private final ResultCalculationService calculationService;
    private final UserRepository userRepository;
    private final SecurityAuditService auditService;

    public PromotionService(
            StudentRepository studentRepository,
            LevelRepository levelRepository,
            AcademicSessionRepository sessionRepository,
            StudentLevelHistoryService historyService,
            ResultCalculationService calculationService,
            UserRepository userRepository,
            SecurityAuditService auditService
    ) {
        this.studentRepository = studentRepository;
        this.levelRepository = levelRepository;
        this.sessionRepository = sessionRepository;
        this.historyService = historyService;
        this.calculationService = calculationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Preview preview(String actor, Long fromId, Long toId) {
        requireHod(actor);
        return build(fromId, toId);
    }

    public Result apply(String actor, String ip, Long fromId, Long toId, List<Long> excludeStudentIds) {
        User user = requireHod(actor);

        Preview plan = build(fromId, toId);
        if (!plan.warnings().isEmpty()) throw new IllegalArgumentException(String.join(" ", plan.warnings()));

        AcademicSession to = session(toId);
        Set<Long> exclude = excludeStudentIds == null ? Set.of() : new HashSet<>(excludeStudentIds);
        Map<Integer, Level> levels = levels();

        int moved = 0, graduated = 0, skipped = 0;

        for (Row row : plan.rows()) {
            if (exclude.contains(row.studentId())) { skipped++; continue; }

            Student s = studentRepository.findById(row.studentId()).orElse(null);
            if (s == null) { skipped++; continue; }

            historyService.recordSnapshot(s);              // keeps the session they are leaving on record

            if ("GRADUATE".equals(row.action())) {
                s.setStatus("GRADUATED");
                studentRepository.save(s);
                graduated++;
                continue;
            }

            Level next = levels.get(row.toLevelNumber());
            if (next == null) { skipped++; continue; }

            s.setLevel(next);
            s.setAcademicSession(to);
            studentRepository.save(s);

            historyService.recordSnapshot(s);              // records the new level for the new session
            moved++;
        }

        String message = moved + " student(s) moved into " + to.getName() + " and " + graduated + " graduated"
                + (skipped > 0 ? ", " + skipped + " left behind" : "") + ".";
        auditService.record(user.getUsername(), String.valueOf(user.getRole()).toUpperCase(), ip, "SESSION_PROMOTE",
                plan.from() + " to " + plan.to() + ": " + message);

        return new Result(moved, graduated, skipped, message);
    }

    // ---------------------------------------------------------------- helpers

    private Preview build(Long fromId, Long toId) {
        AcademicSession from = session(fromId);
        AcademicSession to = session(toId);
        if (from.getId().equals(to.getId())) throw new IllegalArgumentException("Choose two different sessions.");

        List<String> warnings = new ArrayList<>();
        List<AcademicSession> ordered = sessionRepository.findAll().stream()
                .sorted(Comparator.comparing(AcademicSession::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int fi = -1, ti = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(from.getId())) fi = i;
            if (ordered.get(i).getId().equals(to.getId())) ti = i;
        }
        if (ti <= fi) {
            warnings.add("The session you are moving into (" + to.getName() + ") must start after the session students are in now (" + from.getName() + ").");
        } else if (ti - fi > 1) {
            warnings.add("There is a session between " + from.getName() + " and " + to.getName() + " (" + ordered.get(fi + 1).getName()
                    + "). Move students one session at a time.");
        }
        warnings.addAll(dateWarning(from));
        warnings.addAll(dateWarning(to));

        Map<Integer, Level> levels = levels();
        List<Row> rows = new ArrayList<>();
        Map<String, Line> lines = new LinkedHashMap<>();
        Map<String, Integer> elsewhere = new LinkedHashMap<>();
        int toMove = 0, toGraduate = 0;

        for (Student s : studentRepository.findAll()) {
            if (!"ACTIVE".equalsIgnoreCase(s.getStatus())) continue;

            AcademicSession recorded = s.getAcademicSession();
            if (recorded == null || !recorded.getId().equals(from.getId())) {
                elsewhere.merge(recorded == null ? "no session" : recorded.getName(), 1, Integer::sum);
                continue;
            }
            if (s.getLevel() == null || s.getLevel().getLevelNumber() == null) continue;

            int projected = s.getLevel().getLevelNumber() + 100;
            boolean graduating = projected > 400;
            Level next = graduating ? null : levels.get(projected);
            if (!graduating && next == null) continue;

            String programme = s.getProgramme() != null ? s.getProgramme().getName() : "";
            int outstanding = graduating ? outstanding(s.getId()) : 0;

            String action = graduating ? "GRADUATE" : "MOVE";
            String toLevel = graduating ? "Graduated" : next.getName();
            String note = graduating
                    ? (outstanding > 0 ? "Has " + outstanding + " outstanding course(s). Left unticked, so they stay in final year until cleared." : "Finishes the programme.")
                    : "";

            rows.add(new Row(s.getId(), s.getMatricNumber(), s.getFullName(), programme,
                    s.getLevel().getName(), toLevel, graduating ? 0 : projected, action, outstanding, note));

            String key = programme + "|" + s.getLevel().getName() + "|" + toLevel;
            Line old = lines.get(key);
            lines.put(key, new Line(programme, s.getLevel().getName(), toLevel, action, old == null ? 1 : old.count() + 1));

            if (graduating) toGraduate++; else toMove++;
        }

        rows.sort(Comparator.comparing(Row::programme).thenComparing(Row::fullName));
        List<Line> lineList = new ArrayList<>(lines.values());
        lineList.sort(Comparator.comparing(Line::programme).thenComparing(Line::fromLevel));

        List<String> notes = new ArrayList<>();
        elsewhere.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> notes.add(e.getValue() + " active student(s) are recorded under " + e.getKey() + ". They are not touched."));

        if (rows.isEmpty() && warnings.isEmpty()) {
            warnings.add("No active student is recorded in " + from.getName() + ". Place your students in it first (Students Per Session, step 2).");
        }

        return new Preview(from.getName(), to.getName(), rows.size(), toMove, toGraduate, lineList, rows, warnings, notes);
    }

    private List<String> dateWarning(AcademicSession s) {
        if (s.getStartDate() == null) return List.of(s.getName() + " has no start date. Add one on the Academic Sessions page.");
        Matcher m = YEAR.matcher(s.getName() == null ? "" : s.getName());
        if (m.find() && Integer.parseInt(m.group(1)) != s.getStartDate().getYear()) {
            return List.of("The start date of " + s.getName() + " is " + s.getStartDate() + ", which does not match its name. Correct it on the Academic Sessions page.");
        }
        return List.of();
    }

    private int outstanding(Long studentId) {
        try {
            return calculationService.getOutstandingCourses(studentId).size();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private Map<Integer, Level> levels() {
        Map<Integer, Level> map = new HashMap<>();
        for (Level l : levelRepository.findAll()) if (l.getLevelNumber() != null) map.putIfAbsent(l.getLevelNumber(), l);
        return map;
    }

    private AcademicSession session(Long id) {
        return sessionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Academic session not found."));
    }

    private User requireHod(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found."));
        String role = String.valueOf(user.getRole()).toUpperCase();
        if (!role.equals("HOD") && !role.equals("ADMIN")) throw new IllegalArgumentException("Only the H.O.D or an admin can do this.");
        return user;
    }
}