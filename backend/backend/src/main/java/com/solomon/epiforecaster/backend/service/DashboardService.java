package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.ForecastHistory;
import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import com.solomon.epiforecaster.backend.repository.ForecastHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {

    private final DiseaseRecordRepository diseaseRepository;
    private final ForecastHistoryRepository forecastRepository;

    public DashboardService(
            DiseaseRecordRepository diseaseRepository,
            ForecastHistoryRepository forecastRepository) {

        this.diseaseRepository = diseaseRepository;
        this.forecastRepository = forecastRepository;
    }

    /*
     * KPI Cards
     */

    public Map<String, Object> getOverview() {

        Map<String, Object> overview = new LinkedHashMap<>();

        overview.put("totalRecords",
                diseaseRepository.count());

        overview.put("totalDiseases",
                diseaseRepository.countDistinctDiseases());

        overview.put("totalStates",
                diseaseRepository.countDistinctStates());

        overview.put("totalLgas",
                diseaseRepository.countDistinctLgas());

        overview.put("totalPredictions",
                forecastRepository.count());

        return overview;
    }

    /*
     * Existing Statistics API
     */

    public Map<String, Long> getDashboardStatistics() {

        Map<String, Long> stats = new LinkedHashMap<>();

        stats.put("totalRecords",
                diseaseRepository.count());

        stats.put("totalDiseases",
                diseaseRepository.countDistinctDiseases());

        stats.put("totalStates",
                diseaseRepository.countDistinctStates());

        stats.put("totalLgas",
                diseaseRepository.countDistinctLgas());

        return stats;
    }

    /*
     * Disease Distribution
     */

    public Map<String, Long> getDiseaseDistribution() {

        Map<String, Long> distribution =
                new LinkedHashMap<>();

        List<Object[]> rows =
                diseaseRepository.getDiseaseDistribution();

        for (Object[] row : rows) {

            distribution.put(

                    (String) row[0],

                    ((Number) row[1]).longValue()

            );

        }

        return distribution;
    }

    /*
     * State Distribution
     */

    public Map<String, Long> getStateDistribution() {

        Map<String, Long> distribution =
                new LinkedHashMap<>();

        List<Object[]> rows =
                diseaseRepository.getStateDistribution();

        for (Object[] row : rows) {

            distribution.put(

                    (String) row[0],

                    ((Number) row[1]).longValue()

            );

        }

        return distribution;
    }

    /*
     * Weekly Trend
     */

    public List<Map<String, Object>> getWeeklyTrend() {

        List<Map<String, Object>> trend =
                new ArrayList<>();

        List<Object[]> rows =
                diseaseRepository.getWeeklyTrend();

        for (Object[] row : rows) {

            Map<String, Object> point =
                    new LinkedHashMap<>();

            point.put("year",
                    ((Number) row[0]).intValue());

            point.put("week",
                    ((Number) row[1]).intValue());

            point.put("confirmedCases",
                    ((Number) row[2]).longValue());

            trend.add(point);

        }

        return trend;
    }

    /*
     * AI Prediction History
     */

    public List<ForecastHistory> getPredictionSummary() {

        return forecastRepository.findAll();

    }

}