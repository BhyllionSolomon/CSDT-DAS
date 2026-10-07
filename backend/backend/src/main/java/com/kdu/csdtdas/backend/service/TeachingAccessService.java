package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeachingAccessService {

    private final CourseAllocationRepository allocationRepository;
    private final CourseRegistrationRepository registrationRepository;

    public TeachingAccessService(
            CourseAllocationRepository allocationRepository,
            CourseRegistrationRepository registrationRepository
    ) {
        this.allocationRepository = allocationRepository;
        this.registrationRepository = registrationRepository;
    }

    public boolean isPrivileged(User user) {
        return "HOD".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole());
    }

    /** True for the H.O.D/admin, or the lecturer allocated to this exact course slot. */
    public boolean canTeach(User user, Long courseId, Long sessionId, String semester) {
        if (isPrivileged(user)) return true;

        return allocationRepository
                .findByCourseIdAndAcademicSessionIdAndSemester(courseId, sessionId, semester)
                .map(a -> a.getLecturer() != null && a.getLecturer().getId().equals(user.getId()))
                .orElse(false);
    }

    public void requireTeachingRights(User user, Long courseId, Long sessionId, String semester) {
        if (!canTeach(user, courseId, sessionId, semester)) {
            throw new IllegalArgumentException(
                    "You are not assigned to teach this course for the selected session and semester."
            );
        }
    }

    public boolean isRegistered(User user, Long courseId, Long sessionId, String semester) {
        if (user.getStudent() == null) return false;

        return registrationRepository.existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
                user.getStudent().getId(), courseId, sessionId, semester
        );
    }
}