package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.ForecastRun;
import com.solomon.epiforecaster.backend.repository.ForecastRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForecastRunService {

    private final ForecastRunRepository repository;

    public ForecastRunService(ForecastRunRepository repository) {
        this.repository = repository;
    }

    public List<ForecastRun> getAll() {
        return repository.findAll();
    }

    public ForecastRun getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public ForecastRun create(ForecastRun run) {
        return repository.save(run);
    }

    public ForecastRun update(Long id, ForecastRun run) {

        ForecastRun existing = repository.findById(id).orElse(null);

        if (existing == null)
            return null;

        existing.setForecastModel(run.getForecastModel());
        existing.setDataset(run.getDataset());
        existing.setRandomSeed(run.getRandomSeed());
        existing.setStatus(run.getStatus());
        existing.setStartedAt(run.getStartedAt());
        existing.setCompletedAt(run.getCompletedAt());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}