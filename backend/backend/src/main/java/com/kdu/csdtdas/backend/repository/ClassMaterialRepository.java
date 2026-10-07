package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ClassMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassMaterialRepository extends JpaRepository<ClassMaterial, Long> {

    @Query("""
        SELECT m FROM ClassMaterial m
        JOIN FETCH m.course c
        JOIN FETCH m.academicSession s
        JOIN FETCH m.uploadedBy u
        WHERE c.id = :courseId AND s.id = :sessionId AND m.semester = :semester
        ORDER BY m.createdAt DESC
    """)
    List<ClassMaterial> findForCourse(
            @Param("courseId") Long courseId,
            @Param("sessionId") Long sessionId,
            @Param("semester") String semester
    );

    @Query("""
        SELECT m FROM ClassMaterial m
        JOIN FETCH m.course c
        JOIN FETCH m.academicSession s
        JOIN FETCH m.uploadedBy u
        WHERE EXISTS (
            SELECT 1 FROM CourseRegistration cr
            WHERE cr.student.id = :studentId
              AND cr.course.id = c.id
              AND cr.academicSession.id = s.id
              AND cr.semester = m.semester
              AND cr.status = 'REGISTERED'
        )
        ORDER BY m.createdAt DESC
    """)
    List<ClassMaterial> findForStudent(@Param("studentId") Long studentId);

    @Query("""
        SELECT m FROM ClassMaterial m
        JOIN FETCH m.course c
        JOIN FETCH m.academicSession s
        JOIN FETCH m.uploadedBy u
        WHERE m.id = :id
    """)
    Optional<ClassMaterial> findByIdWithDetails(@Param("id") Long id);
}