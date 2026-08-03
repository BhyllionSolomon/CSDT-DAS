package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.RawDataset;
import com.solomon.epiforecaster.backend.repository.RawDatasetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RawDatasetService {

    private final RawDatasetRepository repository;

    public RawDatasetService(RawDatasetRepository repository) {
        this.repository = repository;
    }

    public List<RawDataset> getAllDatasets() {

        return repository.findAllByOrderByUploadedAtDesc();

    }

    public RawDataset getDataset(Long id) {

        return repository.findById(id).orElse(null);

    }

}