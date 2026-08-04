package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.DailySurveillance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailySurveillanceRepository
        extends JpaRepository<DailySurveillance, Long> {

    List<DailySurveillance> findByDisease(String disease);

    List<DailySurveillance> findByState(String state);

    List<DailySurveillance> findByLga(String lga);

    List<DailySurveillance> findByReportDate(LocalDate reportDate);

    List<DailySurveillance> findByDiseaseAndState(
            String disease,
            String state
    );

    boolean existsByDiseaseAndStateAndLgaAndFacilityAndReportDate(
            String disease,
            String state,
            String lga,
            String facility,
            LocalDate reportDate
    );

}