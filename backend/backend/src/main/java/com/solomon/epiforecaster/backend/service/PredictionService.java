package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.client.PredictionClient;
import com.solomon.epiforecaster.backend.dto.PredictionRequest;
import com.solomon.epiforecaster.backend.dto.PredictionResponse;
import com.solomon.epiforecaster.backend.entity.ForecastHistory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PredictionService {

    private final PredictionClient predictionClient;
    private final ForecastHistoryService forecastHistoryService;

    public PredictionService(
            PredictionClient predictionClient,
            ForecastHistoryService forecastHistoryService) {

        this.predictionClient = predictionClient;
        this.forecastHistoryService = forecastHistoryService;
    }

    public PredictionResponse predict(PredictionRequest request) {

        // Call FastAPI
        PredictionResponse response =
                predictionClient.predict(request);

        // Save prediction into forecast history
        ForecastHistory history = new ForecastHistory();

        history.setDisease(request.getDisease());
        history.setCountry(request.getCountry());
        history.setState(request.getState());
        history.setLga(request.getLga());

        history.setForecastHorizon(
                response.getForecastHorizon());

        history.setPredictedCases(
                response.getPredictedCases());

        history.setConfidenceScore(
                response.getConfidenceScore());

        history.setRiskLevel(
                response.getRiskLevel());

        history.setRecommendation(
                response.getRecommendation());

        history.setModelVersion(
                response.getModelVersion());

        history.setPredictionDate(
                LocalDateTime.now());

        forecastHistoryService.create(history);

        return response;
    }

}