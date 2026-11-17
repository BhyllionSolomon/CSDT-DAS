package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.PresentationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PresentationFeedbackRepository extends JpaRepository<PresentationFeedback, Long> {

    List<PresentationFeedback> findByPresentationIdOrderByIdAsc(Long presentationId);

    Optional<PresentationFeedback> findByPresentationIdAndAssessorId(Long presentationId, Long assessorId);
}