package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByStudentId(Long studentId);

    List<Result> findByStudentIdAndAcademicSessionId(
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