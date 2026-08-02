package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.DiseaseType;
import com.solomon.epiforecaster.backend.repository.DiseaseTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiseaseTypeService {

    private final DiseaseTypeRepository repository;

    public DiseaseTypeService(DiseaseTypeRepository repository) {
        this.repository = repository;
    }

    public List<DiseaseType> getAllDiseaseTypes() {
        return repository.findAll();
    }

    public DiseaseType getDiseaseType(Long id) {
        return repository.findById(id).orElse(null);
    }

    public DiseaseType saveDiseaseType(DiseaseType diseaseType) {
        return repository.save(diseaseType);
    }

    public void deleteDiseaseType(Long id) {
        repository.deleteById(id);
    }
}