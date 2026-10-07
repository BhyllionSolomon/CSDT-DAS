package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.OnlineClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OnlineClassRepository extends JpaRepository<OnlineClass, Long> {

    @Query("""
        SELECT oc FROM OnlineClass oc
        JOIN FETCH oc.course c
        JOIN FETCH oc.academicSession s
        JOIN FETCH oc.lecturer l
        WHERE c.id = :courseId AND s.id = :sessionId AND oc.semester = :semester
        ORDER BY oc.createdAt DESC
    """)
    List<OnlineClass> findForCourse(
            @Param("courseId") Long courseId,
            @Param("sessionId") Long sessionId,
            @Param("semester") String semester
    );

    @Query("""
        SELECT oc FROM OnlineClass oc
        JOIN FETCH oc.course c
        JOIN FETCH oc.academicSession s
        JOIN FETCH oc.lecturer l
        WHERE EXISTS (
            SELECT 1 FROM CourseRegistration cr
            WHERE cr.student.id = :studentId
              AND cr.course.id = c.id
              AND cr.academicSession.id = s.id
              AND cr.semester = oc.semester
              AND cr.status = 'REGISTERED'
        )
        ORDER BY oc.createdAt DESC
    """)
    List<OnlineClass> findForStudent(@Param("studentId") Long studentId);

    @Query("""
        SELECT oc FROM OnlineClass oc
        JOIN FETCH oc.course c
        JOIN FETCH oc.academicSession s
        JOIN FETCH oc.lecturer l
        WHERE oc.id = :id
    """)
    Optional<OnlineClass> findByIdWithDetails(@Param("id") Long id);
}