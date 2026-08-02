package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.DiseaseType;
import com.solomon.epiforecaster.backend.service.DiseaseTypeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disease-types")
@CrossOrigin(origins = "*")
public class DiseaseTypeController {

    private final DiseaseTypeService service;

    public DiseaseTypeController(DiseaseTypeService service) {
        this.service = service;
    }

    @GetMapping
    public List<DiseaseType> getAllDiseaseTypes() {
        return service.getAllDiseaseTypes();
    }

    @GetMapping("/{id}")
    public DiseaseType getDiseaseType(@PathVariable Long id) {
        return service.getDiseaseType(id);
    }

    @PostMapping
    public DiseaseType saveDiseaseType(@RequestBody DiseaseType diseaseType) {
        return service.saveDiseaseType(diseaseType);
    }

    @DeleteMapping("/{id}")
    public void deleteDiseaseType(@PathVariable Long id) {
        service.deleteDiseaseType(id);
    }
}