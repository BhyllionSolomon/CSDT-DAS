package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.CourseRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {

    List<CourseRegistration> findByStudentId(Long studentId);

    List<CourseRegistration> findByStudentIdAndAcademicSessionId(
            Long studentId,
            Long academicSessionId
    );

    boolean existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
            Long studentId,
            Long courseId,
            Long academicSessionId,
            String semester
    );
}