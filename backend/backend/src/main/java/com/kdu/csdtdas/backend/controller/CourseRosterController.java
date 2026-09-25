package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.CourseRosterRow;
import com.kdu.csdtdas.backend.dto.CourseRosterSaveResult;
import com.kdu.csdtdas.backend.dto.SaveCourseRosterRequest;
import com.kdu.csdtdas.backend.service.CourseRosterUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/courses/roster")
@CrossOrigin(origins = "*")
public class CourseRosterController {

    private final CourseRosterUploadService rosterService;

    public CourseRosterController(CourseRosterUploadService rosterService) {
        this.rosterService = rosterService;
    }

    @PostMapping("/extract")
    public ResponseEntity<List<CourseRosterRow>> extract(@RequestParam MultipartFile file) {
        return ResponseEntity.ok(rosterService.extractRoster(file));
    }

    @PostMapping("/save")
    public ResponseEntity<CourseRosterSaveResult> save(@RequestBody SaveCourseRosterRequest request) {
        return ResponseEntity.ok(rosterService.saveRoster(
                request.getProgrammeId(), request.getLevelId(), request.getSemester(), request.getRows()
        ));
    }
}