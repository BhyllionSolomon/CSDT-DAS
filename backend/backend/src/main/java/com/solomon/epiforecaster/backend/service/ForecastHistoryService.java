package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.ForecastHistory;
import com.solomon.epiforecaster.backend.repository.ForecastHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForecastHistoryService {

    private final ForecastHistoryRepository repository;

    public ForecastHistoryService(ForecastHistoryRepository repository) {
        this.repository = repository;
    }

    public List<ForecastHistory> getAll() {
        return repository.findAll();
    }

    public ForecastHistory getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public ForecastHistory create(ForecastHistory history) {
        return repository.save(history);
    }

    public ForecastHistory update(Long id, ForecastHistory history) {

        ForecastHistory existing =
                repository.findById(id).orElse(null);

        if (existing == null) {
            return null;
        }

        history.setId(id);

        return repository.save(history);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<ForecastHistory> getByDisease(String disease) {
        return repository.findByDisease(disease);
    }

    public List<ForecastHistory> getByState(String state) {
        return repository.findByState(state);
    }

    public List<ForecastHistory> getByLga(String lga) {
        return repository.findByLga(lga);
    }

}