package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.FeedbackDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.entity.FeedbackReceipt;
import com.kdu.csdtdas.backend.entity.FeedbackResponse;
import com.kdu.csdtdas.backend.entity.FeedbackWindow;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.FeedbackReceiptRepository;
import com.kdu.csdtdas.backend.repository.FeedbackResponseRepository;
import com.kdu.csdtdas.backend.repository.FeedbackWindowRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class FeedbackService {

    /** Results are hidden until at least this many students have rated a lecturer for a course. */
    private static final int MINIMUM = 5;

    private static final Set<String> TEACHING_ROLES = Set.of("LECTURER", "ADJUNCT", "LEVEL_ADVISER");

    private final FeedbackWindowRepository windowRepository;
    private final FeedbackResponseRepository responseRepository;
    private final FeedbackReceiptRepository receiptRepository;
    private final AcademicSessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final CourseAllocationRepository allocationRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public FeedbackService(
            FeedbackWindowRepository windowRepository,
            FeedbackResponseRepository responseRepository,
            FeedbackReceiptRepository receiptRepository,
            AcademicSessionRepository sessionRepository,
            CourseRepository courseRepository,
            CourseAllocationRepository allocationRepository,
            CourseRegistrationRepository registrationRepository,
            UserRepository userRepository
    ) {
        this.windowRepository = windowRepository;
        this.responseRepository = responseRepository;
        this.receiptRepository = receiptRepository;
        this.sessionRepository = sessionRepository;
        this.courseRepository = courseRepository;
        this.allocationRepository = allocationRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }

    // ----------------------------------------------------------------- window

    @Transactional(readOnly = true)
    public FeedbackDTOs.WindowInfo window(Long sessionId, String semester) {
        String sem = clean(semester);
        boolean open = windowRepository.findByAcademicSessionIdAndSemester(sessionId, sem)
                .map(FeedbackWindow::isOpen).orElse(false);
        return new FeedbackDTOs.WindowInfo(sessionId, sem, open);
    }

    public FeedbackDTOs.WindowInfo setWindow(String username, Long sessionId, String semester, boolean open) {
        requireHod(username);
        String sem = clean(semester);
        AcademicSession session = sessionRepository.findById(sessionId).orElseThrow(() -> bad("Academic session not found."));

        FeedbackWindow w = windowRepository.findByAcademicSessionIdAndSemester(sessionId, sem).orElseGet(FeedbackWindow::new);
        w.setAcademicSession(session);
        w.setSemester(sem);
        w.setOpen(open);
        w.setChangedAt(LocalDateTime.now());
        windowRepository.save(w);

        return new FeedbackDTOs.WindowInfo(sessionId, sem, open);
    }

    // ---------------------------------------------------------------- student

    /** The lecturers of the courses this student registered, and whether each is already rated. */
    @Transactional(readOnly = true)
    public FeedbackDTOs.Mine mine(String username, Long sessionId, String semester) {
        Student student = studentOf(username);
        String sem = clean(semester);
        AcademicSession session = sessionRepository.findById(sessionId).orElseThrow(() -> bad("Academic session not found."));

        Set<Long> registered = registeredCourseIds(student.getId(), sessionId, sem);
        List<FeedbackDTOs.Item> items = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();

        for (CourseAllocation a : allocationRepository.findByAcademicSessionIdAndSemester(sessionId, sem)) {
            if (a.getLecturer() == null || !registered.contains(a.getCourse().getId())) continue;
            if (!seen.add(a.getCourse().getId() + "-" + a.getLecturer().getId())) continue;

            boolean rated = receiptRepository
                    .existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemesterAndLecturerId(
                            student.getId(), a.getCourse().getId(), sessionId, sem, a.getLecturer().getId());

            items.add(new FeedbackDTOs.Item(
                    a.getCourse().getId(), a.getCourse().getCode(), a.getCourse().getTitle(),
                    a.getLecturer().getId(), a.getLecturer().getFullName(), rated));
        }

        items.sort(Comparator.comparing(FeedbackDTOs.Item::code));
        return new FeedbackDTOs.Mine(window(sessionId, sem).open(), session.getName(), sem, items);
    }

    /** Saves the anonymous rating and the receipt together, or neither. */
    public FeedbackDTOs.Message submit(String username, FeedbackDTOs.SubmitRequest req) {
        Student student = studentOf(username);
        String sem = clean(req.semester());

        if (!window(req.sessionId(), sem).open()) {
            throw bad("Feedback is not open for this semester.");
        }

        AcademicSession session = sessionRepository.findById(req.sessionId()).orElseThrow(() -> bad("Academic session not found."));
        Course course = courseRepository.findById(req.courseId()).orElseThrow(() -> bad("Course not found."));
        User lecturer = userRepository.findById(req.lecturerId()).orElseThrow(() -> bad("Lecturer not found."));

        if (!registeredCourseIds(student.getId(), session.getId(), sem).contains(course.getId())) {
            throw bad("You can only rate lecturers of courses you registered this semester.");
        }

        boolean teaches = allocationRepository.findForCourse(course.getId(), session.getId(), sem).stream()
                .anyMatch(a -> a.getLecturer() != null && a.getLecturer().getId().equals(lecturer.getId()));
        if (!teaches) throw bad("That lecturer is not listed for this course.");

        int clarity = score(req.clarity());
        int preparation = score(req.preparation());
        int punctuality = score(req.punctuality());
        int helpfulness = score(req.helpfulness());
        int fairness = score(req.fairness());

        String comment = req.comment() == null ? "" : req.comment().trim();
        if (comment.length() > 500) throw bad("Please keep your comment to 500 characters.");

        if (receiptRepository.existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemesterAndLecturerId(
                student.getId(), course.getId(), session.getId(), sem, lecturer.getId())) {
            throw bad("You have already rated this lecturer for this course.");
        }

        FeedbackReceipt receipt = new FeedbackReceipt();
        receipt.setStudent(student);
        receipt.setCourse(course);
        receipt.setAcademicSession(session);
        receipt.setSemester(sem);
        receipt.setLecturer(lecturer);

        FeedbackResponse response = new FeedbackResponse();
        response.setCourse(course);
        response.setAcademicSession(session);
        response.setSemester(sem);
        response.setLecturer(lecturer);
        response.setClarity(clarity);
        response.setPreparation(preparation);
        response.setPunctuality(punctuality);
        response.setHelpfulness(helpfulness);
        response.setFairness(fairness);
        response.setComment(comment.isEmpty() ? null : comment);
        response.setSubmittedOn(LocalDate.now());

        try {
            receiptRepository.save(receipt);
            responseRepository.save(response);
            receiptRepository.flush();
            responseRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw bad("You have already rated this lecturer for this course.");
        }

        return new FeedbackDTOs.Message("Thank you. Your feedback was saved anonymously.");
    }

    // ---------------------------------------------------------------- results

    /** The H.O.D sees every lecturer; a lecturer sees only their own results. */
    @Transactional(readOnly = true)
    public FeedbackDTOs.Results results(String username, Long sessionId, String semester) {
        User user = getUser(username);
        String role = String.valueOf(user.getRole()).toUpperCase();
        boolean hod = role.equals("HOD") || role.equals("ADMIN");

        if (!hod && !TEACHING_ROLES.contains(role)) throw bad("You do not have permission to see feedback results.");

        String sem = clean(semester);
        FeedbackDTOs.WindowInfo window = window(sessionId, sem);
        boolean exists = windowRepository.findByAcademicSessionIdAndSemester(sessionId, sem).isPresent();
        boolean closed = exists && !window.open();

        // Every lecturer-course pair on this semester's list, so lecturers with no ratings still appear.
        Map<String, Course> courses = new LinkedHashMap<>();
        Map<String, User> lecturers = new LinkedHashMap<>();
        for (CourseAllocation a : allocationRepository.findByAcademicSessionIdAndSemester(sessionId, sem)) {
            if (a.getLecturer() == null) continue;
            if (!hod && !a.getLecturer().getId().equals(user.getId())) continue;

            String key = a.getCourse().getId() + "-" + a.getLecturer().getId();
            courses.putIfAbsent(key, a.getCourse());
            lecturers.putIfAbsent(key, a.getLecturer());
        }

        Map<String, List<FeedbackResponse>> byPair = responseRepository.findForSemester(sessionId, sem).stream()
                .collect(Collectors.groupingBy(r -> r.getCourse().getId() + "-" + r.getLecturer().getId()));

        List<FeedbackDTOs.Result> rows = new ArrayList<>();

        for (String key : courses.keySet()) {
            Course c = courses.get(key);
            User l = lecturers.get(key);
            List<FeedbackResponse> list = byPair.getOrDefault(key, List.of());
            int count = list.size();

            boolean visible = closed && count >= MINIMUM;
            String note = null;
            if (!exists) note = "Feedback has not been opened for this semester.";
            else if (!closed) note = "Results are shown after the feedback window closes.";
            else if (count < MINIMUM) note = "Only " + count + " response(s). At least " + MINIMUM + " are needed to protect anonymity.";

            if (!visible) {
                rows.add(new FeedbackDTOs.Result(c.getId(), c.getCode(), c.getTitle(), l.getId(), l.getFullName(),
                        count, false, note, null, null, null, null, null, null, List.of()));
                continue;
            }

            double clarity = average(list.stream().mapToInt(FeedbackResponse::getClarity).average().orElse(0));
            double preparation = average(list.stream().mapToInt(FeedbackResponse::getPreparation).average().orElse(0));
            double punctuality = average(list.stream().mapToInt(FeedbackResponse::getPunctuality).average().orElse(0));
            double helpfulness = average(list.stream().mapToInt(FeedbackResponse::getHelpfulness).average().orElse(0));
            double fairness = average(list.stream().mapToInt(FeedbackResponse::getFairness).average().orElse(0));

            List<String> comments = list.stream().map(FeedbackResponse::getComment)
                    .filter(t -> t != null && !t.isBlank()).collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(comments);

            rows.add(new FeedbackDTOs.Result(c.getId(), c.getCode(), c.getTitle(), l.getId(), l.getFullName(),
                    count, true, null, clarity, preparation, punctuality, helpfulness, fairness,
                    average((clarity + preparation + punctuality + helpfulness + fairness) / 5.0), comments));
        }

        rows.sort(Comparator.comparing(FeedbackDTOs.Result::lecturerName).thenComparing(FeedbackDTOs.Result::code));
        return new FeedbackDTOs.Results(window, MINIMUM, rows);
    }

    // ---------------------------------------------------------------- helpers

    private Set<Long> registeredCourseIds(Long studentId, Long sessionId, String sem) {
        return registrationRepository.findByStudentIdAndAcademicSessionId(studentId, sessionId).stream()
                .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()) && sem.equalsIgnoreCase(r.getSemester()))
                .map(r -> r.getCourse().getId())
                .collect(Collectors.toSet());
    }

    private int score(Integer value) {
        if (value == null || value < 1 || value > 5) throw bad("Please give every question a score from 1 to 5.");
        return value;
    }

    private double average(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String clean(String semester) {
        String s = semester == null ? "" : semester.trim().toUpperCase();
        if (!s.equals("FIRST") && !s.equals("SECOND")) throw bad("Semester must be FIRST or SECOND.");
        return s;
    }

    private Student studentOf(String username) {
        Student s = getUser(username).getStudent();
        if (s == null) throw bad("Your account is not linked to a student record.");
        return s;
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