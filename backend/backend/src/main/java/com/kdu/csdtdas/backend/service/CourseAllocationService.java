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
            Long lecturerId,
            Long courseId,
            Long academicSessionId,
            String semester
    ) {

        if (semester == null || semester.isBlank()) {
            throw new IllegalArgumentException("Semester is required.");
        }

        String cleanSemester = semester.trim().toUpperCase();

        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new IllegalArgumentException("Lecturer not found."));

        if (!"LECTURER".equalsIgnoreCase(lecturer.getRole())) {
            throw new IllegalArgumentException(
                    "The selected user is not registered with the LECTURER role."
            );
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));

        AcademicSession session = academicSessionRepository.findById(academicSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        boolean exists = allocationRepository
                .existsByLecturerIdAndCourseIdAndAcademicSessionIdAndSemester(
                        lecturerId, courseId, academicSessionId, cleanSemester
                );

        if (exists) {
            throw new IllegalArgumentException(
                    "This lecturer is already allocated to this course for this session and semester."
            );
        }

        CourseAllocation allocation = new CourseAllocation();
        allocation.setLecturer(lecturer);
        allocation.setCourse(course);
        allocation.setAcademicSession(session);
        allocation.setSemester(cleanSemester);

        return allocationRepository.save(allocation);
    }

    @Transactional(readOnly = true)
    public List<CourseAllocation> getAll() {
        return allocationRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<CourseAllocation> getForLecturer(
            Long lecturerId,
            Long academicSessionId,
            String semester
    ) {
        return allocationRepository.findByLecturerAndSessionAndSemester(
                lecturerId,
                academicSessionId,
                semester.trim().toUpperCase()
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