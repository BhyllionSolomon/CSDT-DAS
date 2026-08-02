package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.Permission;
import com.solomon.epiforecaster.backend.service.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "*")
public class PermissionController {

    private final PermissionService service;

    public PermissionController(PermissionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Permission> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permission> getById(@PathVariable Long id) {

        Permission permission = service.getById(id);

        if (permission == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(permission);
    }

    @PostMapping
    public ResponseEntity<Permission> create(@RequestBody Permission permission) {

        Permission created = service.create(permission);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Permission> update(@PathVariable Long id,
                                             @RequestBody Permission permission) {

        Permission updated = service.update(id, permission);

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