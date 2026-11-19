package com.kdu.csdtdas.backend.repository;
import com.kdu.csdtdas.backend.entity.ExamCourseTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ExamCourseTaskRepository extends JpaRepository<ExamCourseTask, Long> {
    List<ExamCourseTask> findByCycleIdOrderByExamDateAsc(Long cycleId);
    Optional<ExamCourseTask> findByCycleIdAndCourseId(Long cycleId, Long courseId);
}