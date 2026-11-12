package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.AttendanceMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceMarkRepository extends JpaRepository<AttendanceMark, Long> {

    List<AttendanceMark> findByAttendanceClassId(Long classId);

    void deleteByAttendanceClassId(Long classId);

    @Query("""
        SELECT m FROM AttendanceMark m
        JOIN FETCH m.attendanceClass c
        JOIN FETCH m.student
        WHERE c.course.id = :courseId AND c.academicSession.id = :sessionId AND c.semester = :semester
    """)
    List<AttendanceMark> findForCourse(@Param("courseId") Long courseId, @Param("sessionId") Long sessionId, @Param("semester") String semester);

    @Query("""
        SELECT m FROM AttendanceMark m
        JOIN FETCH m.attendanceClass c
        JOIN FETCH c.course
        WHERE m.student.id = :studentId AND c.academicSession.id = :sessionId AND c.semester = :semester
        ORDER BY c.classDate
    """)
    List<AttendanceMark> findForStudent(@Param("studentId") Long studentId, @Param("sessionId") Long sessionId, @Param("semester") String semester);
}