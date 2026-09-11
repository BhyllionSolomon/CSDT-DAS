package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.Department;
import com.solomon.epiforecaster.backend.entity.Programme;
import com.solomon.epiforecaster.backend.repository.DepartmentRepository;
import com.solomon.epiforecaster.backend.repository.ProgrammeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProgrammeService {

    private final ProgrammeRepository programmeRepository;
    private final DepartmentRepository departmentRepository;

    public ProgrammeService(
            ProgrammeRepository programmeRepository,
            DepartmentRepository departmentRepository
    ) {
        this.programmeRepository = programmeRepository;
        this.departmentRepository = departmentRepository;
    }

    public Programme createProgramme(
            String code,
            String name,
            Long departmentId,
            String description
    ) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Programme code is required."
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Programme name is required."
            );
        }

        if (departmentId == null) {
            throw new IllegalArgumentException(
                    "Department is required."
            );
        }

        String cleanCode = code.trim().toUpperCase();
        String cleanName = name.trim();

        boolean codeExists = programmeRepository.findAll()
                .stream()
                .anyMatch(programme ->
                        programme.getCode()
                                .equalsIgnoreCase(cleanCode)
                );

        if (codeExists) {
            throw new IllegalArgumentException(
                    "A programme with this code already exists."
            );
        }

        boolean nameExists = programmeRepository.findAll()
                .stream()
                .anyMatch(programme ->
                        programme.getName()
                                .equalsIgnoreCase(cleanName)
                );

        if (nameExists) {
            throw new IllegalArgumentException(
                    "A programme with this name already exists."
            );
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found."
                        )
                );

        if (!department.getActive()) {
            throw new IllegalArgumentException(
                    "The selected department is not active."
            );
        }

        Programme programme = new Programme();

        programme.setCode(cleanCode);
        programme.setName(cleanName);
        programme.setDepartment(department);
        programme.setDescription(
                description == null ? null : description.trim()
        );
        programme.setActive(true);

        return programmeRepository.save(programme);
    }

    @Transactional(readOnly = true)
    public Programme getProgramme(Long programmeId) {

        return programmeRepository.findByIdWithDepartment(programmeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Programme not found."
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<Programme> getAllProgrammes() {

        return programmeRepository.findAllWithDepartment();
    }

    public Programme updateProgramme(
            Long programmeId,
            String name,
            String description
    ) {

        Programme programme =
                programmeRepository.findByIdWithDepartment(programmeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Programme not found."
                                )
                        );

        if (name != null && !name.isBlank()) {
            programme.setName(name.trim());
        }

        if (description != null) {
            programme.setDescription(description.trim());
        }

        return programmeRepository.save(programme);
    }

    public Programme deactivateProgramme(Long programmeId) {

        Programme programme =
                programmeRepository.findByIdWithDepartment(programmeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Programme not found."
                                )
                        );

        programme.setActive(false);

        return programmeRepository.save(programme);
    }

    public Programme activateProgramme(Long programmeId) {

        Programme programme =
                programmeRepository.findByIdWithDepartment(programmeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Programme not found."
                                )
                        );

        programme.setActive(true);

        return programmeRepository.save(programme);
    }
}

