package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class DuplicateDetectionService {

    private final DiseaseRecordRepository diseaseRecordRepository;

    public DuplicateDetectionService(
            DiseaseRecordRepository diseaseRecordRepository) {

        this.diseaseRecordRepository = diseaseRecordRepository;
    }

    public boolean isDuplicate(
            String disease,
            String country,
            String state,
            String lga,
            Integer year,
            Integer epiWeek) {

        return diseaseRecordRepository.existsByDiseaseNameAndCountryAndStateAndLgaAndYearAndEpiWeek(
                disease,
                country,
                state,
                lga,
                year,
                epiWeek
        );

    }

}