package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.dto.ForecastRequest;
import com.solomon.epiforecaster.backend.dto.ForecastResponse;
import com.solomon.epiforecaster.backend.service.ForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forecast")
@CrossOrigin(origins = "*")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(
            ForecastService forecastService) {

        this.forecastService = forecastService;
    }

    @PostMapping
    public ResponseEntity<ForecastResponse> forecast(

            @RequestBody ForecastRequest request

    ) {

        ForecastResponse response =
                forecastService.forecast(request);

        return ResponseEntity.ok(response);
    }

}