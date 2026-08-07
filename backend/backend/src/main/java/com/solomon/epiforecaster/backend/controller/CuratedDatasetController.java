package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.CuratedDataset;
import com.solomon.epiforecaster.backend.service.CuratedDatasetGeneratorService;
import com.solomon.epiforecaster.backend.service.CuratedDatasetService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/curated")
@CrossOrigin(origins = "*")
public class CuratedDatasetController {

    private final CuratedDatasetGeneratorService generatorService;
    private final CuratedDatasetService curatedDatasetService;

    public CuratedDatasetController(
            CuratedDatasetGeneratorService generatorService,
            CuratedDatasetService curatedDatasetService) {

        this.generatorService = generatorService;
        this.curatedDatasetService = curatedDatasetService;
    }

    /*
     * Generate Curated Dataset
     */

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

    /*
     * Get all curated records
     */

    @GetMapping
    public ResponseEntity<List<CuratedDataset>> getAll() {

        return ResponseEntity.ok(
                curatedDatasetService.getAll()
        );

    }

    /*
     * Download curated dataset as CSV
     */

    @GetMapping("/download")
    public ResponseEntity<String> downloadCsv() {

        List<CuratedDataset> records =
                curatedDatasetService.getAll();

        StringBuilder csv = new StringBuilder();

        csv.append("Disease,Country,State,LGA,Year,EpiWeek,SuspectedCases,ConfirmedCases,Deaths\n");

        for (CuratedDataset r : records) {

            csv.append(r.getDiseaseName()).append(",")
                    .append(r.getCountry()).append(",")
                    .append(r.getState()).append(",")
                    .append(r.getLga()).append(",")
                    .append(r.getYear()).append(",")
                    .append(r.getEpiWeek()).append(",")
                    .append(r.getSuspectedCases()).append(",")
                    .append(r.getConfirmedCases()).append(",")
                    .append(r.getDeaths()).append("\n");
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=curated_dataset.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv.toString());

    }

    /*
     * Delete one curated record
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        curatedDatasetService.delete(id);

        return ResponseEntity.noContent().build();

    }

    /*
     * Delete all curated records
     */

    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {

        curatedDatasetService.deleteAll();

        return ResponseEntity.noContent().build();

    }

}