package com.kdu.csdtdas.backend.repository;
import com.kdu.csdtdas.backend.entity.ExamAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExamAuditEventRepository extends JpaRepository<ExamAuditEvent,Long>{
 java.util.List<ExamAuditEvent> findTop300ByOrderByOccurredAtDesc();
}