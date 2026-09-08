package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.Department;
import com.solomon.epiforecaster.backend.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository
    ) {
        this.departmentRepository = departmentRepository;
    }

    public Department createDepartment(
            String code,
            String name,
            String description
    ) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Department code is required."
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Department name is required."
            );
        }

        String cleanCode = code.trim().toUpperCase();

        boolean codeExists = departmentRepository.findAll()
                .stream()
                .anyMatch(department ->
                        department.getCode()
                                .equalsIgnoreCase(cleanCode)
                );

        if (codeExists) {
            throw new IllegalArgumentException(
                    "A department with this code already exists."
            );
        }

        boolean nameExists = departmentRepository.findAll()
                .stream()
                .anyMatch(department ->
                        department.getName()
                                .equalsIgnoreCase(name.trim())
                );

        if (nameExists) {
            throw new IllegalArgumentException(
                    "A department with this name already exists."
            );
        }

        Department department = new Department();

        department.setCode(cleanCode);
        department.setName(name.trim());
        department.setDescription(
                description == null ? null : description.trim()
        );
        department.setActive(true);

        return departmentRepository.save(department);
    }

    @Transactional(readOnly = true)
    public Department getDepartment(Long departmentId) {

        return departmentRepository.findById(departmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found."
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department updateDepartment(
            Long departmentId,
            String name,
            String description
    ) {

        Department department = getDepartment(departmentId);

        if (name != null && !name.isBlank()) {
            department.setName(name.trim());
        }

        if (description != null) {
            department.setDescription(description.trim());
        }

        return departmentRepository.save(department);
    }

    public Department deactivateDepartment(Long departmentId) {

        Department department = getDepartment(departmentId);

        department.setActive(false);

        return departmentRepository.save(department);
    }

    public Department activateDepartment(Long departmentId) {

        Department department = getDepartment(departmentId);

        department.setActive(true);

        return departmentRepository.save(department);
    }
}