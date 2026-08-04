package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.ForecastHistory;
import com.solomon.epiforecaster.backend.service.ForecastHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecast-history")
@CrossOrigin(origins = "*")
public class ForecastHistoryController {

    private final ForecastHistoryService service;

    public ForecastHistoryController(ForecastHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<ForecastHistory> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForecastHistory> getById(
            @PathVariable Long id) {

        ForecastHistory history = service.getById(id);

        if (history == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(history);
    }

    @PostMapping
    public ResponseEntity<ForecastHistory> create(
            @RequestBody ForecastHistory history) {

        ForecastHistory created = service.create(history);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ForecastHistory> update(
            @PathVariable Long id,
            @RequestBody ForecastHistory history) {

        ForecastHistory updated = service.update(id, history);

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


    @GetMapping("/disease/{disease}")
    public List<ForecastHistory> byDisease(
            @PathVariable String disease) {

        return service.getByDisease(disease);
    }

    @GetMapping("/state/{state}")
    public List<ForecastHistory> byState(
            @PathVariable String state) {

        return service.getByState(state);
    }

    @GetMapping("/lga/{lga}")
    public List<ForecastHistory> byLga(
            @PathVariable String lga) {

        return service.getByLga(lga);
    }

}