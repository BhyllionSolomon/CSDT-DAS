package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import com.solomon.epiforecaster.backend.repository.ForecastHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final DiseaseRecordRepository diseaseRepository;
    private final ForecastHistoryRepository forecastRepository;

    public AnalyticsService(
            DiseaseRecordRepository diseaseRepository,
            ForecastHistoryRepository forecastRepository) {

        this.diseaseRepository = diseaseRepository;
        this.forecastRepository = forecastRepository;
    }

    /*
     * Dashboard summary cards
     */
    public Map<String, Object> summary() {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("records", diseaseRepository.count());
        map.put("predictions", forecastRepository.count());
        map.put("diseases", diseaseRepository.countDistinctDiseases());
        map.put("states", diseaseRepository.countDistinctStates());

        return map;
    }

    /*
     * Weekly trend chart
     */
    public List<Object[]> weeklyTrend() {

        return diseaseRepository.getWeeklyTrend();

    }

    /*
     * Choropleth map
     */
    public List<Object[]> riskMap() {

        return diseaseRepository.getStateDistribution();

    }

    /*
     * Disease distribution
     */
    public List<Object[]> hotspots() {

        return diseaseRepository.getDiseaseDistribution();

    }

    /*
     * Forecast summary ONLY
     * Do NOT return entities.
     */
    public Map<String, Object> forecastOverview() {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("totalForecasts", forecastRepository.count());

        return map;

    }

}