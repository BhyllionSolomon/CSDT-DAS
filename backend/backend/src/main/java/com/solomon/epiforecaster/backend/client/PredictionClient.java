package com.solomon.epiforecaster.backend.client;

import com.solomon.epiforecaster.backend.dto.PredictionRequest;
import com.solomon.epiforecaster.backend.dto.PredictionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PredictionClient {

    private final RestTemplate restTemplate;

    @Value("${prediction.service.url}")
    private String predictionServiceUrl;

    public PredictionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PredictionResponse predict(PredictionRequest request) {

        ResponseEntity<PredictionResponse> response =
                restTemplate.postForEntity(
                        predictionServiceUrl + "/predict",
                        request,
                        PredictionResponse.class
                );

        return response.getBody();
    }

}