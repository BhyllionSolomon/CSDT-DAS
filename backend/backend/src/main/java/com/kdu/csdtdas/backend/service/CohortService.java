package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.CohortCheckpoint;
import com.kdu.csdtdas.backend.entity.DirectEntryProfile;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.Result;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.StudentLevelHistory;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CohortCheckpointRepository;
import com.kdu.csdtdas.backend.repository.DirectEntryProfileRepository;
import com.kdu.csdtdas.backend.repository.ResultStatsRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Cohort guidance. A normal student is admitted at 100 level and moves up one level a session, so in session
 * year Y the 100 level is the Y intake, the 200 level the Y-1 intake, and so on. This tells the uploader what to
 * expect, tracks what has really been imported, and checks a list of students against it.
 * Estimated records are assumptions, so they are never counted here.
 */
@Service
@Transactional
public class CohortService {

    /** The department's first admission year. */
    public static final int FIRST_YEAR = 2016;

    private static final Pattern YEAR = Pattern.compile("^\\s*(\\d{4})");
    private static final Pattern MATRIC = Pattern.compile("^[A-Za-z]+(\\d{2})\\d{2,}$");

    public record Cell(
            int sessionYear, String session, Long sessionId, int level,
            int students, int results, String studentsStatus, String resultsStatus
    ) {}

    public record Coverage(int intakeYear, String intake, int cohortSize, List<Cell> cells) {}

    public record VerifyRequest(Integer intakeYear, Integer sessionYear, String kind, Boolean verified) {}

    public record CheckRequest(Long sessionId, Long programmeId, Integer levelNumber, String semester, List<String> matricNumbers) {}

    public record CheckRow(String matric, String name, String category, String reason) {}

    public record CheckReport(
            String session, String semester, String programme, String level, String expectedCohort, String guidance,
            int inFile, int expected, int explained, int unverified, int missing, int notFound,
            boolean ready, List<CheckRow> flagged
    ) {}

    public record Message(String message) {}

    @PersistenceContext
    private EntityManager em;

    private final AcademicSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final StudentLevelHistoryRepository historyRepository;
    private final DirectEntryProfileRepository directRepository;
    private final CohortCheckpointRepository checkpointRepository;
    private final ResultStatsRepository resultRepository;
    private final UserRepository userRepository;

    public CohortService(
            AcademicSessionRepository sessionRepository,
            StudentRepository studentRepository,
            StudentLevelHistoryRepository historyRepository,
            DirectEntryProfileRepository directRepository,
            CohortCheckpointRepository checkpointRepository,
            ResultStatsRepository resultRepository,
            UserRepository userRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.historyRepository = historyRepository;
        this.directRepository = directRepository;
        this.checkpointRepository = checkpointRepository;
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
    }

    // ---------------------------------------------------------------- coverage

    @Transactional(readOnly = true)
    public Coverage coverage(String username, int intakeYear) {
        requireHod(username);

        Map<Integer, AcademicSession> byYear = new HashMap<>();
        for (AcademicSession s : sessionRepository.findAll()) byYear.putIfAbsent(yearOf(s), s);

        Map<Long, List<StudentLevelHistory>> rows = realRowsByStudent();
        Map<Long, Integer> entry = entryLevels();

        Map<Long, Integer> admission = new HashMap<>();
        rows.forEach((id, list) -> {
            Integer y = admissionFromHistory(list, entry.getOrDefault(id, 100));
            if (y != null) admission.put(id, y);
        });

        Map<String, CohortCheckpoint> marks = checkpointRepository.findByIntakeYear(intakeYear).stream()
                .collect(Collectors.toMap(c -> c.getSessionYear() + "|" + c.getKind(), c -> c, (a, b) -> a));

        List<Cell> cells = new ArrayList<>();
        int cohortSize = 0;

        for (int j = 0; j < 4; j++) {
            int sy = intakeYear + j;
            AcademicSession session = byYear.get(sy);
            int students = 0, results = 0;

            if (session != null) {
                Set<Long> present = new HashSet<>();
                for (Map.Entry<Long, List<StudentLevelHistory>> e : rows.entrySet()) {
                    Integer a = admission.get(e.getKey());
                    if (a == null || a != intakeYear) continue;
                    boolean inSession = e.getValue().stream().anyMatch(h -> h.getAcademicSession().getId().equals(session.getId()));
                    if (inSession) present.add(e.getKey());
                }
                students = present.size();

                Set<Long> withResults = new HashSet<>();
                for (Result r : resultRepository.findApprovedForSession(session.getId())) {
                    if (present.contains(r.getStudent().getId())) withResults.add(r.getStudent().getId());
                }
                results = withResults.size();
            }

            if (j == 0) cohortSize = students;

            cells.add(new Cell(sy, session == null ? full(sy) : session.getName(), session == null ? null : session.getId(),
                    100 * (j + 1), students, results,
                    status(marks, sy, "STUDENTS", students), status(marks, sy, "RESULTS", results)));
        }

        return new Coverage(intakeYear, full(intakeYear), cohortSize, cells);
    }

    public Message verify(String username, VerifyRequest req) {
        User user = requireHod(username);
        if (req.intakeYear() == null || req.sessionYear() == null) throw bad("Choose a cohort and a session.");

        String kind = "RESULTS".equalsIgnoreCase(req.kind()) ? "RESULTS" : "STUDENTS";
        var existing = checkpointRepository.findByIntakeYearAndSessionYearAndKind(req.intakeYear(), req.sessionYear(), kind);

        if (Boolean.TRUE.equals(req.verified())) {
            if (existing.isEmpty()) {
                CohortCheckpoint c = new CohortCheckpoint();
                c.setIntakeYear(req.intakeYear());
                c.setSessionYear(req.sessionYear());
                c.setKind(kind);
                c.setVerifiedBy(user.getUsername());
                checkpointRepository.save(c);
            }
            return new Message("Marked as verified.");
        }

        existing.ifPresent(checkpointRepository::delete);
        return new Message("Verification removed.");
    }

    // ------------------------------------------------------------------- check

    @Transactional(readOnly = true)
    public CheckReport check(String username, CheckRequest req) {
        requireHod(username);

        AcademicSession session = sessionRepository.findById(req.sessionId()).orElseThrow(() -> bad("Choose an academic session."));
        Programme programme = req.programmeId() == null ? null : em.find(Programme.class, req.programmeId());
        if (programme == null) throw bad("Choose a programme.");

        int level = req.levelNumber() == null ? 0 : req.levelNumber();
        if (level < 100 || level > 400 || level % 100 != 0) throw bad("Choose a level from 100 to 400.");

        Level levelEntity = em.createQuery("SELECT l FROM Level l WHERE l.levelNumber = :n", Level.class)
                .setParameter("n", level).getResultStream().findFirst().orElse(null);
        String levelName = levelEntity != null ? levelEntity.getName() : level + " Level";

        int sy = yearOf(session);
        int normal = sy - (level / 100 - 1);

        String expectedCohort = normal < FIRST_YEAR ? null : full(normal);
        String guidance = expectedCohort == null
                ? "No cohort normally occupies " + levelName + " in " + session.getName() + ": it would have been admitted before the department started in " + FIRST_YEAR + "."
                : "EXPECTED COHORT: Students admitted in " + expectedCohort + " would normally occupy " + levelName + " in " + session.getName() + ".";

        Map<String, Student> byMatric = new HashMap<>();
        for (Student s : studentRepository.findAllWithDetails()) byMatric.put(norm(s.getMatricNumber()), s);

        Map<Long, List<StudentLevelHistory>> rows = realRowsByStudent();
        Map<Long, Integer> entry = entryLevels();

        Set<String> matrics = new LinkedHashSet<>();
        if (req.matricNumbers() != null) for (String m : req.matricNumbers()) if (m != null && !m.isBlank()) matrics.add(norm(m));

        int expected = 0, explained = 0, unverified = 0, missing = 0, notFound = 0;
        List<CheckRow> flagged = new ArrayList<>();

        for (String m : matrics) {
            Student s = byMatric.get(m);
            if (s == null) {
                notFound++;
                flagged.add(new CheckRow(m, "", "NOT_FOUND", "This matriculation number is not in the database. Upload the student list first, or correct the number."));
                continue;
            }

            List<StudentLevelHistory> history = rows.getOrDefault(s.getId(), List.of());
            int entryLevel = entry.getOrDefault(s.getId(), 100);

            // Conflicts with what is already on file are never accepted quietly.
            if (s.getProgramme() != null && !s.getProgramme().getId().equals(programme.getId())) {
                unverified++;
                flagged.add(new CheckRow(m, s.getFullName(), "UNVERIFIED",
                        "Recorded in " + s.getProgramme().getName() + ", not " + programme.getName() + ". Check the programme before importing."));
                continue;
            }
            StudentLevelHistory same = history.stream().filter(h -> h.getAcademicSession().getId().equals(session.getId())).findFirst().orElse(null);
            if (same != null && !Objects.equals(same.getLevel().getLevelNumber(), level)) {
                unverified++;
                flagged.add(new CheckRow(m, s.getFullName(), "UNVERIFIED",
                        "Already recorded in " + session.getName() + " at " + same.getLevel().getName() + ", not " + levelName + "."));
                continue;
            }

            Integer admission = admissionFromHistory(history, entryLevel);
            String basis = "the records on file";
            if (admission == null) {
                admission = matricYear(m);
                basis = "the matric number";
            }

            if (admission == null) {
                missing++;
                flagged.add(new CheckRow(m, s.getFullName(), "MISSING",
                        "No admission information: no earlier record, and the year cannot be read from the matric number."));
                continue;
            }

            int adm = admission;
            boolean repeated = adm < normal && history.stream().anyMatch(h ->
                    Objects.equals(h.getLevel().getLevelNumber(), level) && !h.getAcademicSession().getId().equals(session.getId()));

            if (adm == normal) {
                expected++;
            } else if (entryLevel > 100 && adm == sy - (level - entryLevel) / 100) {
                explained++;
                flagged.add(new CheckRow(m, s.getFullName(), "EXPLAINED", "Direct entry student (entered at " + entryLevel + " level)."));
            } else if (repeated) {
                String other = history.stream()
                        .filter(h -> Objects.equals(h.getLevel().getLevelNumber(), level) && !h.getAcademicSession().getId().equals(session.getId()))
                        .map(h -> h.getAcademicSession().getName()).findFirst().orElse("another session");
                explained++;
                flagged.add(new CheckRow(m, s.getFullName(), "EXPLAINED", "Repeated this level: also recorded at " + levelName + " in " + other + "."));
            } else {
                unverified++;
                flagged.add(new CheckRow(m, s.getFullName(), "UNVERIFIED",
                        "COHORT MISMATCH: admission year " + adm + " (from " + basis + "), but the " + (expectedCohort == null ? "normal" : expectedCohort)
                                + " cohort normally occupies " + levelName + " in " + session.getName()
                                + ". Verify whether this student repeated a level, has an incorrect admission record, or belongs to another level."));
            }
        }

        boolean ready = !matrics.isEmpty() && unverified == 0 && missing == 0 && notFound == 0;

        return new CheckReport(session.getName(), req.semester() == null ? "" : req.semester(), programme.getName(), levelName,
                expectedCohort == null ? "" : expectedCohort, guidance, matrics.size(), expected, explained, unverified, missing, notFound,
                ready, flagged);
    }

    // ---------------------------------------------------------------- helpers

    private Map<Long, List<StudentLevelHistory>> realRowsByStudent() {
        return historyRepository.findAllWithDetails().stream()
                .filter(h -> !"ESTIMATED".equals(h.getSource()))
                .collect(Collectors.groupingBy(h -> h.getStudent().getId()));
    }

    private Map<Long, Integer> entryLevels() {
        Map<Long, Integer> map = new HashMap<>();
        for (DirectEntryProfile d : directRepository.findAll()) map.put(d.getStudent().getId(), d.getEntryLevelNumber());
        return map;
    }

    /** The year a student was admitted, worked out from their earliest real record and their entry level. */
    private Integer admissionFromHistory(List<StudentLevelHistory> list, int entryLevel) {
        StudentLevelHistory first = list.stream()
                .filter(h -> h.getLevel().getLevelNumber() != null)
                .min(Comparator.comparingInt(h -> yearOf(h.getAcademicSession())))
                .orElse(null);
        if (first == null) return null;
        return yearOf(first.getAcademicSession()) - (first.getLevel().getLevelNumber() - entryLevel) / 100;
    }

    private Integer matricYear(String matric) {
        Matcher m = MATRIC.matcher(matric);
        if (!m.matches()) return null;
        int year = 2000 + Integer.parseInt(m.group(1));
        return year >= FIRST_YEAR && year <= 2100 ? year : null;
    }

    private String status(Map<String, CohortCheckpoint> marks, int sessionYear, String kind, int count) {
        if (marks.containsKey(sessionYear + "|" + kind)) return "COMPLETE";
        return count > 0 ? "IMPORTED" : "PENDING";
    }

    private int yearOf(AcademicSession s) {
        Matcher m = YEAR.matcher(s.getName() == null ? "" : s.getName());
        if (m.find()) return Integer.parseInt(m.group(1));
        return s.getStartDate() != null ? s.getStartDate().getYear() : 0;
    }

    private String full(int year) {
        return year + "/" + (year + 1);
    }

    private String norm(String matric) {
        return matric == null ? "" : matric.trim().toUpperCase();
    }

    private User requireHod(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        String role = String.valueOf(user.getRole()).toUpperCase();
        if (!role.equals("HOD") && !role.equals("ADMIN")) throw bad("Only the H.O.D or an admin can use the cohort guide.");
        return user;
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}