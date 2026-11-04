package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CumulativeSummaryDTO;
import com.kdu.csdtdas.backend.dto.ProjectDTOs;
import com.kdu.csdtdas.backend.dto.SemesterResultDTO;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.ProjectSupervision;
import com.kdu.csdtdas.backend.entity.StaffProfile;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.ProjectSupervisionRepository;
import com.kdu.csdtdas.backend.repository.StaffProfileRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectSupervisionService {

    public static final List<String> DESIGNATIONS = List.of(
            "HEAD_OF_DEPARTMENT", "PROFESSOR", "ASSOCIATE_PROFESSOR", "SENIOR_LECTURER",
            "LECTURER_I", "LECTURER_II", "ASSISTANT_LECTURER", "ADJUNCT"
    );
    private static final Set<String> SENIOR = Set.of("PROFESSOR", "ASSOCIATE_PROFESSOR", "SENIOR_LECTURER");
    private static final Set<String> STAFF_ROLES = Set.of("LECTURER", "ADJUNCT", "LEVEL_ADVISER", "HOD");

    private record Ranked(Student student, String cls, BigDecimal cgpa) {}

    /** One supervisor while a proposal is being worked out. */
    private static final class Sup {
        final User user;
        final StaffProfile profile;
        final int tier;
        int total;
        int existing;
        final Map<String, Integer> byClass = new HashMap<>();
        final List<Ranked> proposed = new ArrayList<>();

        Sup(User user, StaffProfile profile, int tier) {
            this.user = user;
            this.profile = profile;
            this.tier = tier;
        }

        void take(Ranked r, String cls) {
            proposed.add(r);
            total++;
            byClass.merge(cls, 1, Integer::sum);
        }
    }

    private final ProjectSupervisionRepository repository;
    private final StaffProfileRepository profileRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AcademicSessionRepository sessionRepository;
    private final ResultCalculationService calculationService;
    private final RegistrationClearanceService clearanceService;

    public ProjectSupervisionService(
            ProjectSupervisionRepository repository,
            StaffProfileRepository profileRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            AcademicSessionRepository sessionRepository,
            ResultCalculationService calculationService,
            RegistrationClearanceService clearanceService
    ) {
        this.repository = repository;
        this.profileRepository = profileRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.calculationService = calculationService;
        this.clearanceService = clearanceService;
    }

    // ---------------------------------------------------------- staff settings

    public List<ProjectDTOs.StaffRow> staff(String username) {
        requireHod(username);

        AcademicSession session = clearanceService.currentSession();
        Map<Long, Integer> load = new HashMap<>();
        if (session != null) {
            for (ProjectSupervision p : repository.findForSession(session.getId())) {
                load.merge(p.getSupervisor().getId(), 1, Integer::sum);
            }
        }

        List<ProjectDTOs.StaffRow> rows = new ArrayList<>();
        for (User u : candidates()) {
            StaffProfile p = profileFor(u);
            rows.add(new ProjectDTOs.StaffRow(
                    u.getId(), u.getFullName(), u.getRole(), p.getDesignation(),
                    p.isSupervisionEligible(), p.isProjectCoordinator(), p.isAssistantCoordinator(),
                    load.getOrDefault(u.getId(), 0)
            ));
        }
        return rows;
    }

    public List<ProjectDTOs.StaffRow> saveStaff(String username, List<ProjectDTOs.StaffUpdate> updates) {
        requireHod(username);
        if (updates == null) throw bad("Nothing to save.");

        if (updates.stream().filter(u -> Boolean.TRUE.equals(u.coordinator())).count() > 1) {
            throw bad("Only one lecturer can be the Project Coordinator.");
        }
        if (updates.stream().filter(u -> Boolean.TRUE.equals(u.assistantCoordinator())).count() > 1) {
            throw bad("Only one lecturer can be the Assistant Project Coordinator.");
        }

        Map<Long, User> byId = candidates().stream().collect(Collectors.toMap(User::getId, u -> u));

        for (ProjectDTOs.StaffUpdate up : updates) {
            User u = byId.get(up.userId());
            if (u == null) continue;

            String designation = up.designation() == null ? "LECTURER_II" : up.designation().trim().toUpperCase();
            if (!DESIGNATIONS.contains(designation)) throw bad("Unknown designation: " + up.designation());

            StaffProfile p = profileFor(u);
            p.setDesignation(designation);
            p.setSupervisionEligible(Boolean.TRUE.equals(up.eligible()));
            p.setProjectCoordinator(Boolean.TRUE.equals(up.coordinator()));
            p.setAssistantCoordinator(Boolean.TRUE.equals(up.assistantCoordinator()));
            profileRepository.save(p);
        }

        return staff(username);
    }

    // --------------------------------------------------------------- proposal

    public ProjectDTOs.Proposal propose(String username, Long sessionId) {
        requireHod(username);
        AcademicSession session = session(sessionId);

        List<Sup> sups = supervisors(session.getId());
        if (sups.isEmpty()) {
            throw bad("No lecturer is marked as a project supervisor yet. Open the supervisor settings and tick the lecturers who supervise.");
        }

        List<Ranked> students = eligibleStudents(session.getId()).stream().map(this::rank).toList();
        List<String> notes = new ArrayList<>();

        if (students.isEmpty()) {
            notes.add("There are no unassigned 400 level students for this session.");
        }
        long unranked = students.stream().filter(r -> r.cgpa() == null).count();
        if (unranked > 0) {
            notes.add(unranked + " student(s) have no approved results yet, so they are treated as ordinary students.");
        }

        allocate(students, sups);

        List<ProjectDTOs.SupervisorGroup> groups = new ArrayList<>();
        for (Sup s : sups) {
            int first = (int) s.proposed.stream().filter(r -> r.cls().equals("FIRST")).count();
            int upper = (int) s.proposed.stream().filter(r -> r.cls().equals("UPPER")).count();

            groups.add(new ProjectDTOs.SupervisorGroup(
                    s.user.getId(), s.user.getFullName(), s.profile.getDesignation(),
                    s.profile.isProjectCoordinator(), s.profile.isAssistantCoordinator(),
                    s.existing, s.proposed.size(), first, upper, s.proposed.size() - first - upper,
                    s.proposed.stream().map(this::line).toList()
            ));
        }
        groups.sort(Comparator.comparing(g -> g.fullName().toLowerCase()));

        return new ProjectDTOs.Proposal(
                session.getId(), session.getName(), students.size(), sups.size(),
                (int) students.stream().filter(r -> r.cls().equals("FIRST")).count(),
                (int) students.stream().filter(r -> r.cls().equals("UPPER")).count(),
                (int) students.stream().filter(r -> r.cls().equals("OTHER")).count(),
                groups, notes
        );
    }

    /**
     * First Class, then Upper Second, then everyone else.
     * For a class: whoever holds the fewest of that class goes next, then the lowest total workload,
     * then the higher-priority supervisor (head, senior or coordinator), then name.
     * For everyone else: lowest total workload, then priority, then name.
     */
    private void allocate(List<Ranked> students, List<Sup> sups) {

        Comparator<Ranked> strongest = Comparator
                .comparing((Ranked r) -> r.cgpa() == null ? BigDecimal.valueOf(-1) : r.cgpa(), Comparator.<BigDecimal>reverseOrder())
                .thenComparing(r -> r.student().getMatricNumber());

        for (String cls : List.of("FIRST", "UPPER")) {
            List<Ranked> group = students.stream().filter(r -> r.cls().equals(cls)).sorted(strongest).toList();
            for (Ranked r : group) {
                Sup chosen = sups.stream().min(Comparator
                        .comparingInt((Sup s) -> s.byClass.getOrDefault(cls, 0))
                        .thenComparingInt(s -> s.total)
                        .thenComparingInt(s -> s.tier)
                        .thenComparing(s -> s.user.getFullName().toLowerCase())).orElseThrow();
                chosen.take(r, cls);
            }
        }

        List<Ranked> rest = students.stream().filter(r -> r.cls().equals("OTHER")).sorted(strongest).toList();
        for (Ranked r : rest) {
            Sup chosen = sups.stream().min(Comparator
                    .comparingInt((Sup s) -> s.total)
                    .thenComparingInt(s -> s.tier)
                    .thenComparing(s -> s.user.getFullName().toLowerCase())).orElseThrow();
            chosen.take(r, "OTHER");
        }
    }

    // ---------------------------------------------------------------- confirm

    /** Saves the whole allocation or none of it. */
    public ProjectDTOs.Message confirm(String username, ProjectDTOs.ConfirmRequest req) {
        requireHod(username);
        if (req == null || req.assignments() == null || req.assignments().isEmpty()) {
            throw bad("There is nothing to confirm.");
        }

        AcademicSession session = session(req.sessionId());
        Map<Long, Student> students = eligibleStudents(session.getId()).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, User> sups = supervisors(session.getId()).stream()
                .collect(Collectors.toMap(s -> s.user.getId(), s -> s.user));

        Set<Long> seen = new HashSet<>();
        List<ProjectSupervision> toSave = new ArrayList<>();

        for (ProjectDTOs.Pair pair : req.assignments()) {
            if (pair.studentId() == null || pair.supervisorId() == null) throw bad("The request is incomplete.");
            if (!seen.add(pair.studentId())) throw bad("A student appears more than once in this allocation.");

            Student s = students.get(pair.studentId());
            if (s == null) {
                throw bad("A student in this list is no longer an unassigned 400 level student. Please generate the proposal again.");
            }
            User sup = sups.get(pair.supervisorId());
            if (sup == null) {
                throw bad("A supervisor in this list is no longer eligible. Please generate the proposal again.");
            }

            Ranked r = rank(s);

            ProjectSupervision p = new ProjectSupervision();
            p.setStudent(s);
            p.setSupervisor(sup);
            p.setAcademicSession(session);
            p.setClassification(r.cls());
            p.setCgpa(r.cgpa());
            p.setStatus("ASSIGNED");
            p.setAssignedAt(LocalDateTime.now());
            p.setAssignedBy(username);
            toSave.add(p);
        }

        try {
            repository.saveAll(toSave);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw bad("Some students were assigned while you were reviewing. Please generate the proposal again.");
        }

        return new ProjectDTOs.Message(toSave.size() + " student(s) assigned to supervisors for " + session.getName() + ".");
    }

    public List<ProjectDTOs.AssignmentRow> all(String username, Long sessionId) {
        requireHod(username);
        AcademicSession session = session(sessionId);

        return repository.findForSession(session.getId()).stream().map(p -> new ProjectDTOs.AssignmentRow(
                p.getId(), p.getStudent().getId(), p.getStudent().getMatricNumber(), p.getStudent().getFullName(),
                p.getStudent().getProgramme() != null ? p.getStudent().getProgramme().getName() : "",
                p.getClassification(), p.getCgpa(),
                p.getSupervisor().getId(), p.getSupervisor().getFullName(),
                profileFor(p.getSupervisor()).getDesignation(),
                p.getStatus(), p.getAcademicSession().getName(), p.getAssignedAt()
        )).toList();
    }

    public void remove(String username, Long id) {
        requireHod(username);
        ProjectSupervision p = repository.findById(id).orElseThrow(() -> bad("Assignment not found."));
        repository.delete(p);
    }

    // ------------------------------------------------------------------ views

    /** A student sees only their own supervisor. */
    public ProjectDTOs.MyAssignment mine(String username) {
        User user = getUser(username);
        Student student = user.getStudent();
        if (student == null) throw bad("Your account is not linked to a student record.");

        List<ProjectSupervision> list = repository.findForStudent(student.getId());
        if (list.isEmpty()) {
            return new ProjectDTOs.MyAssignment(false, null, null, null, null, null, null, null);
        }

        ProjectSupervision p = list.get(0);
        User sup = p.getSupervisor();

        return new ProjectDTOs.MyAssignment(
                true, sup.getFullName(), profileFor(sup).getDesignation(), p.getStatus(),
                p.getAcademicSession().getName(), p.getAssignedAt(), sup.getEmail(), sup.getPhoneNumber()
        );
    }

    /** A supervisor sees the students assigned to them this session. */
    public List<ProjectDTOs.MyStudent> myStudents(String username) {
        User user = getUser(username);
        AcademicSession session = clearanceService.currentSession();
        if (session == null) return List.of();

        return repository.findForSession(session.getId()).stream()
                .filter(p -> p.getSupervisor().getId().equals(user.getId()))
                .map(p -> new ProjectDTOs.MyStudent(
                        p.getStudent().getId(), p.getStudent().getMatricNumber(), p.getStudent().getFullName(),
                        p.getStudent().getProgramme() != null ? p.getStudent().getProgramme().getName() : "",
                        p.getClassification(), p.getAcademicSession().getName(), p.getAssignedAt()
                ))
                .toList();
    }

    // ---------------------------------------------------------------- helpers

    private List<Student> eligibleStudents(Long sessionId) {
        Set<Long> assigned = repository.findForSession(sessionId).stream()
                .map(p -> p.getStudent().getId())
                .collect(Collectors.toSet());

        return studentRepository.findAll().stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .filter(s -> s.getLevel() != null && Integer.valueOf(400).equals(s.getLevel().getLevelNumber()))
                .filter(s -> !assigned.contains(s.getId()))
                .toList();
    }

    /** Uses the CGPA engine's own classification for the student's latest cumulative result. */
    private Ranked rank(Student s) {
        try {
            List<SemesterResultDTO> history = calculationService.calculateFullAcademicHistory(s.getId());
            if (history.isEmpty()) return new Ranked(s, "OTHER", null);

            CumulativeSummaryDTO c = history.get(history.size() - 1).currentCumulative();
            String label = c.degreeClass() == null ? "" : c.degreeClass().toUpperCase();

            String cls = label.contains("FIRST") ? "FIRST" : label.contains("UPPER") ? "UPPER" : "OTHER";
            return new Ranked(s, cls, c.cgpa());
        } catch (RuntimeException e) {
            return new Ranked(s, "OTHER", null);
        }
    }

    private ProjectDTOs.StudentLine line(Ranked r) {
        return new ProjectDTOs.StudentLine(
                r.student().getId(), r.student().getMatricNumber(), r.student().getFullName(),
                r.student().getProgramme() != null ? r.student().getProgramme().getName() : "",
                r.cls(), r.cgpa()
        );
    }

    /** Eligible supervisors, with the students they already have this session. */
    private List<Sup> supervisors(Long sessionId) {
        Map<Long, List<ProjectSupervision>> existing = repository.findForSession(sessionId).stream()
                .collect(Collectors.groupingBy(p -> p.getSupervisor().getId()));

        List<Sup> out = new ArrayList<>();
        for (User u : candidates()) {
            StaffProfile p = profileFor(u);
            if (!p.isSupervisionEligible()) continue;

            Sup s = new Sup(u, p, tierOf(p));
            for (ProjectSupervision e : existing.getOrDefault(u.getId(), List.of())) {
                s.existing++;
                s.total++;
                s.byClass.merge(e.getClassification(), 1, Integer::sum);
            }
            out.add(s);
        }
        return out;
    }

    /** 0 = Head of Department, 1 = senior lecturer or coordinator, 2 = everyone else. */
    private int tierOf(StaffProfile p) {
        if ("HEAD_OF_DEPARTMENT".equals(p.getDesignation())) return 0;
        if (SENIOR.contains(p.getDesignation()) || p.isProjectCoordinator() || p.isAssistantCoordinator()) return 1;
        return 2;
    }

    private List<User> candidates() {
        return userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .filter(u -> STAFF_ROLES.contains(String.valueOf(u.getRole()).toUpperCase()))
                .sorted(Comparator.comparing((User u) -> u.getFullName().toLowerCase()))
                .toList();
    }

    private StaffProfile profileFor(User u) {
        return profileRepository.findByUserId(u.getId()).orElseGet(() -> {
            StaffProfile p = new StaffProfile();
            p.setUser(u);
            String role = String.valueOf(u.getRole()).toUpperCase();
            p.setSupervisionEligible(role.equals("LECTURER") || role.equals("LEVEL_ADVISER"));
            return profileRepository.save(p);
        });
    }

    private AcademicSession session(Long id) {
        if (id != null) {
            return sessionRepository.findById(id).orElseThrow(() -> bad("Academic session not found."));
        }
        AcademicSession current = clearanceService.currentSession();
        if (current == null) throw bad("No academic session has been set up yet.");
        return current;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
    }

    private void requireHod(String username) {
        String role = String.valueOf(getUser(username).getRole()).toUpperCase();
        if (!role.equals("HOD") && !role.equals("ADMIN")) throw bad("Only the H.O.D can do this.");
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}