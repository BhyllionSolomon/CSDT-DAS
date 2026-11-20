package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectSurveyWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectSurveyWindowRepository extends JpaRepository<ProjectSurveyWindow, Long> {

    Optional<ProjectSurveyWindow> findByAcademicSessionId(Long sessionId);
}