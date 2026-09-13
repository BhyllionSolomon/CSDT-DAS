package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.ResultCsvService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/results/csv")
@CrossOrigin(origins = "*")
public class ResultCsvController {

    private final ResultCsvService resultCsvService;

    public ResultCsvController(ResultCsvService resultCsvService) {
        this.resultCsvService = resultCsvService;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(
            @RequestParam Long courseId,
            @RequestParam Long academicSessionId,
            @RequestParam String semester
    ) {

        byte[] file = resultCsvService.generateTemplate(
                courseId, academicSessionId, semester
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData(
                "attachment",
                "result-template-course-" + courseId + ".csv"
        );
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length)
                .body(file);
    }

    @PostMapping("/upload")
    public ResponseEntity<ResultCsvService.CsvUploadResult> uploadScores(
            @RequestParam Long courseId,
            @RequestParam Long academicSessionId,
            @RequestParam String semester,
            @RequestParam MultipartFile file
    ) {

        ResultCsvService.CsvUploadResult result =
                resultCsvService.processUpload(
                        courseId, academicSessionId, semester, file
                );

        return ResponseEntity.ok(result);
    }
}