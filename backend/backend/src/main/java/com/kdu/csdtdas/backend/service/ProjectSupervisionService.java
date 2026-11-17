package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CumulativeSummaryDTO;
import com.kdu.csdtdas.backend.dto.ProjectDTOs;
import com.kdu.csdtdas.backend.dto.SemesterResultDTO;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.ProjectSupervision;
import com.kdu.csdtdas.backend.entity.ReportedCgpa;
import com.kdu.csdtdas.backend.entity.ProjectAllocationDraft;
import com.kdu.csdtdas.backend.entity.ProjectAllocationDraftItem;
import com.kdu.csdtdas.backend.entity.ProjectAllocationEvent;
import com.kdu.csdtdas.backend.entity.StaffProfile;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.ProjectSupervisionRepository;
import com.kdu.csdtdas.backend.repository.ReportedCgpaRepository;
import com.kdu.csdtdas.backend.repository.ProjectAllocationDraftRepository;
import com.kdu.csdtdas.backend.repository.ProjectAllocationDraftItemRepository;
import com.kdu.csdtdas.backend.repository.ProjectAllocationEventRepository;
import com.kdu.csdtdas.backend.repository.StaffProfileRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import com.kdu.csdtdas.backend.util.DegreeClassUtil;
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
    private final ReportedCgpaRepository reportedCgpaRepository;
    private final ProjectAllocationDraftRepository draftRepository;
    private final ProjectAllocationDraftItemRepository draftItemRepository;
    private final ProjectAllocationEventRepository eventRepository;
    private final StaffProfileRepository profileRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AcademicSessionRepository sessionRepository;
    private final ResultCalculationService calculationService;
    private final RegistrationClearanceService clearanceService;

    public ProjectSupervisionService(
            ProjectSupervisionRepository repository,
            ReportedCgpaRepository reportedCgpaRepository,
            ProjectAllocationDraftRepository draftRepository,
            ProjectAllocationDraftItemRepository draftItemRepository,
            ProjectAllocationEventRepository eventRepository,
            StaffProfileRepository profileRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            AcademicSessionRepository sessionRepository,
            ResultCalculationService calculationService,
            RegistrationClearanceService clearanceService
    ) {
        this.repository = repository;
        this.reportedCgpaRepository = reportedCgpaRepository;
        this.draftRepository = draftRepository;
        this.draftItemRepository = draftItemRepository;
        this.eventRepository = eventRepository;
        this.profileRepository = profileRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.calculationService = calculationService;
        this.clearanceService = clearanceService;
    }

    // ---------------------------------------------------------- staff settings

    public List<ProjectDTOs.StaffRow> staff(String username) {
        requireProjectManager(username);

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
                    p.isSupervisionEligible(), p.isFirstClassEligible(), p.isProjectCoordinator(), p.isAssistantCoordinator(),
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
            p.setFirstClassEligible(Boolean.TRUE.equals(up.firstClassEligible()) && Boolean.TRUE.equals(up.eligible()));
            p.setProjectCoordinator(Boolean.TRUE.equals(up.coordinator()));
            p.setAssistantCoordinator(Boolean.TRUE.equals(up.assistantCoordinator()));
            profileRepository.save(p);
        }

        return staff(username);
    }

    // --------------------------------------------------------------- proposal

    public ProjectDTOs.Proposal propose(String username, Long sessionId) {
        AcademicSession session = session(sessionId);
        return buildProposal(username, session, eligibleStudents(session.getId()), true);
    }

    /**
     * A draft is a complete proposed allocation, not just an allocation for
     * students who have never been assigned. Rebuild the full active 400-level
     * cohort so updated CGPAs can correct earlier supervisor allocations.
     */
    private ProjectDTOs.Proposal proposeFullCohort(String username, Long sessionId) {
        AcademicSession session = session(sessionId);
        return buildProposal(username, session, allocationStudents(session.getId()), false);
    }

    private ProjectDTOs.Proposal buildProposal(
            String username, AcademicSession session, List<Student> cohort, boolean includeExistingLoad
    ) {
        requireProjectManager(username);

        List<Sup> sups = supervisors(session.getId());
        if (sups.isEmpty()) {
            throw bad("No lecturer is marked as a project supervisor yet. Open the supervisor settings and tick the lecturers who supervise.");
        }

        // When rebuilding the whole cohort, previous assignments are being
        // reconsidered; they must not count as extra load in the new balancing.
        if (!includeExistingLoad) {
            for (Sup supervisor : sups) {
                supervisor.total = 0;
                supervisor.byClass.clear();
            }
        }

        List<Ranked> students = cohort.stream()
                .map(student -> rank(student, session.getId())).toList();
        List<String> notes = new ArrayList<>();

        if (students.isEmpty()) {
            notes.add("There are no active 400 level students to allocate for this session.");
        }
        long unranked = students.stream().filter(r -> r.cgpa() == null).count();
        if (unranked > 0) {
            notes.add(unranked + " student(s) have no matching uploaded CGPA or approved result history, so they are treated as unclassified students.");
        }

        allocate(students, sups);

        List<ProjectDTOs.SupervisorGroup> groups = new ArrayList<>();
        for (Sup supervisor : sups) {
            int first = (int) supervisor.proposed.stream().filter(r -> r.cls().equals("FIRST")).count();
            int upper = (int) supervisor.proposed.stream().filter(r -> r.cls().equals("UPPER")).count();

            groups.add(new ProjectDTOs.SupervisorGroup(
                    supervisor.user.getId(), supervisor.user.getFullName(), supervisor.profile.getDesignation(),
                    supervisor.profile.isProjectCoordinator(), supervisor.profile.isAssistantCoordinator(),
                    supervisor.existing, supervisor.proposed.size(), first, upper,
                    supervisor.proposed.size() - first - upper,
                    supervisor.proposed.stream().map(this::line).toList()
            ));
        }
        groups.sort(Comparator.comparing(g -> g.fullName().toLowerCase()));

        return new ProjectDTOs.Proposal(
                session.getId(), session.getName(), students.size(), sups.size(),
                (int) students.stream().filter(r -> r.cls().equals("FIRST")).count(),
                (int) students.stream().filter(r -> r.cls().equals("UPPER")).count(),
                (int) students.stream().filter(r -> !r.cls().equals("FIRST") && !r.cls().equals("UPPER")).count(),
                groups, notes
        );
    }

    /**
     * First Class students use strict round-robin across the H.O.D-selected subset.
     * Remaining classifications are handled in academic order and assigned to the
     * currently least-loaded eligible supervisor.
     */
    private void allocate(List<Ranked> students, List<Sup> sups) {
        Comparator<Ranked> strongest = Comparator
                .comparing((Ranked r) -> r.cgpa() == null ? BigDecimal.valueOf(-1) : r.cgpa(),
                        Comparator.<BigDecimal>reverseOrder())
                .thenComparing(r -> r.student().getMatricNumber());

        List<Sup> firstEligible = sups.stream().filter(s -> s.profile.isFirstClassEligible()).toList();
        List<Ranked> firsts = students.stream().filter(r -> r.cls().equals("FIRST")).sorted(strongest).toList();
        if (!firsts.isEmpty() && firstEligible.isEmpty()) {
            throw bad("First Class students are available, but the H.O.D has not selected any First Class-eligible supervisors. Update supervisor settings first.");
        }
        // Strict round-robin: every selected supervisor receives one before anyone receives a second.
        for (Ranked r : firsts) {
            Sup chosen = firstEligible.stream().min(Comparator
                    .comparingInt((Sup s) -> s.byClass.getOrDefault("FIRST", 0))
                    .thenComparing(s -> s.user.getFullName().toLowerCase())).orElseThrow();
            chosen.take(r, "FIRST");
        }

        // Remaining classes are processed in order and balanced across all supervisors.
        for (String cls : List.of("UPPER", "LOWER", "THIRD", "FAIL", "OTHER")) {
            List<Ranked> group = students.stream().filter(r -> r.cls().equals(cls)).sorted(strongest).toList();
            for (Ranked r : group) {
                Sup chosen = sups.stream().min(Comparator
                        .comparingInt((Sup s) -> s.total)
                        .thenComparing(s -> s.user.getFullName().toLowerCase())).orElseThrow();
                chosen.take(r, cls);
            }
        }
    }

    // --------------------------------------------- draft, review, edit and publish

    public ProjectDTOs.DraftView createDraft(String username, Long sessionId) {
        requireProjectCoordinator(username);
        ProjectDTOs.Proposal proposal = proposeFullCohort(username, sessionId);
        AcademicSession academicSession = session(proposal.sessionId());
        draftRepository.findFirstByAcademicSessionIdAndStatusOrderByCreatedAtDesc(academicSession.getId(), "DRAFT")
                .ifPresent(previous -> previous.setStatus("SUPERSEDED"));

        ProjectAllocationDraft draft = new ProjectAllocationDraft();
        draft.setAcademicSession(academicSession);
        draft.setCreatedBy(username);
        draft.setStatus("DRAFT");
        draftRepository.save(draft);

        Map<Long, Student> studentsById = allocationStudents(academicSession.getId()).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, User> supervisorsById = supsById(supervisors(academicSession.getId()));
        List<ProjectAllocationDraftItem> items = new ArrayList<>();
        for (ProjectDTOs.SupervisorGroup group : proposal.groups()) {
            User supervisor = supervisorsById.get(group.supervisorId());
            for (ProjectDTOs.StudentLine line : group.students()) {
                Student student = studentsById.get(line.studentId());
                if (student == null) throw bad("The student list changed while the draft was being generated. Generate it again.");
                ProjectAllocationDraftItem item = new ProjectAllocationDraftItem();
                item.setDraft(draft); item.setStudent(student); item.setSupervisor(supervisor);
                item.setClassification(line.classification()); item.setCgpa(line.cgpa());
                items.add(item);
            }
        }
        draftItemRepository.saveAll(items);
        logEvent(draft, null, "DRAFT_GENERATED", null, null, username,
                "Generated " + items.size() + " proposed student assignments.");
        return draftView(draft);
    }

    public ProjectDTOs.DraftView getDraft(String username, Long draftId) {
        requireProjectManager(username);
        return draftView(loadDraft(draftId));
    }

    public ProjectDTOs.DraftView latestDraft(String username, Long sessionId) {
        requireProjectManager(username);
        AcademicSession academicSession = session(sessionId);
        return draftRepository.findFirstByAcademicSessionIdOrderByCreatedAtDesc(academicSession.getId())
                .map(this::draftView).orElse(null);
    }

    public ProjectDTOs.DraftView moveDraftItem(String username, Long draftId, Long itemId, ProjectDTOs.MoveRequest request) {
        requireProjectCoordinator(username);
        if (request == null || request.supervisorId() == null) throw bad("Select a supervisor.");
        ProjectAllocationDraft draft = loadDraft(draftId); ensureDraftOpen(draft);
        ProjectAllocationDraftItem item = draftItemRepository.findById(itemId)
                .filter(i -> i.getDraft().getId().equals(draftId))
                .orElseThrow(() -> bad("Draft student assignment not found."));
        Sup selected = supervisors(draft.getAcademicSession().getId()).stream()
                .filter(s -> s.user.getId().equals(request.supervisorId())).findFirst()
                .orElseThrow(() -> bad("The selected lecturer is not currently eligible to supervise."));
        if ("FIRST".equals(item.getClassification()) && !selected.profile.isFirstClassEligible()) {
            throw bad("This student is First Class. The H.O.D has not marked the selected lecturer as First Class-eligible.");
        }
        String previous = item.getSupervisor().getFullName();
        item.setSupervisor(selected.user); draftItemRepository.save(item);
        logEvent(draft, item.getStudent(), "STUDENT_MOVED", previous, selected.user.getFullName(), username, request.note());
        return draftView(draft);
    }

    public ProjectDTOs.DraftView publishDraft(String username, Long draftId) {
        requireProjectCoordinator(username);
        ProjectAllocationDraft draft = loadDraft(draftId); ensureDraftOpen(draft);
        List<ProjectAllocationDraftItem> items = draftItemRepository.findByDraftId(draftId);
        if (items.isEmpty()) throw bad("The draft has no student assignments to publish.");
        List<Student> currentCohort = allocationStudents(draft.getAcademicSession().getId());
        Set<Long> currentCohortIds = currentCohort.stream().map(Student::getId).collect(Collectors.toSet());
        Set<Long> draftStudentIds = items.stream().map(i -> i.getStudent().getId()).collect(Collectors.toSet());
        if (draftStudentIds.size() != items.size()
                || !draftStudentIds.equals(currentCohortIds)) {
            throw bad("The active 400 level cohort has changed since this draft was generated. Generate a fresh draft before publishing.");
        }

        Map<Long, ProjectSupervision> existingByStudent = repository.findForSession(draft.getAcademicSession().getId())
                .stream().collect(Collectors.toMap(p -> p.getStudent().getId(), p -> p));
        List<ProjectSupervision> published = new ArrayList<>();
        for (ProjectAllocationDraftItem item : items) {
            if ("FIRST".equals(item.getClassification()) && !profileFor(item.getSupervisor()).isFirstClassEligible())
                throw bad("A First Class student is assigned to a supervisor who is no longer First Class-eligible.");

            // Update an existing assignment in place when rebuilding the cohort;
            // otherwise create the first official assignment for this student.
            ProjectSupervision p = existingByStudent.getOrDefault(item.getStudent().getId(), new ProjectSupervision());
            p.setStudent(item.getStudent()); p.setSupervisor(item.getSupervisor());
            p.setAcademicSession(draft.getAcademicSession()); p.setClassification(item.getClassification());
            p.setCgpa(item.getCgpa()); p.setStatus("PUBLISHED"); p.setAssignedAt(LocalDateTime.now()); p.setAssignedBy(username);
            published.add(p);
        }
        try {
            repository.saveAll(published);
            repository.flush();
            // Include previously confirmed assignments in the official schedule once this session is published.
            List<ProjectSupervision> sessionRows = repository.findForSession(draft.getAcademicSession().getId());
            for (ProjectSupervision row : sessionRows) row.setStatus("PUBLISHED");
            repository.saveAll(sessionRows);
        } catch (DataIntegrityViolationException e) {
            throw bad("The allocation could not be published because some students already have assignments. Generate a fresh draft.");
        }
        draft.setStatus("PUBLISHED"); draft.setPublishedAt(LocalDateTime.now()); draft.setPublishedBy(username);
        logEvent(draft, null, "PUBLISHED", null, null, username,
                "Published " + published.size() + " student assignments for " + draft.getAcademicSession().getName() + ".");
        return draftView(draft);
    }

    /** Public schedule DTO intentionally contains no CGPA or degree classification. */
    public List<ProjectDTOs.PublicScheduleRow> publicSchedule(Long sessionId) {
        AcademicSession academicSession = session(sessionId);
        return repository.findForSession(academicSession.getId()).stream()
                .filter(p -> "PUBLISHED".equalsIgnoreCase(p.getStatus()))
                .map(p -> new ProjectDTOs.PublicScheduleRow(academicSession.getName(),
                        p.getStudent().getMatricNumber(), p.getStudent().getFullName(),
                        p.getStudent().getProgramme() != null ? p.getStudent().getProgramme().getName() : "",
                        p.getSupervisor().getFullName(), profileFor(p.getSupervisor()).getDesignation()))
                .toList();
    }

    private Map<Long, User> supsById(List<Sup> sups) {
        return sups.stream().collect(Collectors.toMap(s -> s.user.getId(), s -> s.user));
    }
    private ProjectAllocationDraft loadDraft(Long id) {
        return draftRepository.findById(id).orElseThrow(() -> bad("Allocation draft not found."));
    }
    private void ensureDraftOpen(ProjectAllocationDraft draft) {
        if (!"DRAFT".equals(draft.getStatus())) throw bad("This allocation is no longer a draft and cannot be edited.");
    }
    private ProjectDTOs.DraftView draftView(ProjectAllocationDraft draft) {
        List<ProjectDTOs.DraftItemRow> rows = draftItemRepository.findByDraftIdOrderByStudentFullNameAsc(draft.getId())
                .stream().map(i -> new ProjectDTOs.DraftItemRow(i.getId(), i.getStudent().getId(),
                        i.getStudent().getMatricNumber(), i.getStudent().getFullName(),
                        i.getStudent().getProgramme() != null ? i.getStudent().getProgramme().getName() : "",
                        i.getClassification(), i.getCgpa(), i.getSupervisor().getId(), i.getSupervisor().getFullName()))
                .toList();
        List<ProjectDTOs.AllocationEventRow> history = eventRepository.findByDraftIdOrderByChangedAtAsc(draft.getId())
                .stream().map(e -> new ProjectDTOs.AllocationEventRow(e.getAction(),
                        e.getStudent() != null ? e.getStudent().getMatricNumber() : null,
                        e.getPreviousSupervisor(), e.getNewSupervisor(), e.getChangedBy(), e.getChangedAt(), e.getNote()))
                .toList();
        return new ProjectDTOs.DraftView(draft.getId(), draft.getAcademicSession().getId(),
                draft.getAcademicSession().getName(), draft.getStatus(), draft.getCreatedAt(),
                draft.getUpdatedAt(), draft.getPublishedAt(), rows, history);
    }
    private void logEvent(ProjectAllocationDraft draft, Student student, String action,
                          String previous, String next, String username, String note) {
        ProjectAllocationEvent event = new ProjectAllocationEvent();
        event.setDraft(draft); event.setStudent(student); event.setAction(action);
        event.setPreviousSupervisor(previous); event.setNewSupervisor(next);
        event.setChangedBy(username); event.setChangedAt(LocalDateTime.now()); event.setNote(note);
        eventRepository.save(event);
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

            Ranked r = rank(s, session.getId());

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
        requireProjectManager(username);
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

    private List<Student> allocationStudents(Long sessionId) {
        return studentRepository.findAll().stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .filter(s -> s.getAcademicSession() != null
                        && s.getAcademicSession().getId().equals(sessionId))
                .filter(s -> s.getLevel() != null && Integer.valueOf(400).equals(s.getLevel().getLevelNumber()))
                .toList();
    }

    /** Students who do not yet have an official assignment; used by the legacy confirm endpoint. */
    private List<Student> eligibleStudents(Long sessionId) {
        Set<Long> assigned = repository.findForSession(sessionId).stream()
                .map(p -> p.getStudent().getId())
                .collect(Collectors.toSet());
        return allocationStudents(sessionId).stream()
                .filter(s -> !assigned.contains(s.getId()))
                .toList();
    }

    /**
     * Uses the latest CGPA uploaded for this student, programme, level and
     * selected academic session. If no valid uploaded value exists, it falls
     * back to the cumulative CGPA calculated from approved academic results.
     */
    private Ranked rank(Student s, Long sessionId) {
        List<ReportedCgpa> uploaded = reportedCgpaRepository.findLatestForAllocation(
                s.getMatricNumber(),
                sessionId
        );

        if (!uploaded.isEmpty()) {
            BigDecimal cgpa = uploaded.get(0).getCgpa();
            if (isValidCgpa(cgpa)) return rankedFromCgpa(s, cgpa);
        }

        // The roster may have been uploaded against the preceding academic
        // session before students were promoted. CGPA is cumulative, so use
        // the latest matric-matched upload from any session as a fallback.
        List<ReportedCgpa> anySession = reportedCgpaRepository.findLatestForAllocationAnySession(
                s.getMatricNumber()
        );
        if (!anySession.isEmpty() && isValidCgpa(anySession.get(0).getCgpa())) {
            return rankedFromCgpa(s, anySession.get(0).getCgpa());
        }

        try {
            List<SemesterResultDTO> history = calculationService.calculateFullAcademicHistory(s.getId());
            if (history.isEmpty()) return new Ranked(s, "OTHER", null);

            CumulativeSummaryDTO cumulative = history.get(history.size() - 1).currentCumulative();
            return rankedFromCgpa(s, cumulative.cgpa());
        } catch (RuntimeException e) {
            // Do not let a missing or incomplete result history stop allocation
            // for the rest of the cohort. The proposal reports this student as unclassified.
            return new Ranked(s, "OTHER", null);
        }
    }

    private boolean isValidCgpa(BigDecimal cgpa) {
        return cgpa != null && cgpa.compareTo(BigDecimal.ZERO) >= 0
                && cgpa.compareTo(BigDecimal.valueOf(5.00)) <= 0;
    }

    private Ranked rankedFromCgpa(Student student, BigDecimal cgpa) {
        String label = DegreeClassUtil.classify(cgpa);
        String normalized = label == null ? "" : label.toUpperCase();

        String classification = normalized.contains("FIRST") ? "FIRST"
                : normalized.contains("UPPER") ? "UPPER"
                : normalized.contains("LOWER") ? "LOWER"
                : normalized.contains("THIRD") ? "THIRD"
                : normalized.contains("FAIL") ? "FAIL" : "OTHER";

        return new Ranked(student, classification, cgpa);
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

    private void requireProjectManager(String username) {
        User user = getUser(username);
        String role = String.valueOf(user.getRole()).toUpperCase();
        StaffProfile profile = profileFor(user);
        if (!role.equals("HOD") && !role.equals("ADMIN") && !profile.isProjectCoordinator()
                && !profile.isAssistantCoordinator())
            throw bad("Only the H.O.D or a Project Coordinator can manage project allocations.");
    }

    private void requireProjectCoordinator(String username) {
        User user = getUser(username);
        String role = String.valueOf(user.getRole()).toUpperCase();
        StaffProfile profile = profileFor(user);
        if (!role.equals("HOD") && !role.equals("ADMIN") && !profile.isProjectCoordinator()
                && !profile.isAssistantCoordinator())
            throw bad("Only the Project Coordinator, Assistant Project Coordinator or H.O.D can do this.");
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}