package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.Dataset;
import com.solomon.epiforecaster.backend.repository.DatasetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatasetService {

    private final DatasetRepository repository;

    public DatasetService(DatasetRepository repository) {
        this.repository = repository;
    }

    public List<Dataset> getAll() {
        return repository.findAll();
    }

    public Dataset getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Dataset create(Dataset dataset) {
        return repository.save(dataset);
    }

    public Dataset update(Long id, Dataset dataset) {

        Dataset existing = repository.findById(id).orElse(null);

        if (existing == null) {
            return null;
        }

        existing.setName(dataset.getName());
        existing.setDescription(dataset.getDescription());
        existing.setDiseaseType(dataset.getDiseaseType());
        existing.setSource(dataset.getSource());
        existing.setStatus(dataset.getStatus());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}