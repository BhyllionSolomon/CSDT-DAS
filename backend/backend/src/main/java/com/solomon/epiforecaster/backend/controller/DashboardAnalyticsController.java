package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.ForecastHistory;
import com.solomon.epiforecaster.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardAnalyticsController {

    private final DashboardService dashboardService;

    public DashboardAnalyticsController(
            DashboardService dashboardService) {

        this.dashboardService = dashboardService;
    }

    /*
     * Existing dashboard statistics
     */

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> statistics() {

        return ResponseEntity.ok(
                dashboardService.getDashboardStatistics()
        );

    }

    /*
     * Dashboard Overview
     */

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {

        return ResponseEntity.ok(
                dashboardService.getOverview()
        );

    }

    /*
     * Disease Distribution
     */

    @GetMapping("/disease-distribution")
    public ResponseEntity<Map<String, Long>> diseaseDistribution() {

        return ResponseEntity.ok(
                dashboardService.getDiseaseDistribution()
        );

    }

    /*
     * State Distribution
     */

    @GetMapping("/state-distribution")
    public ResponseEntity<Map<String, Long>> stateDistribution() {

        return ResponseEntity.ok(
                dashboardService.getStateDistribution()
        );

    }

    /*
     * Weekly Trend
     */

    @GetMapping("/weekly-trend")
    public ResponseEntity<List<Map<String, Object>>> weeklyTrend() {

        return ResponseEntity.ok(
                dashboardService.getWeeklyTrend()
        );

    }

    /*
     * AI Prediction History
     */

    @GetMapping("/prediction-summary")
    public ResponseEntity<List<ForecastHistory>> predictionSummary() {

        return ResponseEntity.ok(
                dashboardService.getPredictionSummary()
        );

    }

}