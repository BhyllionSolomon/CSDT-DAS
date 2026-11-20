package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ProgressDTOs;
import com.kdu.csdtdas.backend.service.StudentProgressService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/results/student")
@CrossOrigin(origins = "*")
public class StudentProgressController {

    private final StudentProgressService service;

    public StudentProgressController(StudentProgressService service) {
        this.service = service;
    }

    // The safety filter already lets a student ask only about their own id; the service checks again.
    @GetMapping("/{studentId}/progress")
    public ProgressDTOs.Progress progress(@PathVariable Long studentId, Authentication a) {
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return service.progress(a.getName(), studentId);
    }
}