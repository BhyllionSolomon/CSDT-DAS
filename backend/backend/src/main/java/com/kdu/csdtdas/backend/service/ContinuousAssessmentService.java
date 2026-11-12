package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CaDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.CaItem;
import com.kdu.csdtdas.backend.entity.CaScore;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CaItemRepository;
import com.kdu.csdtdas.backend.repository.CaScoreRepository;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
public class ContinuousAssessmentService {

    /** Continuous assessment is out of 30; the exam is out of 70. */
    public static final BigDecimal CAP = BigDecimal.valueOf(30);

    private static final Set<String> CATEGORIES = Set.of("PRACTICAL", "ASSIGNMENT", "PRESENTATION", "TEST");
    private static final Set<String> STAFF = Set.of("HOD", "ADMIN", "LECTURER", "ADJUNCT", "LEVEL_ADVISER");

    private final CaItemRepository itemRepository;
    private final CaScoreRepository scoreRepository;
    private final CourseRepository courseRepository;
    private final AcademicSessionRepository sessionRepository;
    private final CourseAllocationRepository allocationRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final TeachingAccessService accessService;
    private final UserRepository userRepository;

    public ContinuousAssessmentService(
            CaItemRepository itemRepository,
            CaScoreRepository scoreRepository,
            CourseRepository courseRepository,
            AcademicSessionRepository sessionRepository,
            CourseAllocationRepository allocationRepository,
            CourseRegistrationRepository registrationRepository,
            TeachingAccessService accessService,
            UserRepository userRepository
    ) {
        this.itemRepository = itemRepository;
        this.scoreRepository = scoreRepository;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
        this.allocationRepository = allocationRepository;
        this.registrationRepository = registrationRepository;
        this.accessService = accessService;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ staff

    @Transactional(readOnly = true)
    public List<CaDTOs.CourseInfo> courses(String username, Long sessionId, String semester) {
        User user = staff(username);
        String sem = clean(semester);
        boolean hod = isHod(user);

        Map<Long, Course> found = new LinkedHashMap<>();
        for (CourseAllocation a : allocationRepository.findByAcademicSessionIdAndSemester(sessionId, sem)) {
            if (!hod && (a.getLecturer() == null || !a.getLecturer().getId().equals(user.getId()))) continue;
            found.putIfAbsent(a.getCourse().getId(), a.getCourse());
        }

        List<CaDTOs.CourseInfo> out = new ArrayList<>();
        for (Course c : found.values()) {
            out.add(new CaDTOs.CourseInfo(c.getId(), c.getCode(), c.getTitle(),
                    roster(c.getId(), sessionId, sem).size(), allocated(c.getId(), sessionId, sem, null).doubleValue()));
        }
        out.sort(Comparator.comparing(CaDTOs.CourseInfo::code));
        return out;
    }

    @Transactional(readOnly = true)
    public CaDTOs.Sheet sheet(String username, Long courseId, Long sessionId, String semester) {
        String sem = clean(semester);
        authorize(staff(username), courseId, sessionId, sem);

        List<CaItem> items = itemRepository.findByCourseIdAndAcademicSessionIdAndSemesterOrderByIdAsc(courseId, sessionId, sem);

        Map<Long, Map<Long, BigDecimal>> byStudent = new HashMap<>();
        Map<Long, Integer> marked = new HashMap<>();
        for (CaScore s : scoreRepository.findForCourse(courseId, sessionId, sem)) {
            byStudent.computeIfAbsent(s.getStudent().getId(), k -> new HashMap<>()).put(s.getItem().getId(), s.getScore());
            marked.merge(s.getItem().getId(), 1, Integer::sum);
        }

        List<CaDTOs.ItemRow> itemRows = items.stream()
                .map(i -> new CaDTOs.ItemRow(i.getId(), i.getCategory(), i.getTitle(), i.getMaxMark().doubleValue(),
                        i.isReleased(), marked.getOrDefault(i.getId(), 0)))
                .toList();

        List<CaDTOs.StudentRow> rows = new ArrayList<>();
        for (Student s : roster(courseId, sessionId, sem)) {
            Map<Long, BigDecimal> mine = byStudent.getOrDefault(s.getId(), Map.of());
            Map<Long, Double> scores = new LinkedHashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<Long, BigDecimal> e : mine.entrySet()) {
                scores.put(e.getKey(), e.getValue().doubleValue());
                total = total.add(e.getValue());
            }
            rows.add(new CaDTOs.StudentRow(s.getId(), s.getMatricNumber(), s.getFullName(), scores,
                    total.setScale(2, RoundingMode.HALF_UP).doubleValue()));
        }

        return new CaDTOs.Sheet(CAP.doubleValue(), allocated(courseId, sessionId, sem, null).doubleValue(), itemRows, rows);
    }

    public CaDTOs.ItemRow createItem(String username, CaDTOs.ItemRequest req) {
        User user = staff(username);
        String sem = clean(req.semester());
        authorize(user, req.courseId(), req.sessionId(), sem);

        String category = req.category() == null ? "" : req.category().trim().toUpperCase();
        if (!CATEGORIES.contains(category)) throw bad("Choose Practical, Assignment, Class presentation or Test.");

        String title = title(req.title());
        BigDecimal max = max(req.maxMark());
        checkRoom(req.courseId(), req.sessionId(), sem, null, max);

        Course course = courseRepository.findById(req.courseId()).orElseThrow(() -> bad("Course not found."));
        AcademicSession session = sessionRepository.findById(req.sessionId()).orElseThrow(() -> bad("Academic session not found."));

        CaItem item = new CaItem();
        item.setCourse(course);
        item.setAcademicSession(session);
        item.setSemester(sem);
        item.setCategory(category);
        item.setTitle(title);
        item.setMaxMark(max);
        item.setCreatedBy(user.getUsername());
        CaItem saved = itemRepository.save(item);

        return new CaDTOs.ItemRow(saved.getId(), category, title, max.doubleValue(), false, 0);
    }

    public CaDTOs.ItemRow updateItem(String username, Long id, CaDTOs.ItemUpdate req) {
        CaItem item = item(id);
        authorize(staff(username), item.getCourse().getId(), item.getAcademicSession().getId(), item.getSemester());

        if (req.title() != null) item.setTitle(title(req.title()));

        if (req.maxMark() != null) {
            BigDecimal max = max(req.maxMark());
            checkRoom(item.getCourse().getId(), item.getAcademicSession().getId(), item.getSemester(), item.getId(), max);

            boolean someoneAbove = scoreRepository.findByItemId(id).stream().anyMatch(s -> s.getScore().compareTo(max) > 0);
            if (someoneAbove) throw bad("Some students already have more than " + max.stripTrailingZeros().toPlainString() + " here. Correct their scores first.");
            item.setMaxMark(max);
        }

        if (req.released() != null) item.setReleased(req.released());
        itemRepository.save(item);

        return new CaDTOs.ItemRow(item.getId(), item.getCategory(), item.getTitle(), item.getMaxMark().doubleValue(),
                item.isReleased(), scoreRepository.findByItemId(id).size());
    }

    public void deleteItem(String username, Long id) {
        CaItem item = item(id);
        authorize(staff(username), item.getCourse().getId(), item.getAcademicSession().getId(), item.getSemester());
        scoreRepository.deleteByItemId(id);
        itemRepository.delete(item);
    }

    /** Saves one column of the grid. An empty score clears that student's mark. */
    public void saveScores(String username, Long itemId, List<CaDTOs.ScoreEntry> entries) {
        User user = staff(username);
        CaItem item = item(itemId);
        Long courseId = item.getCourse().getId();
        authorize(user, courseId, item.getAcademicSession().getId(), item.getSemester());

        if (entries == null || entries.isEmpty()) return;

        Map<Long, Student> onRoster = roster(courseId, item.getAcademicSession().getId(), item.getSemester()).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, CaScore> current = scoreRepository.findByItemId(itemId).stream()
                .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s));

        Set<Long> seen = new HashSet<>();
        for (CaDTOs.ScoreEntry e : entries) {
            Student student = onRoster.get(e.studentId());
            if (student == null) throw bad("A student in this list is not registered for the course.");
            if (!seen.add(student.getId())) throw bad("A student appears twice in this list.");

            CaScore existing = current.get(student.getId());

            if (e.score() == null) {
                if (existing != null) scoreRepository.delete(existing);
                continue;
            }

            BigDecimal value = e.score().setScale(2, RoundingMode.HALF_UP);
            if (value.signum() < 0) throw bad("A score cannot be negative.");
            if (value.compareTo(item.getMaxMark()) > 0) {
                throw bad(student.getFullName() + " has " + value.stripTrailingZeros().toPlainString()
                        + ", but " + item.getTitle() + " is out of " + item.getMaxMark().stripTrailingZeros().toPlainString() + ".");
            }

            CaScore s = existing != null ? existing : new CaScore();
            s.setItem(item);
            s.setStudent(student);
            s.setScore(value);
            s.setRecordedBy(user.getUsername());
            s.setRecordedAt(LocalDateTime.now());
            scoreRepository.save(s);
        }
    }

    // ---------------------------------------------------------------- student

    /** A student sees only their own released assessments. */
    @Transactional(readOnly = true)
    public CaDTOs.Mine mine(String username, Long sessionId, String semester) {
        Student student = userRepository.findByUsername(username).map(User::getStudent).orElse(null);
        if (student == null) throw bad("Your account is not linked to a student record.");
        String sem = clean(semester);

        Map<Long, BigDecimal> scores = new HashMap<>();
        for (CaScore s : scoreRepository.findForStudent(student.getId(), sessionId, sem)) scores.put(s.getItem().getId(), s.getScore());

        List<CaDTOs.MyCourse> courses = new ArrayList<>();
        for (CourseRegistration reg : registrationRepository.findByStudentIdAndAcademicSessionId(student.getId(), sessionId)) {
            if (!"REGISTERED".equalsIgnoreCase(reg.getStatus()) || !sem.equalsIgnoreCase(reg.getSemester())) continue;
            Course c = reg.getCourse();

            List<CaDTOs.MyItem> items = new ArrayList<>();
            BigDecimal earned = BigDecimal.ZERO;
            BigDecimal releasedMax = BigDecimal.ZERO;

            for (CaItem i : itemRepository.findByCourseIdAndAcademicSessionIdAndSemesterOrderByIdAsc(c.getId(), sessionId, sem)) {
                if (!i.isReleased()) continue;
                BigDecimal score = scores.get(i.getId());
                items.add(new CaDTOs.MyItem(i.getCategory(), i.getTitle(), i.getMaxMark().doubleValue(),
                        score == null ? null : score.doubleValue()));
                releasedMax = releasedMax.add(i.getMaxMark());
                if (score != null) earned = earned.add(score);
            }
            courses.add(new CaDTOs.MyCourse(c.getId(), c.getCode(), c.getTitle(), items,
                    earned.doubleValue(), releasedMax.doubleValue()));
        }
        courses.sort(Comparator.comparing(CaDTOs.MyCourse::code));
        return new CaDTOs.Mine(CAP.doubleValue(), courses);
    }

    // ---------------------------------------------------------------- helpers

    /** Marks already given out to assessments in this course and semester, leaving one item out. */
    private BigDecimal allocated(Long courseId, Long sessionId, String sem, Long exceptItemId) {
        return itemRepository.findByCourseIdAndAcademicSessionIdAndSemesterOrderByIdAsc(courseId, sessionId, sem).stream()
                .filter(i -> exceptItemId == null || !i.getId().equals(exceptItemId))
                .map(CaItem::getMaxMark)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void checkRoom(Long courseId, Long sessionId, String sem, Long exceptItemId, BigDecimal max) {
        BigDecimal left = CAP.subtract(allocated(courseId, sessionId, sem, exceptItemId));
        if (max.compareTo(left) > 0) {
            throw bad("Continuous assessment is out of 30, and only " + left.stripTrailingZeros().toPlainString()
                    + " mark(s) are left to allocate in this course.");
        }
    }

    private BigDecimal max(BigDecimal v) {
        if (v == null || v.signum() <= 0) throw bad("Enter the maximum mark, greater than zero.");
        if (v.compareTo(CAP) > 0) throw bad("An assessment cannot be worth more than 30 marks.");
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private String title(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.isEmpty() || t.length() > 80) throw bad("Give the assessment a title of up to 80 characters.");
        return t;
    }

    private List<Student> roster(Long courseId, Long sessionId, String sem) {
        return registrationRepository.findByCourseIdAndAcademicSessionIdAndSemester(courseId, sessionId, sem).stream()
                .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()))
                .map(CourseRegistration::getStudent)
                .sorted(Comparator.comparing(Student::getMatricNumber))
                .toList();
    }

    private CaItem item(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> bad("Assessment not found."));
    }

    private void authorize(User user, Long courseId, Long sessionId, String sem) {
        if (!accessService.canTeach(user, courseId, sessionId, sem)) {
            throw bad("You are not assigned to teach this course for this session and semester.");
        }
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