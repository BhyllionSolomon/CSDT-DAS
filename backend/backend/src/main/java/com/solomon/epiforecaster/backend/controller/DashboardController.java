package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /*
     * ==========================================
     * ONE ENDPOINT FOR THE ENTIRE DASHBOARD
     * ==========================================
     */

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard() {

        Map<String, Object> dashboard =
                new LinkedHashMap<>();

        dashboard.put(
                "overview",
                dashboardService.getOverview()
        );

        dashboard.put(
                "statistics",
                dashboardService.getDashboardStatistics()
        );

        dashboard.put(
                "diseaseDistribution",
                dashboardService.getDiseaseDistribution()
        );

        dashboard.put(
                "stateDistribution",
                dashboardService.getStateDistribution()
        );

        dashboard.put(
                "weeklyTrend",
                dashboardService.getWeeklyTrend()
        );

        dashboard.put(
                "predictionSummary",
                dashboardService.getPredictionSummary()
        );

        return ResponseEntity.ok(dashboard);
    }

    /*
     * ==========================================
     * KEEP THE OLD ENDPOINTS
     * (Backward compatibility)
     * ==========================================
     */

    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        return ResponseEntity.ok(
                dashboardService.getOverview()
        );
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> statistics() {
        return ResponseEntity.ok(
                dashboardService.getDashboardStatistics()
        );
    }

    @GetMapping("/disease-distribution")
    public ResponseEntity<?> diseaseDistribution() {
        return ResponseEntity.ok(
                dashboardService.getDiseaseDistribution()
        );
    }

    @GetMapping("/state-distribution")
    public ResponseEntity<?> stateDistribution() {
        return ResponseEntity.ok(
                dashboardService.getStateDistribution()
        );
    }

    @GetMapping("/weekly-trend")
    public ResponseEntity<?> weeklyTrend() {
        return ResponseEntity.ok(
                dashboardService.getWeeklyTrend()
        );
    }

    @GetMapping("/prediction-summary")
    public ResponseEntity<?> predictionSummary() {
        return ResponseEntity.ok(
                dashboardService.getPredictionSummary()
        );
    }

}