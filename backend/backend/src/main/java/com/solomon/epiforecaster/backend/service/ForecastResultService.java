package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.ForecastResult;
import com.solomon.epiforecaster.backend.repository.ForecastResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForecastResultService {

    private final ForecastResultRepository repository;

    public ForecastResultService(ForecastResultRepository repository) {
        this.repository = repository;
    }

    public List<ForecastResult> getAll() {
        return repository.findAll();
    }

    public ForecastResult getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public ForecastResult create(ForecastResult result) {
        return repository.save(result);
    }

    public ForecastResult update(Long id, ForecastResult result) {

        ForecastResult existing = repository.findById(id).orElse(null);

        if (existing == null)
            return null;

        existing.setForecastRun(result.getForecastRun());
        existing.setDiseaseType(result.getDiseaseType());
        existing.setLga(result.getLga());
        existing.setYear(result.getYear());
        existing.setEpiWeek(result.getEpiWeek());
        existing.setPredictedCases(result.getPredictedCases());
        existing.setActualCases(result.getActualCases());
        existing.setMae(result.getMae());
        existing.setRmse(result.getRmse());
        existing.setMape(result.getMape());
        existing.setSmape(result.getSmape());
        existing.setR2(result.getR2());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}