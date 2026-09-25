package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ResultAnalytics;
import com.kdu.csdtdas.backend.dto.SaveSelfReportedResultsRequest;
import com.kdu.csdtdas.backend.dto.SelfReportedResultRow;
import com.kdu.csdtdas.backend.service.SelfReportedResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/self-results")
@CrossOrigin(origins = "*")
public class SelfReportedResultController {

    private final SelfReportedResultService service;

    public SelfReportedResultController(SelfReportedResultService service) {
        this.service = service;
    }

    @PostMapping("/extract")
    public ResponseEntity<List<SelfReportedResultRow>> extract(@RequestParam MultipartFile file) {
        return ResponseEntity.ok(service.extractResults(file));
    }

    @PostMapping("/save")
    public ResponseEntity<Integer> save(@RequestBody SaveSelfReportedResultsRequest request) {
        return ResponseEntity.ok(service.saveResults(request.getStudentId(), request.getRows()));
    }

    @GetMapping("/analytics/{studentId}")
    public ResponseEntity<ResultAnalytics> analytics(@PathVariable Long studentId) {
        return ResponseEntity.ok(service.getAnalytics(studentId));
    }
}