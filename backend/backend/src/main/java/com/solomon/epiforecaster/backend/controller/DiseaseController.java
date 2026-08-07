package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.service.DiseaseRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diseases")
@CrossOrigin(origins = "*")
public class DiseaseController {

    private final DiseaseRecordService diseaseRecordService;

    public DiseaseController(DiseaseRecordService diseaseRecordService) {
        this.diseaseRecordService = diseaseRecordService;
    }

    @GetMapping
    public ResponseEntity<List<String>> getDiseases() {
        return ResponseEntity.ok(
                diseaseRecordService.getDiseases()
        );
    }
}