package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.entity.RawDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DiseaseRecordRepository
        extends JpaRepository<DiseaseRecord, Long> {

    //----------------------------------------------------
    // Duplicate Detection
    //----------------------------------------------------

    boolean existsByDiseaseNameAndCountryAndStateAndLgaAndYearAndEpiWeek(
            String diseaseName,
            String country,
            String state,
            String lga,
            Integer year,
            Integer epiWeek
    );

    //----------------------------------------------------
    // Dataset Records
    //----------------------------------------------------

    List<DiseaseRecord> findByDataset(RawDataset dataset);

    //----------------------------------------------------
    // Dashboard Statistics
    //----------------------------------------------------

    @Query("SELECT COUNT(DISTINCT d.diseaseName) FROM DiseaseRecord d")
    long countDistinctDiseases();

    @Query("SELECT COUNT(DISTINCT d.state) FROM DiseaseRecord d")
    long countDistinctStates();

    @Query("SELECT COUNT(DISTINCT d.lga) FROM DiseaseRecord d")
    long countDistinctLgas();

}