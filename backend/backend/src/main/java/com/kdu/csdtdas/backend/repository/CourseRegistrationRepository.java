package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.CourseRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {

    @Query("""
        SELECT cr
        FROM CourseRegistration cr
        JOIN FETCH cr.student st
        JOIN FETCH cr.course c
        JOIN FETCH cr.academicSession s
        WHERE st.id = :studentId
        ORDER BY s.startDate DESC, cr.semester, c.code
    """)
    List<CourseRegistration> findByStudentId(@Param("studentId") Long studentId);

    @Query("""
        SELECT cr
        FROM CourseRegistration cr
        JOIN FETCH cr.student st
        JOIN FETCH cr.course c
        JOIN FETCH cr.academicSession s
        WHERE st.id = :studentId
          AND s.id = :academicSessionId
        ORDER BY cr.semester, c.code
    """)
    List<CourseRegistration> findByStudentIdAndAcademicSessionId(
            @Param("studentId") Long studentId,
            @Param("academicSessionId") Long academicSessionId
    );

    @Query("""
        SELECT cr
        FROM CourseRegistration cr
        JOIN FETCH cr.student st
        JOIN FETCH cr.course c
        JOIN FETCH cr.academicSession s
        WHERE c.id = :courseId
          AND s.id = :academicSessionId
          AND cr.semester = :semester
          AND cr.status = 'REGISTERED'
        ORDER BY st.matricNumber
    """)
    List<CourseRegistration> findByCourseIdAndAcademicSessionIdAndSemester(
            @Param("courseId") Long courseId,
            @Param("academicSessionId") Long academicSessionId,
            @Param("semester") String semester
    );

    boolean existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
            Long studentId,
            Long courseId,
            Long academicSessionId,
            String semester
    );
}