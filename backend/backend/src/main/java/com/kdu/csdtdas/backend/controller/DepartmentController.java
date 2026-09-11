package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.entity.Department;
import com.kdu.csdtdas.backend.service.DepartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ResponseEntity<Department> createDepartment(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam(required = false) String description
    ) {

        Department department =
                departmentService.createDepartment(
                        code,
                        name,
                        description
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(department);
    }

    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments() {

        return ResponseEntity.ok(
                departmentService.getAllDepartments()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> getDepartment(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                departmentService.getDepartment(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description
    ) {

        Department department =
                departmentService.updateDepartment(
                        id,
                        name,
                        description
                );

        return ResponseEntity.ok(department);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Department> activateDepartment(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                departmentService.activateDepartment(id)
        );
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Department> deactivateDepartment(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                departmentService.deactivateDepartment(id)
        );
    }
}
