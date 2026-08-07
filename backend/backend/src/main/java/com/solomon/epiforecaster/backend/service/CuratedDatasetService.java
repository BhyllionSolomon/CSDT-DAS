package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.CuratedDataset;
import com.solomon.epiforecaster.backend.repository.CuratedDatasetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CuratedDatasetService {

    private final CuratedDatasetRepository repository;

    public CuratedDatasetService(
            CuratedDatasetRepository repository) {

        this.repository = repository;
    }

    /*
     * Get all curated records
     */

    public List<CuratedDataset> getAll() {

        return repository.findAll();

    }

    /*
     * Get one curated record
     */

    public CuratedDataset getById(Long id) {

        return repository.findById(id).orElse(null);

    }

    /*
     * Save curated record
     */

    public CuratedDataset save(
            CuratedDataset curatedDataset) {

        return repository.save(curatedDataset);

    }

    /*
     * Delete one curated record
     */

    public void delete(Long id) {

        repository.deleteById(id);

    }

    /*
     * Delete all curated records
     */

    public void deleteAll() {

        repository.deleteAll();

    }

    /*
     * Count curated records
     */

    public long count() {

        return repository.count();

    }

}