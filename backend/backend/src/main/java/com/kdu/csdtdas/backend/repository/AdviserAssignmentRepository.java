package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.AdviserAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdviserAssignmentRepository extends JpaRepository<AdviserAssignment, Long> {

    @Query("""
        SELECT a FROM AdviserAssignment a
        JOIN FETCH a.user
        JOIN FETCH a.programme
        LEFT JOIN FETCH a.level
        ORDER BY a.startedAt DESC
    """)
    List<AdviserAssignment> findAllWithDetails();

    @Query("SELECT a FROM AdviserAssignment a WHERE a.user.id = :userId AND a.endedAt IS NULL")
    List<AdviserAssignment> findActiveByUser(@Param("userId") Long userId);
}