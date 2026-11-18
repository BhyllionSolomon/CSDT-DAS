package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.LabManualOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LabManualOrderRepository extends JpaRepository<LabManualOrder, Long> {
    List<LabManualOrder> findAllByOrderByCreatedAtDesc();
    List<LabManualOrder> findAllByStudentIdAndAcademicSessionIdOrderByCreatedAtDesc(Long studentId, Long sessionId);
    Optional<LabManualOrder> findFirstByManualIdAndStudentIdAndAcademicSessionIdAndStatusIn(Long manualId, Long studentId, Long sessionId, List<String> statuses);
}