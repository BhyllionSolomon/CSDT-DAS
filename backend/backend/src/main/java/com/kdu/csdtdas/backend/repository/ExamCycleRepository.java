package com.kdu.csdtdas.backend.repository;
import com.kdu.csdtdas.backend.entity.ExamCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ExamCycleRepository extends JpaRepository<ExamCycle, Long> {
    Optional<ExamCycle> findByAcademicSessionIdAndSemester(Long sessionId, String semester);
}