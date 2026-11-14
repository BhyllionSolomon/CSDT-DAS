package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Read-only queries over results, for statistics and the Today panel. */
public interface ResultStatsRepository extends JpaRepository<Result, Long> {

    @Query("""
        SELECT r FROM Result r
        JOIN FETCH r.course c
        JOIN FETCH r.academicSession s
        JOIN FETCH r.student st
        WHERE r.status = 'APPROVED' AND s.id = :sessionId
    """)
    List<Result> findApprovedForSession(@Param("sessionId") Long sessionId);

    @Query("SELECT COUNT(r) FROM Result r WHERE r.status = 'PENDING'")
    long countPending();
}