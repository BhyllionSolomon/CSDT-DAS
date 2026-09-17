package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.ResultCsvService;
import com.kdu.csdtdas.backend.service.ResultDocxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/results/docx")
@CrossOrigin(origins = "*")
public class ResultDocxController {

    private final ResultDocxService resultDocxService;

    public ResultDocxController(ResultDocxService resultDocxService) {
        this.resultDocxService = resultDocxService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ResultCsvService.CsvUploadResult> uploadScores(
            @RequestParam Long courseId,
            @RequestParam Long academicSessionId,
            @RequestParam String semester,
            @RequestParam MultipartFile file
    ) {
        return ResponseEntity.ok(
                resultDocxService.processUpload(courseId, academicSessionId, semester, file)
        );
    }
}