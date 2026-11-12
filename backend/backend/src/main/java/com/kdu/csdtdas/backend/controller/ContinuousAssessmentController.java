package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.CaDTOs;
import com.kdu.csdtdas.backend.service.ContinuousAssessmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ContinuousAssessmentController {

    private final ContinuousAssessmentService service;

    public ContinuousAssessmentController(ContinuousAssessmentService service) {
        this.service = service;
    }

    @GetMapping("/ca/courses")
    public List<CaDTOs.CourseInfo> courses(@RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.courses(name(a), sessionId, semester);
    }

    @GetMapping("/ca/sheet")
    public CaDTOs.Sheet sheet(@RequestParam Long courseId, @RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.sheet(name(a), courseId, sessionId, semester);
    }

    @PostMapping("/ca/items")
    public ResponseEntity<CaDTOs.ItemRow> create(@RequestBody CaDTOs.ItemRequest body, Authentication a) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createItem(name(a), body));
    }

    @PutMapping("/ca/items/{id}")
    public CaDTOs.ItemRow update(@PathVariable Long id, @RequestBody CaDTOs.ItemUpdate body, Authentication a) {
        return service.updateItem(name(a), id, body);
    }

    @DeleteMapping("/ca/items/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication a) {
        service.deleteItem(name(a), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/ca/items/{id}/scores")
    public ResponseEntity<Void> saveScores(@PathVariable Long id, @RequestBody List<CaDTOs.ScoreEntry> body, Authentication a) {
        service.saveScores(name(a), id, body);
        return ResponseEntity.noContent().build();
    }

    // Students: kept under /academic-sessions, a path students are already allowed to read.
    // The student comes from the sign-in, never from the request.
    @GetMapping("/academic-sessions/ca/mine")
    public CaDTOs.Mine mine(@RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.mine(name(a), sessionId, semester);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}