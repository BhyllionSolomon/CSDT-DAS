package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.dto.DailySurveillanceStatistics;
import com.solomon.epiforecaster.backend.entity.DailySurveillance;
import com.solomon.epiforecaster.backend.repository.DailySurveillanceRepository;
import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DailySurveillanceService {

    private final DailySurveillanceRepository repository;
    private final DiseaseRecordRepository diseaseRecordRepository;

    public DailySurveillanceService(
            DailySurveillanceRepository repository,
            DiseaseRecordRepository diseaseRecordRepository) {

        this.repository = repository;
        this.diseaseRecordRepository = diseaseRecordRepository;
    }

    /*
     * Populate Disease dropdown
     */
    public List<String> getDiseases() {
        return diseaseRecordRepository.findDistinctDiseases();
    }

    /*
     * Get all surveillance records
     */
    public List<DailySurveillance> getAll() {
        return repository.findAll();
    }

    /*
     * Get one record
     */
    public DailySurveillance getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    /*
     * Create record
     */
    public DailySurveillance create(DailySurveillance surveillance) {
        return repository.save(surveillance);
    }

    /*
     * Update record
     */
    public DailySurveillance update(
            Long id,
            DailySurveillance surveillance) {

        DailySurveillance existing =
                repository.findById(id).orElse(null);

        if (existing == null) {
            return null;
        }

        surveillance.setId(id);

        return repository.save(surveillance);
    }

    /*
     * Delete record
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /*
     * Dashboard statistics
     */
    public DailySurveillanceStatistics getStatistics() {

        DailySurveillanceStatistics stats =
                new DailySurveillanceStatistics();

        List<DailySurveillance> records =
                repository.findAll();

        stats.setTotalReports(records.size());

        stats.setTotalSuspectedCases(
                records.stream()
                        .mapToInt(r -> r.getSuspectedCases() == null ? 0 : r.getSuspectedCases())
                        .sum());

        stats.setTotalConfirmedCases(
                records.stream()
                        .mapToInt(r -> r.getConfirmedCases() == null ? 0 : r.getConfirmedCases())
                        .sum());

        stats.setTotalDeaths(
                records.stream()
                        .mapToInt(r -> r.getDeaths() == null ? 0 : r.getDeaths())
                        .sum());

        stats.setTotalRecovered(
                records.stream()
                        .mapToInt(r -> r.getRecovered() == null ? 0 : r.getRecovered())
                        .sum());

        return stats;
    }

}