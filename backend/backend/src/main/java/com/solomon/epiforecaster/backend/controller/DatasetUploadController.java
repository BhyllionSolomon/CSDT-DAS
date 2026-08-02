package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.DatasetUpload;
import com.solomon.epiforecaster.backend.service.DatasetUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dataset-uploads")
@CrossOrigin(origins = "*")
public class DatasetUploadController {

    private final DatasetUploadService service;

    public DatasetUploadController(DatasetUploadService service) {
        this.service = service;
    }

    @GetMapping
    public List<DatasetUpload> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatasetUpload> getById(@PathVariable Long id) {

        DatasetUpload upload = service.getById(id);

        if (upload == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(upload);
    }

    @PostMapping
    public ResponseEntity<DatasetUpload> create(@RequestBody DatasetUpload upload) {

        DatasetUpload created = service.create(upload);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DatasetUpload> update(
            @PathVariable Long id,
            @RequestBody DatasetUpload upload) {

        DatasetUpload updated = service.update(id, upload);

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