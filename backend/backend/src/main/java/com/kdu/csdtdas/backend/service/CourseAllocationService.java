package com.kdu.csdtdas.backend.service;

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

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourseAllocationService {

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

    public CourseAllocation allocate(
            Long lecturerId, Long courseId, Long academicSessionId, String semester
    ) {
        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new IllegalArgumentException("Lecturer not found."));

        CourseAllocation allocation = upsertSlot(courseId, academicSessionId, semester);

        if (allocation.getLecturer() != null && !allocation.getLecturer().getId().equals(lecturerId)) {
            throw new IllegalArgumentException(
                    "This course is already assigned to " + allocation.getLecturer().getFullName() + "."
            );
        }

        allocation.setLecturer(lecturer);
        allocation.setLecturerName(lecturer.getFullName());

        return allocationRepository.save(allocation);
    }

    /**
     * Self-selection: a lecturer/adjunct claims a course from the published
     * pool. If the H.O.D's document already created a row for this course
     * (with just a text name), this fills in the real account link.
     */
    public CourseAllocation claimCourse(
            String username, Long courseId, Long academicSessionId, String semester
    ) {
        User lecturer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!"LECTURER".equalsIgnoreCase(lecturer.getRole())
                && !"ADJUNCT".equalsIgnoreCase(lecturer.getRole())) {
            throw new IllegalArgumentException(
                    "Only lecturers or adjunct lecturers can claim courses."
            );
        }

        CourseAllocation allocation = upsertSlot(courseId, academicSessionId, semester);

        if (allocation.getLecturer() != null && !allocation.getLecturer().getId().equals(lecturer.getId())) {
            throw new IllegalArgumentException(
                    "This course has already been claimed by " + allocation.getLecturer().getFullName() + "."
            );
        }

        allocation.setLecturer(lecturer);
        allocation.setLecturerName(lecturer.getFullName());

        return allocationRepository.save(allocation);
    }

    private CourseAllocation upsertSlot(Long courseId, Long academicSessionId, String semester) {
        String cleanSemester = semester.trim().toUpperCase();

        return allocationRepository
                .findByCourseIdAndAcademicSessionIdAndSemester(courseId, academicSessionId, cleanSemester)
                .orElseGet(() -> {
                    Course course = courseRepository.findById(courseId)
                            .orElseThrow(() -> new IllegalArgumentException("Course not found."));
                    AcademicSession session = academicSessionRepository.findById(academicSessionId)
                            .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

                    CourseAllocation newAllocation = new CourseAllocation();
                    newAllocation.setCourse(course);
                    newAllocation.setAcademicSession(session);
                    newAllocation.setSemester(cleanSemester);
                    return newAllocation;
                });
    }

    @Transactional(readOnly = true)
    public List<Course> getAvailableCourses(
            Long academicSessionId, String semester, List<Course> candidatePool
    ) {
        String cleanSemester = semester.trim().toUpperCase();
        return candidatePool.stream()
                .filter(c -> allocationRepository
                        .findByCourseIdAndAcademicSessionIdAndSemester(c.getId(), academicSessionId, cleanSemester)
                        .map(a -> a.getLecturer() == null)
                        .orElse(true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseAllocation> getAll() {
        return allocationRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<CourseAllocation> getForSession(Long academicSessionId, String semester) {
        return allocationRepository.findByAcademicSessionIdAndSemester(
                academicSessionId, semester.trim().toUpperCase()
        );
    }

    @Transactional(readOnly = true)
    public List<CourseAllocation> getAllForLecturer(Long lecturerId) {
        return allocationRepository.findByLecturerId(lecturerId);
    }

    public void remove(Long id) {
        allocationRepository.deleteById(id);
    }
}