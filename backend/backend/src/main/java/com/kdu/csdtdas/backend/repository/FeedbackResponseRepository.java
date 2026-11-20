package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.FeedbackResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FeedbackResponseRepository extends JpaRepository<FeedbackResponse, UUID> {

    @Query("""
        SELECT r FROM FeedbackResponse r
        JOIN FETCH r.course
        JOIN FETCH r.lecturer
        WHERE r.academicSession.id = :sessionId AND r.semester = :semester
    """)
    List<FeedbackResponse> findForSemester(@Param("sessionId") Long sessionId, @Param("semester") String semester);
}