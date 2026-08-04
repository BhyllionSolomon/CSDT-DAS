package com.solomon.epiforecaster.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("status", "UP");
        response.put("application", "EpiSentinel");
        response.put("backend", "Spring Boot");
        response.put("database", "Connected");
        response.put("predictionService", "Available");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> version() {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("name", "AI-Driven Epidemiological Surveillance and Disease Forecasting Platform");
        response.put("version", "1.0.0");
        response.put("model", "DCTMN");
        response.put("release", "Prototype");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

}