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
    WHERE r.course.id = :courseId
      AND r.academicSession.id = :academicSessionId
      AND r.semester = :semester
      AND r.status = :status
""")
    List<Result> findByCourseIdAndAcademicSessionIdAndSemesterAndStatus(
            @Param("courseId") Long courseId,
            @Param("academicSessionId") Long academicSessionId,
            @Param("semester") String semester,
            @Param("status") String status
    );

    @Query("""
    SELECT r
    FROM Result r
    WHERE r.course.id = :courseId
      AND r.academicSession.id = :academicSessionId
      AND r.semester = :semester
""")
    List<Result> findByCourseIdAndAcademicSessionIdAndSemester(
            @Param("courseId") Long courseId,
            @Param("academicSessionId") Long academicSessionId,
            @Param("semester") String semester
    );





    @Query("""
        SELECT r
        FROM Result r
        JOIN FETCH r.course c
        JOIN FETCH r.academicSession s
        WHERE r.student.id = :studentId
          AND s.id = :sessionId
          AND r.semester = :semester
          AND r.status = 'APPROVED'
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
          AND r.status = 'APPROVED'
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

    @Query("""
        SELECT r
        FROM Result r
        JOIN FETCH r.student st
        JOIN FETCH r.course c
        JOIN FETCH r.academicSession s
        WHERE r.id = :id
    """)
    Optional<Result> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT r
        FROM Result r
        JOIN FETCH r.student st
        JOIN FETCH r.course c
        JOIN FETCH r.academicSession s
        WHERE r.student.id = :studentId
        ORDER BY s.startDate ASC, r.semester ASC, c.code ASC
    """)
    List<Result> findByStudentIdWithDetails(
            @Param("studentId") Long studentId
    );

    @Query("""
        SELECT r
        FROM Result r
        JOIN FETCH r.student st
        JOIN FETCH r.course c
        JOIN FETCH r.academicSession s
        WHERE r.student.id = :studentId
          AND s.id = :academicSessionId
        ORDER BY r.semester ASC, c.code ASC
    """)
    List<Result> findByStudentIdAndAcademicSessionIdWithDetails(
            @Param("studentId") Long studentId,
            @Param("academicSessionId") Long academicSessionId
    );

    @Query("""
    SELECT r
    FROM Result r
    JOIN FETCH r.student st
    JOIN FETCH r.course c
    JOIN FETCH r.academicSession s
    WHERE s.id = :sessionId
      AND r.semester = :semester
      AND r.status = 'APPROVED'
    ORDER BY st.matricNumber, c.code
""")
    List<Result> findApprovedResultsBySessionAndSemester(
            @Param("sessionId") Long sessionId,
            @Param("semester") String semester
    );
}