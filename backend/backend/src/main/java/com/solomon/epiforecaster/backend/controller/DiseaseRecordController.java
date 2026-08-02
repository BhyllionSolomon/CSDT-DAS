package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.service.DiseaseRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disease-records")
@CrossOrigin(origins = "*")
public class DiseaseRecordController {

    private final DiseaseRecordService service;

    public DiseaseRecordController(DiseaseRecordService service) {
        this.service = service;
    }

    @GetMapping
    public List<DiseaseRecord> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiseaseRecord> getById(@PathVariable Long id) {

        DiseaseRecord diseaseRecord = service.getById(id);

        if (diseaseRecord == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(diseaseRecord);
    }

    @PostMapping
    public ResponseEntity<DiseaseRecord> create(@RequestBody DiseaseRecord diseaseRecord) {

        DiseaseRecord created = service.create(diseaseRecord);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiseaseRecord> update(
            @PathVariable Long id,
            @RequestBody DiseaseRecord diseaseRecord) {

        DiseaseRecord updated = service.update(id, diseaseRecord);

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