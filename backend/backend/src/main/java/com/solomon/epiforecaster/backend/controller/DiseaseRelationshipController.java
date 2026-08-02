package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.DiseaseRelationship;
import com.solomon.epiforecaster.backend.service.DiseaseRelationshipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disease-relationships")
@CrossOrigin(origins = "*")
public class DiseaseRelationshipController {

    private final DiseaseRelationshipService service;

    public DiseaseRelationshipController(DiseaseRelationshipService service) {
        this.service = service;
    }

    @GetMapping
    public List<DiseaseRelationship> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiseaseRelationship> getById(@PathVariable Long id) {

        DiseaseRelationship relationship = service.getById(id);

        if (relationship == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(relationship);
    }

    @PostMapping
    public ResponseEntity<DiseaseRelationship> create(@RequestBody DiseaseRelationship relationship) {

        DiseaseRelationship created = service.create(relationship);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiseaseRelationship> update(
            @PathVariable Long id,
            @RequestBody DiseaseRelationship relationship) {

        DiseaseRelationship updated = service.update(id, relationship);

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