package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.ForecastResult;
import com.solomon.epiforecaster.backend.service.ForecastResultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecast-results")
@CrossOrigin(origins = "*")
public class ForecastResultController {

    private final ForecastResultService service;

    public ForecastResultController(ForecastResultService service) {
        this.service = service;
    }

    @GetMapping
    public List<ForecastResult> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForecastResult> getById(@PathVariable Long id) {

        ForecastResult result = service.getById(id);

        if (result == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<ForecastResult> create(@RequestBody ForecastResult result) {

        ForecastResult created = service.create(result);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ForecastResult> update(
            @PathVariable Long id,
            @RequestBody ForecastResult result) {

        ForecastResult updated = service.update(id, result);

        if (updated == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}