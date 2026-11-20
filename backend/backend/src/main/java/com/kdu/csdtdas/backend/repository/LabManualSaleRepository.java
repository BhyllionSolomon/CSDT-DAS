package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.LabManualSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LabManualSaleRepository extends JpaRepository<LabManualSale, Long> {

    @Query("""
        SELECT s FROM LabManualSale s
        JOIN FETCH s.manual m
        LEFT JOIN FETCH m.course
        JOIN FETCH s.student st
        JOIN FETCH s.academicSession a
        ORDER BY s.soldAt DESC
    """)
    List<LabManualSale> findAllWithDetails();

    boolean existsByManualIdAndStudentIdAndAcademicSessionIdAndVoidedFalse(Long manualId, Long studentId, Long sessionId);
}