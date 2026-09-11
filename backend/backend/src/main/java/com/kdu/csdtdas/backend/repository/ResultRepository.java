package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResultRepository extends JpaRepository<Result, Long> {

    @Query("""
        SELECT r
        FROM Result r
        JOIN FETCH r.course c
        JOIN FETCH r.academicSession s
        WHERE r.student.id = :studentId
          AND s.id = :sessionId
          AND r.semester = :semester
        ORDER BY c.code
    """)
    List<Result> findSemesterResults(
            @Param("studentId") Long studentId,
            @Param("sessionId") Long sessionId,
            @Param("semester") String semester
    );

    @Query("""
        SELECT r
        FROM Result r
        JOIN FETCH r.course c
        JOIN FETCH r.academicSession s
        WHERE r.student.id = :studentId
        ORDER BY s.startDate ASC, r.semester ASC, c.code ASC
    """)
    List<Result> findFullAcademicHistory(
            @Param("studentId") Long studentId
    );

    Optional<Result>
    findByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
            Long studentId,
            Long courseId,
            Long academicSessionId,
            String semester
    );

    boolean existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
            Long studentId,
            Long courseId,
            Long academicSessionId,
            String semester
    );

    List<Result> findByStudentId(Long studentId);

    List<Result> findByStudentIdAndAcademicSessionId(
            Long studentId,
            Long academicSessionId
    );
}