package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.ForecastRun;
import com.solomon.epiforecaster.backend.service.ForecastRunService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecast-runs")
@CrossOrigin(origins = "*")
public class ForecastRunController {

    private final ForecastRunService service;

    public ForecastRunController(ForecastRunService service) {
        this.service = service;
    }

    @GetMapping
    public List<ForecastRun> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForecastRun> getById(@PathVariable Long id) {

        ForecastRun run = service.getById(id);

        if (run == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(run);
    }

    @PostMapping
    public ResponseEntity<ForecastRun> create(@RequestBody ForecastRun run) {

        ForecastRun created = service.create(run);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ForecastRun> update(
            @PathVariable Long id,
            @RequestBody ForecastRun run) {

        ForecastRun updated = service.update(id, run);

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