package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.CaSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CaSubmissionRepository extends JpaRepository<CaSubmission, Long> {
    Optional<CaSubmission> findByItemIdAndStudentId(Long itemId, Long studentId);
    List<CaSubmission> findByItemIdOrderBySubmittedAtAsc(Long itemId);
    void deleteByItemId(Long itemId);
}