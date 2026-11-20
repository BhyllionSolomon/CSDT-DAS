package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectAssessmentRepository extends JpaRepository<ProjectAssessment, Long> {

    Optional<ProjectAssessment> findBySupervisionId(Long supervisionId);
}