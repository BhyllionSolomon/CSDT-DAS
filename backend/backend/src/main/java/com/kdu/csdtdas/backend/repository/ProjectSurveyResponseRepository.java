package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectSurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectSurveyResponseRepository extends JpaRepository<ProjectSurveyResponse, UUID> {

    @Query("SELECT r FROM ProjectSurveyResponse r JOIN FETCH r.supervisor WHERE r.academicSession.id = :sessionId")
    List<ProjectSurveyResponse> findForSession(@Param("sessionId") Long sessionId);
}