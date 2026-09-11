package com.kdu.csdtdas.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", "UP");
        response.put("application", "Epidemiological Forecaster");
        response.put("version", "1.0.0");
        response.put("database", "Connected");
        response.put("serverTime", LocalDateTime.now());

        return response;
    }
}
