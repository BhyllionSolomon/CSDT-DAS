package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.repository.DiseaseMasterRepository;
import org.springframework.stereotype.Service;

@Service
public class DiseaseValidationService {

    private final DiseaseMasterRepository diseaseMasterRepository;

    public DiseaseValidationService(
            DiseaseMasterRepository diseaseMasterRepository) {

        this.diseaseMasterRepository = diseaseMasterRepository;
    }

    public boolean isValidDisease(String diseaseName) {

        if (diseaseName == null || diseaseName.isBlank()) {
            return false;
        }

        return diseaseMasterRepository.existsByNameIgnoreCase(
                diseaseName.trim()
        );

    }

}