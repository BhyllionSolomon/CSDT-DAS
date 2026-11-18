package com.kdu.csdtdas.backend.repository;
import com.kdu.csdtdas.backend.entity.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {
    Optional<ExamPaper> findByTaskIdAndLecturerId(Long taskId, Long lecturerId);
    List<ExamPaper> findByTaskIdOrderBySubmittedAtAsc(Long taskId);
    List<ExamPaper> findByLecturerIdOrderBySubmittedAtDesc(Long lecturerId);
}