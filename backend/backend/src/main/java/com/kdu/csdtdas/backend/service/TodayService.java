package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.AttendanceDTOs;
import com.kdu.csdtdas.backend.dto.FeedbackDTOs;
import com.kdu.csdtdas.backend.dto.LabDTOs;
import com.kdu.csdtdas.backend.dto.TimetableDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.AttendanceClass;
import com.kdu.csdtdas.backend.entity.RegistrationClearance;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AttendanceClassRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.ProjectSupervisionRepository;
import com.kdu.csdtdas.backend.repository.RegistrationClearanceRepository;
import com.kdu.csdtdas.backend.repository.ResultStatsRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Builds the "Today" cards for the signed-in person. Each card is worked out on its own, in its own
 * transaction, so one card that cannot be built never hides the others.
 */
@Service
public class TodayService {

    public record Item(String key, String tone, String title, String detail, String link) {}

    public record Today(String sessionName, String semester, String date, List<Item> items) {}

    private static final ZoneId LAGOS = ZoneId.of("Africa/Lagos");
    private static final Set<String> HOD = Set.of("HOD", "ADMIN");
    private static final Set<String> LAB = Set.of("LAB_OFFICER", "TECHNICAL_STAFF");
    private static final Set<String> MONEY = Set.of("LAB_OFFICER");

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final RegistrationClearanceRepository clearanceRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final ProjectSupervisionRepository supervisionRepository;
    private final AttendanceClassRepository attendanceClassRepository;
    private final ResultStatsRepository resultRepository;
    private final RegistrationClearanceService clearanceService;
    private final TimetableService timetableService;
    private final FeedbackService feedbackService;
    private final AttendanceService attendanceService;
    private final ProjectSupervisionService projectService;
    private final LabManualService labService;
    private final ResultCalculationService calculationService;
    private final TransactionTemplate tx;

    public TodayService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            RegistrationClearanceRepository clearanceRepository,
            CourseRegistrationRepository registrationRepository,
            ProjectSupervisionRepository supervisionRepository,
            AttendanceClassRepository attendanceClassRepository,
            ResultStatsRepository resultRepository,
            RegistrationClearanceService clearanceService,
            TimetableService timetableService,
            FeedbackService feedbackService,
            AttendanceService attendanceService,
            ProjectSupervisionService projectService,
            LabManualService labService,
            ResultCalculationService calculationService,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.clearanceRepository = clearanceRepository;
        this.registrationRepository = registrationRepository;
        this.supervisionRepository = supervisionRepository;
        this.attendanceClassRepository = attendanceClassRepository;
        this.resultRepository = resultRepository;
        this.clearanceService = clearanceService;
        this.timetableService = timetableService;
        this.feedbackService = feedbackService;
        this.attendanceService = attendanceService;
        this.projectService = projectService;
        this.labService = labService;
        this.calculationService = calculationService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public Today today(String username) {
        final User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found."));
        final String role = String.valueOf(user.getRole()).toUpperCase();
        final LocalDate day = LocalDate.now(LAGOS);
        final String code = day.getDayOfWeek().name().substring(0, 3);
        // First semester runs from October to February, second semester from March to September.
        final String sem = day.getMonthValue() >= 10 || day.getMonthValue() <= 2 ? "FIRST" : "SECOND";

        final AcademicSession session = clearanceService.currentSession();
        List<Item> items = new ArrayList<>();

        if (session == null) {
            boolean hod = HOD.contains(role);
            items.add(item("warn", "No academic session is set up yet",
                    hod ? "Create the current session first." : "The department has not started a session.", hod ? "/sessions" : null));
            return new Today("", sem, day.toString(), items);
        }

        final Long sid = session.getId();

        if (HOD.contains(role)) {
            attempt(items, () -> hodRegistrations(sid));
            attempt(items, this::hodResults);
            attempt(items, this::hodAccounts);
            attempt(items, () -> hodTimetable(user, sid, sem));
            attempt(items, () -> hodFeedback(sid, sem));
            attempt(items, () -> hodFinalYear(sid));
            attempt(items, this::hodSetup);
        } else if (role.equals("STUDENT")) {
            attempt(items, () -> studentRegistration(user, sid, sem));
            attempt(items, () -> classesToday(user, sid, sem, code, true));
            attempt(items, () -> studentAttendance(user, sid, sem));
            attempt(items, () -> studentFeedback(user, sid, sem));
            attempt(items, () -> studentCarryOvers(user));
            attempt(items, () -> studentSupervisor(user));
        } else if (LAB.contains(role)) {
            attempt(items, () -> labStock(user));
            attempt(items, () -> labSales(user, role));
        } else {
            attempt(items, () -> classesToday(user, sid, sem, code, false));
            attempt(items, () -> lecturerTimetable(user, sid, sem));
            attempt(items, () -> lecturerAttendance(user, sid, sem));
            attempt(items, () -> lecturerProject(user));
            attempt(items, () -> lecturerFeedback(user, sid, sem));
        }

        if (items.isEmpty()) items.add(item("ok", "All clear", "Nothing needs your attention right now.", null));
        items.sort(Comparator.comparingInt(i -> i.tone().equals("warn") ? 0 : i.tone().equals("info") ? 1 : 2));

        return new Today(session.getName(), sem, day.toString(), items);
    }

    // ------------------------------------------------------------------ H.O.D

    private List<Item> hodRegistrations(Long sid) {
        long n = clearanceRepository.findForSession(sid).stream()
                .filter(c -> "PENDING".equals(c.getStatus()) || "FLAGGED".equals(c.getStatus())).count();
        return n == 0 ? List.of() : List.of(item("warn", n + " registration" + s(n) + " waiting for approval", "Check each invoice, then approve.", "/registration-approvals"));
    }

    private List<Item> hodResults() {
        long n = resultRepository.countPending();
        return n == 0 ? List.of() : List.of(item("warn", n + " result" + s(n) + " waiting for verification", "Review and approve them so students can see them.", "/verification"));
    }

    private List<Item> hodAccounts() {
        List<User> all = userRepository.findAll();
        long staff = all.stream()
                .filter(u -> Set.of("LECTURER", "ADJUNCT").contains(String.valueOf(u.getRole()).toUpperCase()) && !Boolean.TRUE.equals(u.getActive())).count();
        long unlinked = all.stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(String.valueOf(u.getRole())) && u.getStudent() == null).count();

        List<Item> out = new ArrayList<>();
        if (staff > 0) out.add(item("warn", staff + " staff account" + s(staff) + " waiting to be activated", "New lecturers cannot sign in until you activate them.", "/users"));
        if (unlinked > 0) out.add(item("warn", unlinked + " student account" + s(unlinked) + " not linked to a student record", "Link them so those students can register.", "/registration-approvals"));
        return out;
    }

    private List<Item> hodTimetable(User user, Long sid, String sem) {
        TimetableDTOs.AdminStatus st = timetableService.adminStatus(user.getUsername(), sid, sem);
        String status = st.window().status();
        String label = sem.equals("FIRST") ? "first" : "second";

        if ("NONE".equals(status)) {
            return List.of(item("info", "The " + label + " semester timetable has not been opened", "Open a window so lecturers can choose their times.", "/timetable-admin"));
        }
        if ("OPEN".equals(status)) {
            String due = st.window().deadline() == null ? "" : "Deadline " + st.window().deadline().toLocalDate() + ".";
            return List.of(item("warn", st.respondedCourses() + " of " + st.totalCourses() + " courses have a timetable slot", due, "/timetable-admin"));
        }
        if ("CLOSED".equals(status)) {
            return List.of(item("warn", "The " + label + " semester timetable is ready for your approval", "Review it, then approve and publish.", "/timetable-admin"));
        }
        return List.of();
    }

    private List<Item> hodFeedback(Long sid, String sem) {
        return feedbackService.window(sid, sem).open()
                ? List.of(item("info", "Student feedback on lecturers is open", "Close it when you are ready to see the results.", "/feedback-admin"))
                : List.of();
    }

    private List<Item> hodFinalYear(Long sid) {
        Set<Long> assigned = supervisionRepository.findForSession(sid).stream().map(p -> p.getStudent().getId()).collect(Collectors.toSet());
        long left = studentRepository.findAll().stream()
                .filter(st -> "ACTIVE".equalsIgnoreCase(st.getStatus()))
                .filter(st -> st.getLevel() != null && Integer.valueOf(400).equals(st.getLevel().getLevelNumber()))
                .filter(st -> !assigned.contains(st.getId())).count();
        return left == 0 ? List.of() : List.of(item("warn", left + " final-year student" + s(left) + " without a project supervisor", "Allocate them in one click.", "/final-year-projects"));
    }

    private List<Item> hodSetup() {
        List<Item> out = new ArrayList<>();
        if (studentRepository.count() == 0) out.add(item("info", "No students have been added yet", "Upload a roster to get started.", "/bulk-upload-students"));
        if (courseRepository.count() == 0) out.add(item("info", "No courses have been added yet", "Upload your courses or create them one by one.", "/bulk-upload-courses"));
        return out;
    }

    // ----------------------------------------------------------- lecturers

    private List<Item> lecturerTimetable(User user, Long sid, String sem) {
        TimetableDTOs.MineView mv = timetableService.mine(user.getUsername(), sid, sem);
        if (!"OPEN".equals(mv.window().status())) return List.of();

        long missing = mv.courses().stream().filter(c -> c.entries().isEmpty()).count();
        if (missing == 0) return List.of();

        String due = mv.window().deadline() == null ? "" : "Deadline " + mv.window().deadline().toLocalDate() + ".";
        return List.of(item("warn", missing + " course" + s(missing) + " still need your timetable choices", due, "/my-timetable"));
    }

    private List<Item> lecturerAttendance(User user, Long sid, String sem) {
        List<Item> out = new ArrayList<>();
        LocalDate today = LocalDate.now(LAGOS);

        for (AttendanceDTOs.CourseInfo c : attendanceService.courses(user.getUsername(), sid, sem)) {
            if (out.size() >= 3) break;
            List<AttendanceClass> held = attendanceClassRepository
                    .findByCourseIdAndAcademicSessionIdAndSemesterOrderByClassDateDesc(c.courseId(), sid, sem);

            if (held.isEmpty()) {
                if (c.rosterSize() > 0) out.add(item("info", "No attendance recorded yet for " + c.code(), c.rosterSize() + " students are registered.", "/attendance"));
            } else {
                long days = java.time.temporal.ChronoUnit.DAYS.between(held.get(0).getClassDate(), today);
                if (days > 14) out.add(item("warn", "Attendance for " + c.code() + " was last taken " + days + " days ago", "Record your recent classes.", "/attendance"));
            }
        }
        return out;
    }

    private List<Item> lecturerProject(User user) {
        int n = projectService.myStudents(user.getUsername()).size();
        return n == 0 ? List.of() : List.of(item("info", "You supervise " + n + " final-year student" + s(n), "See who they are.", "/my-project-students"));
    }

    private List<Item> lecturerFeedback(User user, Long sid, String sem) {
        FeedbackDTOs.Results r = feedbackService.results(user.getUsername(), sid, sem);
        long ready = r.rows().stream().filter(FeedbackDTOs.Result::visible).count();
        return ready == 0 ? List.of() : List.of(item("info", "Student feedback is ready for " + ready + " of your courses", "Anonymous and combined.", "/my-feedback"));
    }

    // ------------------------------------------------- classes today (both)

    private List<Item> classesToday(User user, Long sid, String sem, String code, boolean student) {
        TimetableDTOs.StudentView v = student
                ? timetableService.studentView(user.getUsername(), sid, sem)
                : timetableService.view(user.getUsername(), sid, sem);
        if (!v.published()) return List.of();

        List<TimetableDTOs.EntryDto> mine = v.entries().stream()
                .filter(e -> code.equals(e.day()))
                .filter(e -> student || user.getId().equals(e.lecturerId()))
                .sorted(Comparator.comparing(TimetableDTOs.EntryDto::startTime))
                .toList();

        if (mine.isEmpty()) return List.of(item("ok", "No class today", "Use the time to catch up on materials.", "/timetable"));

        List<Item> out = new ArrayList<>();
        for (TimetableDTOs.EntryDto e : mine) {
            String where = ("PRACTICAL".equals(e.kind()) ? "Practical" : "Lecture") + " in " + e.room()
                    + (student && e.lecturer() != null ? " · " + e.lecturer() : "");
            out.add(item("info", e.courseCode() + " · " + e.startTime() + "–" + e.endTime(), where, student ? "/timetable" : "/attendance"));
        }
        return out;
    }

    // -------------------------------------------------------------- students

    private List<Item> studentRegistration(User user, Long sid, String sem) {
        Student st = user.getStudent();
        if (st == null) return List.of(item("warn", "Your account is not linked to a student record yet", "Ask the H.O.D or your Level Adviser to link it.", null));

        long registered = registrationRepository.findByStudentIdAndAcademicSessionId(st.getId(), sid).stream()
                .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()) && sem.equalsIgnoreCase(r.getSemester())).count();
        if (registered == 0) return List.of(item("warn", "Register your courses for this semester", "Carry-overs come first.", "/register-courses"));

        RegistrationClearance c = clearanceRepository.findByStudentIdAndAcademicSessionId(st.getId(), sid).orElse(null);
        if (c == null) return List.of(item("warn", "Submit your invoice code to finish registration", "Your courses are chosen. The H.O.D approves them after your payment.", "/my-registration"));

        return switch (c.getStatus()) {
            case "APPROVED" -> List.of(item("ok", "Your registration is approved", "Everything is unlocked.", null));
            case "REJECTED" -> List.of(item("warn", "Your registration needs attention", c.getNote() == null ? "Open it to see what to fix." : c.getNote(), "/my-registration"));
            default -> List.of(item("info", "Waiting for the H.O.D to approve your registration", "You will be unlocked as soon as it is approved.", "/my-registration"));
        };
    }

    private List<Item> studentAttendance(User user, Long sid, String sem) {
        AttendanceDTOs.Mine m = attendanceService.mine(user.getUsername(), sid, sem);
        List<Item> out = new ArrayList<>();
        for (AttendanceDTOs.MyCourse c : m.courses()) {
            if (out.size() >= 3) break;
            if (c.percent() != null && !c.eligible()) {
                out.add(item("warn", "Attendance is " + c.percent() + "% in " + c.code(), m.required() + "% is needed to sit the exam.", "/my-attendance"));
            }
        }
        return out;
    }

    private List<Item> studentFeedback(User user, Long sid, String sem) {
        FeedbackDTOs.Mine m = feedbackService.mine(user.getUsername(), sid, sem);
        long waiting = m.items().stream().filter(i -> !i.rated()).count();
        return m.open() && waiting > 0
                ? List.of(item("info", "Rate your lecturers (" + waiting + " waiting)", "It is anonymous and takes a minute.", "/rate-lecturers"))
                : List.of();
    }

    private List<Item> studentCarryOvers(User user) {
        Student st = user.getStudent();
        if (st == null) return List.of();
        int n = calculationService.getOutstandingCourses(st.getId()).size();
        return n == 0 ? List.of() : List.of(item("warn", n + " carry-over course" + s(n) + " to clear", "Register them first each semester.", "/my-carryovers"));
    }

    private List<Item> studentSupervisor(User user) {
        var mine = projectService.mine(user.getUsername());
        return mine.assigned() ? List.of(item("info", "Your project supervisor is " + mine.supervisorName(), null, "/my-project-supervisor")) : List.of();
    }

    // -------------------------------------------------------------------- lab

    private List<Item> labStock(User user) {
        List<Item> out = new ArrayList<>();
        for (LabDTOs.ManualRow m : labService.manuals(user.getUsername())) {
            if (!m.active() || m.stock() > 5) continue;
            out.add(item("warn", m.stock() <= 0 ? m.title() + " is out of stock" : "Low stock: " + m.title() + " (" + m.stock() + " left)",
                    "Add copies when new ones arrive.", "/lab-manuals"));
        }
        return out;
    }

    private List<Item> labSales(User user, String role) {
        LocalDate today = LocalDate.now(LAGOS);
        List<LabDTOs.SaleRow> sold = labService.sales(user.getUsername()).stream()
                .filter(x -> !x.voided())
                .filter(x -> x.soldAt().atZone(ZoneId.systemDefault()).withZoneSameInstant(LAGOS).toLocalDate().equals(today))
                .toList();
        if (sold.isEmpty()) return List.of();

        String detail = null;
        if (MONEY.contains(role)) {
            BigDecimal total = sold.stream().map(x -> x.unitPrice().multiply(BigDecimal.valueOf(x.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
            detail = "₦" + total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " today.";
        }
        return List.of(item("info", sold.size() + " manual" + s(sold.size()) + " sold today", detail, "/lab-manuals"));
    }

    // ---------------------------------------------------------------- helpers

    private void attempt(List<Item> out, Supplier<List<Item>> card) {
        try {
            List<Item> got = tx.execute(status -> card.get());
            if (got != null) out.addAll(got);
        } catch (RuntimeException ignored) {
            // This card cannot be built right now. The others still show.
        }
    }

    private Item item(String tone, String title, String detail, String link) {
        return new Item(tone + "-" + Math.abs((title + link).hashCode()), tone, title, detail, link);
    }

    private String s(long n) {
        return n == 1 ? "" : "s";
    }
}