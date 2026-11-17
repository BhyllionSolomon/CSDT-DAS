package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectStageRepository extends JpaRepository<ProjectStage, Long> {

    List<ProjectStage> findBySupervisionId(Long supervisionId);
}