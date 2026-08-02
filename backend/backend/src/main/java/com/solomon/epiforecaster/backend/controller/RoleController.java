package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.Role;
import com.solomon.epiforecaster.backend.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping
    public List<Role> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getById(@PathVariable Long id) {

        Role role = service.getById(id);

        if (role == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(role);
    }

    @PostMapping
    public ResponseEntity<Role> create(@RequestBody Role role) {

        Role created = service.create(role);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Role> update(@PathVariable Long id,
                                       @RequestBody Role role) {

        Role updated = service.update(id, role);

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