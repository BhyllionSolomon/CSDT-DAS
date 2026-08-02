package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiseaseRecordService {

    private final DiseaseRecordRepository repository;

    public DiseaseRecordService(DiseaseRecordRepository repository) {
        this.repository = repository;
    }

    public List<DiseaseRecord> getAll() {
        return repository.findAll();
    }

    public DiseaseRecord getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public DiseaseRecord create(DiseaseRecord diseaseRecord) {
        return repository.save(diseaseRecord);
    }

    public DiseaseRecord update(Long id, DiseaseRecord diseaseRecord) {
        DiseaseRecord existing = repository.findById(id).orElse(null);

        if (existing == null) {
            return null;
        }

        existing.setDiseaseName(diseaseRecord.getDiseaseName());
        existing.setCountry(diseaseRecord.getCountry());
        existing.setState(diseaseRecord.getState());
        existing.setLga(diseaseRecord.getLga());
        existing.setYear(diseaseRecord.getYear());
        existing.setEpiWeek(diseaseRecord.getEpiWeek());
        existing.setSuspectedCases(diseaseRecord.getSuspectedCases());
        existing.setConfirmedCases(diseaseRecord.getConfirmedCases());
        existing.setDeaths(diseaseRecord.getDeaths());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}