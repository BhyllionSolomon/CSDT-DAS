package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectSurveyReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSurveyReceiptRepository extends JpaRepository<ProjectSurveyReceipt, Long> {

    boolean existsByStudentIdAndAcademicSessionId(Long studentId, Long sessionId);
}