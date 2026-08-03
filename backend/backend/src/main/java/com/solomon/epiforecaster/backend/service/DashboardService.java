package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final DiseaseRecordRepository repository;

    public DashboardService(DiseaseRecordRepository repository) {
        this.repository = repository;
    }

    public Map<String, Long> getDashboardStatistics() {

        Map<String, Long> stats = new HashMap<>();

        stats.put("totalRecords", repository.count());

        stats.put("totalDiseases",
                repository.countDistinctDiseases());

        stats.put("totalStates",
                repository.countDistinctStates());

        stats.put("totalLgas",
                repository.countDistinctLgas());

        return stats;
    }

}