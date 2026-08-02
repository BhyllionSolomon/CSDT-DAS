package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.Dataset;
import com.solomon.epiforecaster.backend.service.DatasetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasets")
@CrossOrigin(origins = "*")
public class DatasetController {

    private final DatasetService service;

    public DatasetController(DatasetService service) {
        this.service = service;
    }

    @GetMapping
    public List<Dataset> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dataset> getById(@PathVariable Long id) {

        Dataset dataset = service.getById(id);

        if (dataset == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(dataset);
    }

    @PostMapping
    public ResponseEntity<Dataset> create(@RequestBody Dataset dataset) {

        Dataset created = service.create(dataset);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dataset> update(
            @PathVariable Long id,
            @RequestBody Dataset dataset) {

        Dataset updated = service.update(id, dataset);

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