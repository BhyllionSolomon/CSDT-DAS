package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.dto.PredictionRequest;
import com.solomon.epiforecaster.backend.dto.PredictionResponse;
import com.solomon.epiforecaster.backend.service.PredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predictions")
@CrossOrigin(origins = "*")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping
    public ResponseEntity<PredictionResponse> predict(
            @RequestBody PredictionRequest request) {

        PredictionResponse response =
                predictionService.predict(request);

        return ResponseEntity.ok(response);
    }

}