package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.SelfReportedResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SelfReportedResultRepository extends JpaRepository<SelfReportedResult, Long> {
    List<SelfReportedResult> findByStudentIdOrderByUploadedAtDesc(Long studentId);
}