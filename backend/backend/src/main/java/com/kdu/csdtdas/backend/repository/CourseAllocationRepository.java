package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.CourseAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseAllocationRepository extends JpaRepository<CourseAllocation, Long> {

    @Query("""
        SELECT ca
        FROM CourseAllocation ca
        JOIN FETCH ca.lecturer l
        JOIN FETCH ca.course c
        JOIN FETCH ca.academicSession s
        ORDER BY s.startDate DESC, ca.semester, c.code
    """)
    List<CourseAllocation> findAllWithDetails();

    @Query("""
        SELECT ca
        FROM CourseAllocation ca
        JOIN FETCH ca.lecturer l
        JOIN FETCH ca.course c
        JOIN FETCH ca.academicSession s
        WHERE l.id = :lecturerId
          AND s.id = :sessionId
          AND ca.semester = :semester
        ORDER BY c.code
    """)
    List<CourseAllocation> findByLecturerAndSessionAndSemester(
            @Param("lecturerId") Long lecturerId,
            @Param("sessionId") Long sessionId,
            @Param("semester") String semester
    );

    @Query("""
        SELECT ca
        FROM CourseAllocation ca
        JOIN FETCH ca.lecturer l
        JOIN FETCH ca.course c
        JOIN FETCH ca.academicSession s
        WHERE l.id = :lecturerId
        ORDER BY s.startDate DESC, ca.semester, c.code
    """)
    List<CourseAllocation> findByLecturerId(@Param("lecturerId") Long lecturerId);

    boolean existsByLecturerIdAndCourseIdAndAcademicSessionIdAndSemester(
            Long lecturerId,
            Long courseId,
            Long academicSessionId,
            String semester
    );
}