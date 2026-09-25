package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.SaveStudentRosterRequest;
import com.kdu.csdtdas.backend.dto.StudentRosterRow;
import com.kdu.csdtdas.backend.dto.StudentRosterSaveResult;
import com.kdu.csdtdas.backend.service.StudentRosterUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/students/roster")
@CrossOrigin(origins = "*")
public class StudentRosterController {

    private final StudentRosterUploadService rosterService;

    public StudentRosterController(StudentRosterUploadService rosterService) {
        this.rosterService = rosterService;
    }

    @PostMapping("/extract")
    public ResponseEntity<List<StudentRosterRow>> extract(@RequestParam MultipartFile file) {
        return ResponseEntity.ok(rosterService.extractRoster(file));
    }

    @PostMapping("/save")
    public ResponseEntity<StudentRosterSaveResult> save(
            @RequestBody SaveStudentRosterRequest request, Authentication authentication
    ) {
        String uploadedBy = authentication != null ? authentication.getName() : "unknown";
        StudentRosterSaveResult result = rosterService.saveRoster(
                request.getProgrammeId(), request.getLevelId(), request.getAcademicSessionId(),
                request.getRows(), uploadedBy
        );
        return ResponseEntity.ok(result);
    }
}