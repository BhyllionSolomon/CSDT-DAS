package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import com.solomon.epiforecaster.backend.repository.ForecastHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
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

    public Map<String,Object> summary(){

        Map<String,Object> m=new LinkedHashMap<>();

        m.put("records",diseaseRepository.count());

        m.put("predictions",forecastRepository.count());

        m.put("diseases",
                diseaseRepository.countDistinctDiseases());

        m.put("states",
                diseaseRepository.countDistinctStates());

        return m;
    }

    public Object riskMap(){

        return diseaseRepository.getStateDistribution();

    }

    public Object hotspots(){

        return diseaseRepository.getDiseaseDistribution();

    }

    public Object forecastOverview(){

        return forecastRepository.findAll();

    }

}