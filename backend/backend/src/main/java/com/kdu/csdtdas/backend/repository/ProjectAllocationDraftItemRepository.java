package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectAllocationDraftItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectAllocationDraftItemRepository extends JpaRepository<ProjectAllocationDraftItem, Long> {
    List<ProjectAllocationDraftItem> findByDraftIdOrderByStudentFullNameAsc(Long draftId);
    List<ProjectAllocationDraftItem> findByDraftId(Long draftId);
    void deleteByDraftId(Long draftId);
}
