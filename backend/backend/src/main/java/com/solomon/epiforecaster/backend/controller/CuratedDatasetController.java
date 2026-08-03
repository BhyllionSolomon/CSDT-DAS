package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.service.CuratedDatasetGeneratorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/curated")
@CrossOrigin(origins = "*")
public class CuratedDatasetController {

    private final CuratedDatasetGeneratorService generatorService;

    public CuratedDatasetController(
            CuratedDatasetGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateDataset() {

        try {

            generatorService.generate();

            return ResponseEntity.ok(
                    "Curated Dataset Generated Successfully."
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "Failed to generate curated dataset.\n\n"
                                    + ex.getMessage()
                    );
        }
    }
}