package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins="*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(
            AnalyticsService analyticsService) {

        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary() {

        return ResponseEntity.ok(
                analyticsService.summary());

    }

    @GetMapping("/risk-map")
    public ResponseEntity<?> riskMap() {

        return ResponseEntity.ok(
                analyticsService.riskMap());

    }

    @GetMapping("/hotspots")
    public ResponseEntity<?> hotspots() {

        return ResponseEntity.ok(
                analyticsService.hotspots());

    }

    @GetMapping("/forecast")
    public ResponseEntity<?> forecastOverview() {

        return ResponseEntity.ok(
                analyticsService.forecastOverview());

    }

}