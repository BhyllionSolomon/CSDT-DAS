package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.AttendanceDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.AttendanceClass;
import com.kdu.csdtdas.backend.entity.AttendanceMark;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.AttendanceClassRepository;
import com.kdu.csdtdas.backend.repository.AttendanceMarkRepository;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    /** A student needs at least this percentage to be eligible. */
    public static final int REQUIRED_PERCENT = 75;

    private static final Set<String> STATUSES = Set.of("PRESENT", "ABSENT", "LATE", "EXCUSED");
    private static final Set<String> STAFF = Set.of("HOD", "ADMIN", "LECTURER", "ADJUNCT", "LEVEL_ADVISER");

    private final AttendanceClassRepository classRepository;
    private final AttendanceMarkRepository markRepository;
    private final CourseRepository courseRepository;
    private final AcademicSessionRepository sessionRepository;
    private final CourseAllocationRepository allocationRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final TeachingAccessService accessService;
    private final UserRepository userRepository;

    public AttendanceService(
            AttendanceClassRepository classRepository,
            AttendanceMarkRepository markRepository,
            CourseRepository courseRepository,
            AcademicSessionRepository sessionRepository,
            CourseAllocationRepository allocationRepository,
            CourseRegistrationRepository registrationRepository,
            TeachingAccessService accessService,
            UserRepository userRepository
    ) {
        this.classRepository = classRepository;
        this.markRepository = markRepository;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
        this.allocationRepository = allocationRepository;
        this.registrationRepository = registrationRepository;
        this.accessService = accessService;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ staff

    /** The courses this person can take attendance for: their own, or every course for the H.O.D. */
    @Transactional(readOnly = true)
    public List<AttendanceDTOs.CourseInfo> courses(String username, Long sessionId, String semester) {
        User user = staff(username);
        String sem = clean(semester);
        boolean hod = isHod(user);

        Map<Long, Course> found = new LinkedHashMap<>();
        for (CourseAllocation a : allocationRepository.findByAcademicSessionIdAndSemester(sessionId, sem)) {
            if (!hod && (a.getLecturer() == null || !a.getLecturer().getId().equals(user.getId()))) continue;
            found.putIfAbsent(a.getCourse().getId(), a.getCourse());
        }

        List<AttendanceDTOs.CourseInfo> out = new ArrayList<>();
        for (Course c : found.values()) {
            out.add(new AttendanceDTOs.CourseInfo(
                    c.getId(), c.getCode(), c.getTitle(), c.getLevel().getName(),
                    classRepository.findByCourseIdAndAcademicSessionIdAndSemesterOrderByClassDateDesc(c.getId(), sessionId, sem).size(),
                    roster(c.getId(), sessionId, sem).size()));
        }
        out.sort(Comparator.comparing(AttendanceDTOs.CourseInfo::code));
        return out;
    }

    @Transactional(readOnly = true)
    public List<AttendanceDTOs.ClassRow> classes(String username, Long courseId, Long sessionId, String semester) {
        String sem = clean(semester);
        authorize(staff(username), courseId, sessionId, sem);

        Map<Long, int[]> counts = new HashMap<>();
        for (AttendanceMark m : markRepository.findForCourse(courseId, sessionId, sem)) {
            int[] c = counts.computeIfAbsent(m.getAttendanceClass().getId(), k -> new int[2]);
            c[1]++;
            if (m.getStatus().equals("PRESENT") || m.getStatus().equals("LATE")) c[0]++;
        }

        return classRepository.findByCourseIdAndAcademicSessionIdAndSemesterOrderByClassDateDesc(courseId, sessionId, sem).stream()
                .map(c -> {
                    int[] n = counts.getOrDefault(c.getId(), new int[2]);
                    return new AttendanceDTOs.ClassRow(c.getId(), c.getClassDate().toString(), c.getKind(), c.getTopic(), n[0], n[1]);
                }).toList();
    }

    /** The students to mark, with the marks already saved for that class if there is one. */
    @Transactional(readOnly = true)
    public AttendanceDTOs.Roster rosterFor(String username, Long courseId, Long sessionId, String semester, String date, String kind) {
        String sem = clean(semester);
        authorize(staff(username), courseId, sessionId, sem);

        LocalDate day = parseDate(date);
        String k = kind(kind);

        AttendanceClass existing = classRepository
                .findByCourseIdAndAcademicSessionIdAndSemesterAndClassDateAndKind(courseId, sessionId, sem, day, k).orElse(null);

        Map<Long, String> saved = new HashMap<>();
        if (existing != null) {
            for (AttendanceMark m : markRepository.findByAttendanceClassId(existing.getId())) {
                saved.put(m.getStudent().getId(), m.getStatus());
            }
        }

        List<AttendanceDTOs.RosterStudent> students = roster(courseId, sessionId, sem).stream()
                .map(s -> new AttendanceDTOs.RosterStudent(s.getId(), s.getMatricNumber(), s.getFullName(),
                        saved.getOrDefault(s.getId(), "PRESENT")))
                .toList();

        return new AttendanceDTOs.Roster(existing == null ? null : existing.getId(),
                existing == null ? null : existing.getTopic(), existing != null, students);
    }

    /** Saves one class. Saving the same class again updates it, so nothing is duplicated. */
    public AttendanceDTOs.ClassRow save(String username, AttendanceDTOs.SaveRequest req) {
        User user = staff(username);
        String sem = clean(req.semester());
        authorize(user, req.courseId(), req.sessionId(), sem);

        LocalDate day = parseDate(req.date());
        if (day.isAfter(LocalDate.now())) throw bad("You cannot take attendance for a future date.");
        if (req.marks() == null || req.marks().isEmpty()) throw bad("There is nobody to mark.");

        String kind = kind(req.kind());
        Course course = courseRepository.findById(req.courseId()).orElseThrow(() -> bad("Course not found."));
        AcademicSession session = sessionRepository.findById(req.sessionId()).orElseThrow(() -> bad("Academic session not found."));

        Map<Long, Student> onRoster = roster(course.getId(), session.getId(), sem).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        AttendanceClass cls = classRepository
                .findByCourseIdAndAcademicSessionIdAndSemesterAndClassDateAndKind(course.getId(), session.getId(), sem, day, kind)
                .orElseGet(AttendanceClass::new);

        cls.setCourse(course);
        cls.setAcademicSession(session);
        cls.setSemester(sem);
        cls.setClassDate(day);
        cls.setKind(kind);
        cls.setTopic(req.topic() == null || req.topic().isBlank() ? null : req.topic().trim().substring(0, Math.min(req.topic().trim().length(), 150)));
        cls.setTakenBy(user.getUsername());
        cls.setTakenAt(LocalDateTime.now());
        classRepository.save(cls);

        Map<Long, AttendanceMark> current = markRepository.findByAttendanceClassId(cls.getId()).stream()
                .collect(Collectors.toMap(m -> m.getStudent().getId(), m -> m));

        Set<Long> seen = new HashSet<>();
        int present = 0;

        for (AttendanceDTOs.Mark mark : req.marks()) {
            String status = mark.status() == null ? "" : mark.status().trim().toUpperCase();
            if (!STATUSES.contains(status)) throw bad("A mark must be PRESENT, ABSENT, LATE or EXCUSED.");

            Student student = onRoster.get(mark.studentId());
            if (student == null) throw bad("A student in this list is not registered for the course.");
            if (!seen.add(student.getId())) throw bad("A student appears twice in this list.");

            AttendanceMark m = current.getOrDefault(student.getId(), new AttendanceMark());
            m.setAttendanceClass(cls);
            m.setStudent(student);
            m.setStatus(status);
            markRepository.save(m);

            if (status.equals("PRESENT") || status.equals("LATE")) present++;
        }

        return new AttendanceDTOs.ClassRow(cls.getId(), day.toString(), kind, cls.getTopic(), present, seen.size());
    }

    public void delete(String username, Long classId) {
        User user = staff(username);
        AttendanceClass cls = classRepository.findById(classId).orElseThrow(() -> bad("Class not found."));

        if (!isHod(user) && !user.getUsername().equals(cls.getTakenBy())) {
            throw bad("Only the lecturer who took this class, or the H.O.D, can delete it.");
        }
        markRepository.deleteByAttendanceClassId(classId);
        classRepository.delete(cls);
    }

    @Transactional(readOnly = true)
    public AttendanceDTOs.Summary summary(String username, Long courseId, Long sessionId, String semester) {
        String sem = clean(semester);
        authorize(staff(username), courseId, sessionId, sem);

        Map<Long, int[]> counts = new HashMap<>(); // present, late, absent, excused
        Set<Long> classIds = new HashSet<>();
        for (AttendanceMark m : markRepository.findForCourse(courseId, sessionId, sem)) {
            classIds.add(m.getAttendanceClass().getId());
            int[] c = counts.computeIfAbsent(m.getStudent().getId(), k -> new int[4]);
            switch (m.getStatus()) {
                case "PRESENT" -> c[0]++;
                case "LATE" -> c[1]++;
                case "ABSENT" -> c[2]++;
                default -> c[3]++;
            }
        }

        List<AttendanceDTOs.SummaryRow> rows = new ArrayList<>();
        for (Student s : roster(courseId, sessionId, sem)) {
            int[] c = counts.getOrDefault(s.getId(), new int[4]);
            int classes = c[0] + c[1] + c[2] + c[3];
            Integer percent = percent(c[0], c[1], c[3], classes);
            rows.add(new AttendanceDTOs.SummaryRow(s.getId(), s.getMatricNumber(), s.getFullName(),
                    classes, c[0], c[1], c[2], c[3], percent, percent != null && percent >= REQUIRED_PERCENT));
        }
        return new AttendanceDTOs.Summary(REQUIRED_PERCENT, classIds.size(), rows);
    }

    // ---------------------------------------------------------------- student

    /** A student sees only their own attendance. */
    @Transactional(readOnly = true)
    public AttendanceDTOs.Mine mine(String username, Long sessionId, String semester) {
        Student student = userRepository.findByUsername(username).map(User::getStudent).orElse(null);
        if (student == null) throw bad("Your account is not linked to a student record.");
        String sem = clean(semester);

        Map<Long, List<AttendanceMark>> byCourse = new LinkedHashMap<>();
        for (AttendanceMark m : markRepository.findForStudent(student.getId(), sessionId, sem)) {
            byCourse.computeIfAbsent(m.getAttendanceClass().getCourse().getId(), k -> new ArrayList<>()).add(m);
        }

        List<AttendanceDTOs.MyCourse> courses = new ArrayList<>();
        for (CourseRegistration reg : registrationRepository.findByStudentIdAndAcademicSessionId(student.getId(), sessionId)) {
            if (!"REGISTERED".equalsIgnoreCase(reg.getStatus()) || !sem.equalsIgnoreCase(reg.getSemester())) continue;

            Course c = reg.getCourse();
            List<AttendanceMark> marks = byCourse.getOrDefault(c.getId(), List.of());

            int present = 0, late = 0, absent = 0, excused = 0;
            List<AttendanceDTOs.HistoryItem> history = new ArrayList<>();
            for (AttendanceMark m : marks) {
                switch (m.getStatus()) {
                    case "PRESENT" -> present++;
                    case "LATE" -> late++;
                    case "ABSENT" -> absent++;
                    default -> excused++;
                }
                history.add(new AttendanceDTOs.HistoryItem(
                        m.getAttendanceClass().getClassDate().toString(), m.getAttendanceClass().getKind(), m.getStatus()));
            }

            int classes = marks.size();
            Integer percent = percent(present, late, excused, classes);
            courses.add(new AttendanceDTOs.MyCourse(c.getId(), c.getCode(), c.getTitle(), classes, present + late, late,
                    absent, excused, percent, percent != null && percent >= REQUIRED_PERCENT, history));
        }
        courses.sort(Comparator.comparing(AttendanceDTOs.MyCourse::code));
        return new AttendanceDTOs.Mine(REQUIRED_PERCENT, courses);
    }

    // ---------------------------------------------------------------- helpers

    /** Present and late count as attended; excused classes are left out of the total. */
    private Integer percent(int present, int late, int excused, int classes) {
        int counted = classes - excused;
        if (counted <= 0) return null;
        return (int) Math.round((present + late) * 100.0 / counted);
    }

    private List<Student> roster(Long courseId, Long sessionId, String sem) {
        return registrationRepository.findByCourseIdAndAcademicSessionIdAndSemester(courseId, sessionId, sem).stream()
                .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()))
                .map(CourseRegistration::getStudent)
                .sorted(Comparator.comparing(Student::getMatricNumber))
                .toList();
    }

    private void authorize(User user, Long courseId, Long sessionId, String sem) {
        if (!accessService.canTeach(user, courseId, sessionId, sem)) {
            throw bad("You are not assigned to teach this course for this session and semester.");
        }
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw bad("Enter the date properly.");
        }
    }

    private String kind(String raw) {
        return "PRACTICAL".equalsIgnoreCase(raw) ? "PRACTICAL" : "LECTURE";
    }

    private String clean(String semester) {
        String s = semester == null ? "" : semester.trim().toUpperCase();
        if (!s.equals("FIRST") && !s.equals("SECOND")) throw bad("Semester must be FIRST or SECOND.");
        return s;
    }

    private boolean isHod(User user) {
        String role = String.valueOf(user.getRole()).toUpperCase();
        return role.equals("HOD") || role.equals("ADMIN");
    }

    private User staff(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        if (!STAFF.contains(String.valueOf(user.getRole()).toUpperCase())) throw bad("You do not have permission to do this.");
        return user;
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}