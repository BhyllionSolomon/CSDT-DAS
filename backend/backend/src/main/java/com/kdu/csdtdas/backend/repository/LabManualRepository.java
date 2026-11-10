package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.LabManual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LabManualRepository extends JpaRepository<LabManual, Long> {

    @Query("SELECT m FROM LabManual m LEFT JOIN FETCH m.course ORDER BY m.title")
    List<LabManual> findAllWithCourse();
}