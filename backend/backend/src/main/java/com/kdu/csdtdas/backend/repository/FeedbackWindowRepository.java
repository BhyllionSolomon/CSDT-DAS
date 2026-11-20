package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.FeedbackWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackWindowRepository extends JpaRepository<FeedbackWindow, Long> {

    Optional<FeedbackWindow> findByAcademicSessionIdAndSemester(Long sessionId, String semester);
}