package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectPresentation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectPresentationRepository extends JpaRepository<ProjectPresentation, Long> {

    List<ProjectPresentation> findBySupervisionIdOrderByRoundNoAsc(Long supervisionId);
}