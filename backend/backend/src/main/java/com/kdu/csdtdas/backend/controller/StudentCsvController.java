package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.StudentCsvService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/students/csv")
@CrossOrigin(origins = "*")
public class StudentCsvController {

    private final StudentCsvService studentCsvService;

    public StudentCsvController(StudentCsvService studentCsvService) {
        this.studentCsvService = studentCsvService;
    }

    @PostMapping("/upload")
    public ResponseEntity<StudentCsvService.CsvUploadResult> uploadStudents(
            @RequestParam Long departmentId,
            @RequestParam Long academicSessionId,
            @RequestParam MultipartFile file
    ) {

        StudentCsvService.CsvUploadResult result =
                studentCsvService.processUpload(departmentId, academicSessionId, file);

        return ResponseEntity.ok(result);
    }
}