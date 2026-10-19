package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.Result;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AnalyticsRepository extends Repository<Result, Long> {

    @Query("""
        SELECT r FROM Result r
        JOIN FETCH r.student s
        JOIN FETCH s.programme
        JOIN FETCH r.course c
        JOIN FETCH c.level
        JOIN FETCH r.academicSession a
        WHERE r.status = 'APPROVED'
    """)
    List<Result> findAllApproved();
}