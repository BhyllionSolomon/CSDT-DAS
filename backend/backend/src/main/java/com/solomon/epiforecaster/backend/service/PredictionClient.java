package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.dto.ForecastRequest;
import com.solomon.epiforecaster.backend.dto.PredictionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PredictionClient {

    private final RestTemplate restTemplate;

    private static final String PYTHON_API =
            "http://localhost:8000/predict";

    @Autowired
    public PredictionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PredictionResult predict(ForecastRequest request) {

        ResponseEntity<PredictionResult> response =
                restTemplate.postForEntity(
                        PYTHON_API,
                        request,
                        PredictionResult.class
                );

        return response.getBody();
    }

}