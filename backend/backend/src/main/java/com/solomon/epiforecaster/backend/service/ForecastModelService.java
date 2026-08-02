package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.ForecastModel;
import com.solomon.epiforecaster.backend.repository.ForecastModelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForecastModelService {

    private final ForecastModelRepository repository;

    public ForecastModelService(ForecastModelRepository repository) {
        this.repository = repository;
    }

    public List<ForecastModel> getAll() {
        return repository.findAll();
    }

    public ForecastModel getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public ForecastModel create(ForecastModel model) {
        return repository.save(model);
    }

    public ForecastModel update(Long id, ForecastModel model) {

        ForecastModel existing = repository.findById(id).orElse(null);

        if (existing == null) {
            return null;
        }

        existing.setModelName(model.getModelName());
        existing.setVersion(model.getVersion());
        existing.setArchitecture(model.getArchitecture());
        existing.setDescription(model.getDescription());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}