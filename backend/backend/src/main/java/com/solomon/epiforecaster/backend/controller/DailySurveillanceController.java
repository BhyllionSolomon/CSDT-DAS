package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.dto.DailySurveillanceStatistics;
import com.solomon.epiforecaster.backend.entity.DailySurveillance;
import com.solomon.epiforecaster.backend.service.DailySurveillanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/daily-surveillance")
@CrossOrigin(origins = "*")
public class DailySurveillanceController {

    private final DailySurveillanceService service;

    public DailySurveillanceController(DailySurveillanceService service) {
        this.service = service;
    }

    @GetMapping
    public List<DailySurveillance> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DailySurveillance> getById(
            @PathVariable Long id) {

        DailySurveillance surveillance = service.getById(id);

        if (surveillance == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(surveillance);
    }

    @PostMapping
    public ResponseEntity<DailySurveillance> create(
            @RequestBody DailySurveillance surveillance) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(surveillance));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DailySurveillance> update(
            @PathVariable Long id,
            @RequestBody DailySurveillance surveillance) {

        DailySurveillance updated =
                service.update(id, surveillance);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    public DailySurveillanceStatistics statistics() {
        return service.getStatistics();
    }

}