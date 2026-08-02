package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.AuditLog;
import com.solomon.epiforecaster.backend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin(origins = "*")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditLog> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getById(@PathVariable Long id) {

        AuditLog auditLog = service.getById(id);

        if (auditLog == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(auditLog);
    }

    @PostMapping
    public ResponseEntity<AuditLog> create(@RequestBody AuditLog auditLog) {

        AuditLog created = service.create(auditLog);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuditLog> update(
            @PathVariable Long id,
            @RequestBody AuditLog auditLog) {

        AuditLog updated = service.update(id, auditLog);

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