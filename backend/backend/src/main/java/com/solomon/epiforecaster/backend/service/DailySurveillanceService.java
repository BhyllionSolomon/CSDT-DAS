package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.dto.DailySurveillanceStatistics;
import com.solomon.epiforecaster.backend.entity.DailySurveillance;
import com.solomon.epiforecaster.backend.repository.DailySurveillanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DailySurveillanceService {

    private final DailySurveillanceRepository repository;

    public DailySurveillanceService(DailySurveillanceRepository repository) {
        this.repository = repository;
    }

    public List<DailySurveillance> getAll() {
        return repository.findAll();
    }

    public DailySurveillance getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public DailySurveillance create(DailySurveillance surveillance) {
        return repository.save(surveillance);
    }

    public DailySurveillance update(Long id,
                                    DailySurveillance surveillance) {

        DailySurveillance existing =
                repository.findById(id).orElse(null);

        if (existing == null) {
            return null;
        }

        surveillance.setId(id);

        return repository.save(surveillance);
    }

    public void delete(Long id) {

        repository.deleteById(id);

    }

    public DailySurveillanceStatistics getStatistics() {

        List<DailySurveillance> reports = repository.findAll();

        long facilities =
                reports.stream()
                        .map(DailySurveillance::getFacility)
                        .distinct()
                        .count();

        long suspected =
                reports.stream()
                        .mapToLong(DailySurveillance::getSuspectedCases)
                        .sum();

        long confirmed =
                reports.stream()
                        .mapToLong(DailySurveillance::getConfirmedCases)
                        .sum();

        long deaths =
                reports.stream()
                        .mapToLong(DailySurveillance::getDeaths)
                        .sum();

        return new DailySurveillanceStatistics(
                reports.size(),
                facilities,
                suspected,
                confirmed,
                deaths
        );

    }

}