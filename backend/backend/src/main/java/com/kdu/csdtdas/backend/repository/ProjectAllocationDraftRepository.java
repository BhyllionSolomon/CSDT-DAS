package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectAllocationDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProjectAllocationDraftRepository extends JpaRepository<ProjectAllocationDraft, Long> {
    Optional<ProjectAllocationDraft> findFirstByAcademicSessionIdAndStatusOrderByCreatedAtDesc(Long sessionId, String status);
    Optional<ProjectAllocationDraft> findFirstByAcademicSessionIdOrderByCreatedAtDesc(Long sessionId);
}
