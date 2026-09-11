package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.entity.RolePermission;
import com.kdu.csdtdas.backend.service.RolePermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
@CrossOrigin(origins = "*")
public class RolePermissionController {

    private final RolePermissionService service;

    public RolePermissionController(RolePermissionService service) {
        this.service = service;
    }

    @GetMapping
    public List<RolePermission> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolePermission> getById(@PathVariable Long id) {

        RolePermission rp = service.getById(id);

        if (rp == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(rp);
    }

    @PostMapping
    public ResponseEntity<RolePermission> create(@RequestBody RolePermission rp) {

        RolePermission created = service.create(rp);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RolePermission> update(
            @PathVariable Long id,
            @RequestBody RolePermission rp) {

        RolePermission updated = service.update(id, rp);

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
