package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.entity.RawDataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiseaseRecordRepository
        extends JpaRepository<DiseaseRecord, Long> {

    boolean existsByDiseaseNameAndCountryAndStateAndLgaAndYearAndEpiWeek(
            String diseaseName,
            String country,
            String state,
            String lga,
            Integer year,
            Integer epiWeek
    );

    List<DiseaseRecord> findByDataset(RawDataset dataset);

}