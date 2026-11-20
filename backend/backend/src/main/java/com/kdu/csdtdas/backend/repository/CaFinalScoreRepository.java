package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.CaFinalScore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CaFinalScoreRepository extends JpaRepository<CaFinalScore, Long> {
    Optional<CaFinalScore> findByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
            Long studentId, Long courseId, Long sessionId, String semester);
    List<CaFinalScore> findByCourseIdAndAcademicSessionIdAndSemester(
            Long courseId, Long sessionId, String semester);
}