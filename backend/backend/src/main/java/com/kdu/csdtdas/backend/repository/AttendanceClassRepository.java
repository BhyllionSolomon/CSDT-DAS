package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.AttendanceClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceClassRepository extends JpaRepository<AttendanceClass, Long> {

    List<AttendanceClass> findByCourseIdAndAcademicSessionIdAndSemesterOrderByClassDateDesc(
            Long courseId, Long sessionId, String semester);

    Optional<AttendanceClass> findByCourseIdAndAcademicSessionIdAndSemesterAndClassDateAndKind(
            Long courseId, Long sessionId, String semester, LocalDate classDate, String kind);
}