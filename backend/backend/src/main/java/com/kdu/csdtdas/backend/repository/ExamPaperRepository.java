package com.kdu.csdtdas.backend.repository;
import com.kdu.csdtdas.backend.entity.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {
    List<ExamPaper> findByTaskIdOrderByVersionDesc(Long taskId);
    List<ExamPaper> findByLecturerIdOrderBySubmittedAtDesc(Long lecturerId);
}