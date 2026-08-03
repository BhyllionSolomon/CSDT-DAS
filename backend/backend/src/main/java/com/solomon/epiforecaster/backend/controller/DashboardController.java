package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/statistics")
    public Map<String, Long> statistics() {
        return dashboardService.getDashboardStatistics();
    }

}