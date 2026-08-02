package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.service.CsvUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins="*")
public class CsvUploadController {

    private final CsvUploadService csvUploadService;

    public CsvUploadController(CsvUploadService csvUploadService) {
        this.csvUploadService = csvUploadService;
    }

    @PostMapping("/csv")
    public ResponseEntity<String> uploadCSV(
            @RequestParam("file") MultipartFile file) {

        csvUploadService.importCsv(file);

        return ResponseEntity.ok("Upload Successful");
    }

}