package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.AllocationDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseAllocationService {

    private static final Set<String> TEACHING_ROLES = Set.of("LECTURER", "ADJUNCT", "LEVEL_ADVISER");

    private final CourseAllocationRepository allocationRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AcademicSessionRepository academicSessionRepository;

    public CourseAllocationService(
            CourseAllocationRepository allocationRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            AcademicSessionRepository academicSessionRepository
    ) {
        this.allocationRepository = allocationRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.academicSessionRepository = academicSessionRepository;
    }

    // ---------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public List<AllocationDTOs.Entry> getAll() {
        return allocationRepository.findAllWithDetails().stream().map(AllocationDTOs.Entry::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AllocationDTOs.Entry> getForSession(Long sessionId, String semester) {
        return rows(sessionId, semester).stream().map(AllocationDTOs.Entry::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AllocationDTOs.Entry> getAllForLecturer(Long lecturerId) {
        return allocationRepository.findByLecturerId(lecturerId).stream().map(AllocationDTOs.Entry::from).toList();
    }

    /** What the signed-in lecturer can do for this session and semester. */
    @Transactional(readOnly = true)
    public AllocationDTOs.MyOptions optionsFor(String username, Long sessionId, String semester) {

        User user = getUser(username);
        List<CourseAllocation> rows = rows(sessionId, semester);

        Set<Long> mineCourses = rows.stream()
                .filter(r -> isMine(r, user))
                .map(r -> r.getCourse().getId())
                .collect(Collectors.toSet());

        List<AllocationDTOs.Entry> confirm = new ArrayList<>();
        List<AllocationDTOs.Entry> mine = new ArrayList<>();
        List<AllocationDTOs.Entry> open = new ArrayList<>();
        Set<Long> openSeen = new HashSet<>();

        for (CourseAllocation r : rows) {
            Long courseId = r.getCourse().getId();

            if (isMine(r, user)) {
                mine.add(AllocationDTOs.Entry.from(r));
            } else if (mineCourses.contains(courseId)) {
                continue;
            } else if (r.getLecturer() == null && hasName(r)
                    && NameMatcher.matches(r.getLecturerName(), user.getFullName())) {
                confirm.add(AllocationDTOs.Entry.from(r));
            } else if (r.getLecturer() == null && !hasName(r) && openSeen.add(courseId)) {
                open.add(AllocationDTOs.Entry.from(r));
            }
        }

        return new AllocationDTOs.MyOptions(confirm, mine, open);
    }

    // --------------------------------------------------------------- writes

    /** A lecturer confirms the entry listed in their name, or claims a course with no lecturer yet. */
    public AllocationDTOs.Entry claim(String username, Long courseId, Long sessionId, String semester) {

        User user = getUser(username);
        if (!TEACHING_ROLES.contains(String.valueOf(user.getRole()).toUpperCase())) {
            throw new IllegalArgumentException("Only lecturers or adjunct lecturers can claim courses.");
        }

        String sem = clean(semester);
        List<CourseAllocation> rows = allocationRepository.findForCourse(courseId, sessionId, sem);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "This course is not on the course list for the selected session and semester."
            );
        }
        if (rows.stream().anyMatch(r -> isMine(r, user))) {
            throw new IllegalArgumentException("You have already claimed this course.");
        }

        CourseAllocation target = rows.stream()
                .filter(r -> r.getLecturer() == null && hasName(r)
                        && NameMatcher.matches(r.getLecturerName(), user.getFullName()))
                .findFirst()
                .or(() -> rows.stream().filter(r -> r.getLecturer() == null && !hasName(r)).findFirst())
                .orElseThrow(() -> new IllegalArgumentException(
                        "This course already has lecturer(s) on it. If you should teach it too, ask the H.O.D to add you."
                ));

        link(target, user, "CLAIM");
        return AllocationDTOs.Entry.from(allocationRepository.save(target));
    }

    /** Puts a course on the semester's list with no lecturer yet. */
    public AllocationDTOs.Entry offer(Long courseId, Long sessionId, String semester) {

        String sem = clean(semester);
        Course course = findCourse(courseId);
        AcademicSession session = findSession(sessionId);

        List<CourseAllocation> rows = allocationRepository.findForCourse(courseId, sessionId, sem);
        CourseAllocation row = rows.isEmpty()
                ? allocationRepository.save(newRow(course, session, sem, "HOD"))
                : rows.get(0);

        return AllocationDTOs.Entry.from(row);
    }

    /** The H.O.D adds a lecturer to a course: by account (confirmed) or by name only. */
    public AllocationDTOs.Entry addLecturer(
            Long courseId, Long sessionId, String semester, Long lecturerId, String lecturerName
    ) {
        String sem = clean(semester);
        Course course = findCourse(courseId);
        AcademicSession session = findSession(sessionId);
        List<CourseAllocation> rows = allocationRepository.findForCourse(courseId, sessionId, sem);

        if (lecturerId != null) {
            User user = userRepository.findById(lecturerId)
                    .orElseThrow(() -> new IllegalArgumentException("Lecturer account not found."));

            if (!TEACHING_ROLES.contains(String.valueOf(user.getRole()).toUpperCase())) {
                throw new IllegalArgumentException("That account is not a lecturer.");
            }
            if (rows.stream().anyMatch(r -> isMine(r, user))) {
                throw new IllegalArgumentException(user.getFullName() + " is already on this course.");
            }

            CourseAllocation target = rows.stream()
                    .filter(r -> r.getLecturer() == null && hasName(r)
                            && NameMatcher.matches(r.getLecturerName(), user.getFullName()))
                    .findFirst()
                    .or(() -> rows.stream().filter(r -> r.getLecturer() == null && !hasName(r)).findFirst())
                    .orElseGet(() -> newRow(course, session, sem, "HOD"));

            link(target, user, "HOD");
            return AllocationDTOs.Entry.from(allocationRepository.save(target));
        }

        String name = lecturerName == null ? "" : lecturerName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Choose a lecturer account or type a name.");
        }

        return listName(course, session, sem, name, "HOD")
                .map(AllocationDTOs.Entry::from)
                .orElseThrow(() -> new IllegalArgumentException(name + " is already listed for this course."));
    }

    /** The H.O.D links a listed name to a signed-up account when automatic matching could not. */
    public AllocationDTOs.Entry linkToAccount(Long allocationId, Long userId) {

        CourseAllocation row = allocationRepository.findByIdWithDetails(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Lecturer account not found."));

        if (row.getLecturer() != null) {
            throw new IllegalArgumentException("This entry is already linked to an account.");
        }
        if (!TEACHING_ROLES.contains(String.valueOf(user.getRole()).toUpperCase())) {
            throw new IllegalArgumentException("That account is not a lecturer.");
        }

        boolean alreadyOnCourse = allocationRepository
                .findForCourse(row.getCourse().getId(), row.getAcademicSession().getId(), row.getSemester())
                .stream().anyMatch(r -> isMine(r, user));
        if (alreadyOnCourse) {
            throw new IllegalArgumentException(user.getFullName() + " is already on this course.");
        }

        link(row, user, row.getSource());
        return AllocationDTOs.Entry.from(allocationRepository.save(row));
    }

    public void remove(Long id) {
        allocationRepository.deleteById(id);
    }

    // ----------------------------------------- helpers used by the document import

    /** Opens a lecturer-less slot if the course has no entry yet. True if one was created. */
    public boolean ensureOpenSlot(Course course, AcademicSession session, String semester, String source) {
        if (!allocationRepository.findForCourse(course.getId(), session.getId(), semester).isEmpty()) {
            return false;
        }
        allocationRepository.save(newRow(course, session, semester, source));
        return true;
    }

    /** Lists a lecturer's name on a course. Empty if that person is already on it. */
    public Optional<CourseAllocation> listName(
            Course course, AcademicSession session, String semester, String name, String source
    ) {
        List<CourseAllocation> rows = allocationRepository.findForCourse(course.getId(), session.getId(), semester);
        String normalized = NameMatcher.normalize(name);

        for (CourseAllocation r : rows) {
            String existing = r.getLecturer() != null ? r.getLecturer().getFullName() : r.getLecturerName();
            if (existing == null || existing.isBlank()) continue;

            if (NameMatcher.normalize(existing).equals(normalized) || NameMatcher.matches(name, existing)) {
                return Optional.empty();
            }
        }

        CourseAllocation target = rows.stream()
                .filter(r -> r.getLecturer() == null && !hasName(r))
                .findFirst()
                .orElseGet(() -> newRow(course, session, semester, source));

        target.setLecturerName(name);
        return Optional.of(allocationRepository.save(target));
    }

    // -------------------------------------------------------------- internals

    private List<CourseAllocation> rows(Long sessionId, String semester) {
        return allocationRepository.findByAcademicSessionIdAndSemester(sessionId, clean(semester));
    }

    private CourseAllocation newRow(Course course, AcademicSession session, String semester, String source) {
        CourseAllocation row = new CourseAllocation();
        row.setCourse(course);
        row.setAcademicSession(session);
        row.setSemester(semester);
        row.setSource(source);
        return row;
    }

    private void link(CourseAllocation row, User user, String source) {
        row.setLecturer(user);
        row.setLecturerName(user.getFullName());
        row.setPhoneNumber(user.getPhoneNumber());
        row.setClaimedAt(LocalDateTime.now());
        if (row.getSource() == null) row.setSource(source);
    }

    private boolean isMine(CourseAllocation r, User user) {
        return r.getLecturer() != null && r.getLecturer().getId().equals(user.getId());
    }

    private boolean hasName(CourseAllocation r) {
        return r.getLecturerName() != null && !r.getLecturerName().isBlank();
    }

    private String clean(String semester) {
        if (semester == null || semester.isBlank()) throw new IllegalArgumentException("Semester is required.");
        return semester.trim().toUpperCase();
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found."));
    }

    private AcademicSession findSession(Long id) {
        return academicSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }
}