package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.ForecastModel;
import com.solomon.epiforecaster.backend.service.ForecastModelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecast-models")
@CrossOrigin(origins = "*")
public class ForecastModelController {

    private final ForecastModelService service;

    public ForecastModelController(ForecastModelService service) {
        this.service = service;
    }

    @GetMapping
    public List<ForecastModel> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForecastModel> getById(@PathVariable Long id) {

        ForecastModel model = service.getById(id);

        if (model == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(model);
    }

    @PostMapping
    public ResponseEntity<ForecastModel> create(@RequestBody ForecastModel model) {

        ForecastModel created = service.create(model);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ForecastModel> update(
            @PathVariable Long id,
            @RequestBody ForecastModel model) {

        ForecastModel updated = service.update(id, model);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}