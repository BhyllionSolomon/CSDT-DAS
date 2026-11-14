package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CourseResultDTO;
import com.kdu.csdtdas.backend.dto.SemesterResultDTO;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.DirectEntryProfile;
import com.kdu.csdtdas.backend.entity.ExtraUnitGrant;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.DirectEntryProfileRepository;
import com.kdu.csdtdas.backend.repository.ExtraUnitGrantRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RegistrationRulesService {

    public record CourseOption(Long id, String code, String title, int unit, String status, String level) {}

    public record Options(
            Long studentId, String fullName, String programme, String level,
            Long sessionId, String sessionName, String semester,
            int requiredUnits, int approvedExtra, boolean directEntry, Integer entryLevel,
            List<CourseOption> carryOvers, List<CourseOption> offered, List<CourseOption> backlog,
            List<Long> registeredIds, String notice
    ) {}

    public record Checked(int totalUnits, int requiredUnits) {}

    private record Plan(List<Course> carry, List<Course> offered, List<Course> backlog, DirectEntryProfile directEntry) {}

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AcademicSessionRepository sessionRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final DirectEntryProfileRepository directRepository;
    private final ExtraUnitGrantRepository grantRepository;
    private final ResultCalculationService calculationService;
    private final SemesterUnitLimitService unitLimitService;
    private final UserRepository userRepository;

    public RegistrationRulesService(
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            AcademicSessionRepository sessionRepository,
            CourseRegistrationRepository registrationRepository,
            DirectEntryProfileRepository directRepository,
            ExtraUnitGrantRepository grantRepository,
            ResultCalculationService calculationService,
            SemesterUnitLimitService unitLimitService,
            UserRepository userRepository
    ) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
        this.registrationRepository = registrationRepository;
        this.directRepository = directRepository;
        this.grantRepository = grantRepository;
        this.calculationService = calculationService;
        this.unitLimitService = unitLimitService;
        this.userRepository = userRepository;
    }

    /** A student sees only their own options; the H.O.D, admin and level advisers can see any student's. */
    @Transactional(readOnly = true)
    public Options optionsFor(String username, Long studentId, Long sessionId, String semester) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        String role = String.valueOf(user.getRole()).toUpperCase();

        if (role.equals("STUDENT")) {
            if (user.getStudent() == null || !user.getStudent().getId().equals(studentId)) {
                throw bad("You can only see your own course options.");
            }
        } else if (!Set.of("HOD", "ADMIN", "LEVEL_ADVISER").contains(role)) {
            throw bad("You do not have permission to see these options.");
        }
        return options(studentId, sessionId, semester);
    }

    @Transactional(readOnly = true)
    public Options options(Long studentId, Long sessionId, String semester) {
        Student s = studentRepository.findById(studentId).orElseThrow(() -> bad("Student not found."));
        AcademicSession session = sessionRepository.findById(sessionId).orElseThrow(() -> bad("Academic session not found."));
        String sem = clean(semester);

        Plan plan = plan(s, sem);
        int required = requiredUnits(s, sem);
        int extra = extraUnits(s.getId(), sessionId, sem);

        String notice = null;
        if (!"ACTIVE".equalsIgnoreCase(s.getStatus())) {
            notice = "This student is not active and cannot register courses.";
        } else if (required <= 0) {
            notice = "Required units are not set for this programme, level and semester. The H.O.D sets them under Courses, Set Required Units.";
        }

        return new Options(
                s.getId(), s.getFullName(), s.getProgramme().getName(), s.getLevel().getName(),
                session.getId(), session.getName(), sem, required, extra,
                plan.directEntry() != null,
                plan.directEntry() == null ? null : plan.directEntry().getEntryLevelNumber(),
                plan.carry().stream().map(this::option).toList(),
                plan.offered().stream().map(this::option).toList(),
                plan.backlog().stream().map(this::option).toList(),
                List.copyOf(registeredIds(s.getId(), sessionId, sem)),
                notice
        );
    }

    /** Throws unless these courses are allowed for this student this semester. */
    public Checked validate(Student s, AcademicSession session, String sem, List<Long> ids) {

        if (!"ACTIVE".equalsIgnoreCase(s.getStatus())) {
            throw bad("This student is not active and cannot register courses.");
        }

        int required = requiredUnits(s, sem);
        if (required <= 0) {
            throw bad("Required units are not set for this programme, level and semester. Please contact the H.O.D.");
        }

        Plan plan = plan(s, sem);
        int extra = extraUnits(s.getId(), session.getId(), sem);
        Set<Long> registered = registeredIds(s.getId(), session.getId(), sem);

        Map<Long, Course> carry = byId(plan.carry());
        Map<Long, Course> offered = byId(plan.offered());
        Map<Long, Course> backlog = byId(plan.backlog());

        int base = 0, extraUsed = 0;

        for (Long id : ids) {
            if (carry.containsKey(id)) {
                base += unitOf(carry.get(id));
            } else if (offered.containsKey(id)) {
                base += unitOf(offered.get(id));
            } else if (backlog.containsKey(id)) {
                if (extra <= 0) {
                    throw bad(backlog.get(id).getCode() + " is a lower-level course. Ask your Level Adviser to approve extra units first.");
                }
                extraUsed += unitOf(backlog.get(id));
            } else {
                String code = courseRepository.findById(id).map(Course::getCode).orElse("A course");
                throw bad(code + " is not available to you this semester.");
            }
        }

        List<String> missing = carry.values().stream()
                .filter(c -> !ids.contains(c.getId()) && !registered.contains(c.getId()))
                .map(Course::getCode).toList();
        if (!missing.isEmpty()) {
            throw bad("Courses you failed in the " + sem.toLowerCase() + " semester must be registered first: " + String.join(", ", missing));
        }

        if (base != required) {
            throw bad("Your carry-over and current-level courses total " + base + " unit(s), but exactly " + required
                    + " are required for this level and semester. Adjust your selection.");
        }
        if (extraUsed > extra) {
            throw bad("You chose " + extraUsed + " extra unit(s) but only " + extra + " are approved.");
        }

        return new Checked(base + extraUsed, required);
    }

    // ---------------------------------------------------------------- helpers

    private Plan plan(Student s, String sem) {

        List<Course> carry = calculationService.getOutstandingCourses(s.getId()).stream()
                .filter(c -> c.getSemester() != null && c.getSemester().equalsIgnoreCase(sem))
                .toList();
        Set<Long> carryIds = carry.stream().map(Course::getId).collect(Collectors.toSet());

        Long programmeId = s.getProgramme().getId();
        List<Course> all = courseRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()) && sem.equalsIgnoreCase(c.getSemester()))
                .filter(c -> c.getProgrammes().stream().anyMatch(p -> p.getId().equals(programmeId)))
                .toList();

        List<Course> offered = all.stream()
                .filter(c -> c.getLevel().getId().equals(s.getLevel().getId()) && !carryIds.contains(c.getId()))
                .toList();

        DirectEntryProfile direct = directRepository.findByStudentId(s.getId()).orElse(null);
        List<Course> backlog = List.of();

        if (direct != null) {
            Set<String> passed = passedCodes(s.getId());
            backlog = all.stream()
                    .filter(c -> c.getLevel().getLevelNumber() != null && c.getLevel().getLevelNumber() < direct.getEntryLevelNumber())
                    .filter(c -> !passed.contains(c.getCode()) && !carryIds.contains(c.getId()))
                    .toList();
        }

        return new Plan(carry, offered, backlog, direct);
    }

    private Set<String> passedCodes(Long studentId) {
        Set<String> passed = new HashSet<>();
        try {
            for (SemesterResultDTO h : calculationService.calculateFullAcademicHistory(studentId)) {
                for (CourseResultDTO c : h.courses()) {
                    if (c.score() != null && c.score().compareTo(BigDecimal.valueOf(40)) >= 0) passed.add(c.courseCode());
                }
            }
        } catch (RuntimeException ignored) {
            // no approved results yet
        }
        return passed;
    }

    private int requiredUnits(Student s, String sem) {
        try {
            return unitLimitService.getRequiredUnits(s.getProgramme().getId(), s.getLevel().getId(), sem);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private int extraUnits(Long studentId, Long sessionId, String sem) {
        return grantRepository.findByStudentIdAndAcademicSessionIdAndSemester(studentId, sessionId, sem)
                .map(ExtraUnitGrant::getUnits).orElse(0);
    }

    private Set<Long> registeredIds(Long studentId, Long sessionId, String sem) {
        return registrationRepository.findByStudentIdAndAcademicSessionId(studentId, sessionId).stream()
                .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()) && sem.equalsIgnoreCase(r.getSemester()))
                .map(r -> r.getCourse().getId())
                .collect(Collectors.toSet());
    }

    private Map<Long, Course> byId(List<Course> courses) {
        Map<Long, Course> map = new LinkedHashMap<>();
        courses.forEach(c -> map.put(c.getId(), c));
        return map;
    }

    private CourseOption option(Course c) {
        return new CourseOption(c.getId(), c.getCode(), c.getTitle(), unitOf(c), c.getStatus(), c.getLevel().getName());
    }

    private int unitOf(Course c) {
        return c.getCreditUnit() == null ? 0 : c.getCreditUnit();
    }

    private String clean(String semester) {
        String s = semester == null ? "" : semester.trim().toUpperCase();
        if (!s.equals("FIRST") && !s.equals("SECOND")) throw bad("Semester must be FIRST or SECOND.");
        return s;
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}