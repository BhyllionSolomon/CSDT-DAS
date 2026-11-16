package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectAllocationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectAllocationEventRepository extends JpaRepository<ProjectAllocationEvent, Long> {
    List<ProjectAllocationEvent> findByDraftIdOrderByChangedAtAsc(Long draftId);
}
