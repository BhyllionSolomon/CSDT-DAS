package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.dto.ForecastRequest;
import com.solomon.epiforecaster.backend.dto.ForecastResponse;
import com.solomon.epiforecaster.backend.dto.PredictionResult;
import org.springframework.stereotype.Service;

@Service
public class ForecastService {

    private final PredictionClient predictionClient;

    public ForecastService(
            PredictionClient predictionClient) {

        this.predictionClient = predictionClient;
    }

    public ForecastResponse forecast(
            ForecastRequest request) {

        PredictionResult result =
                predictionClient.predict(request);

        ForecastResponse response =
                new ForecastResponse();

        response.setDiseaseName(
                request.getDiseaseName());

        response.setLocation(

                request.getCountry()
                        + ", "
                        + request.getState()
                        + ", "
                        + request.getLga()

        );

        response.setPredictions(
                result.getPredictions());

        return response;
    }

}