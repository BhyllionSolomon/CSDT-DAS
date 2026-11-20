package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.LecturerPerformanceDTOs;
import com.kdu.csdtdas.backend.service.LecturerPerformanceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lecturer-performance")
@CrossOrigin(origins = "*")
public class LecturerPerformanceController {

    private final LecturerPerformanceService service;

    public LecturerPerformanceController(LecturerPerformanceService service) {
        this.service = service;
    }

    @GetMapping
    public LecturerPerformanceDTOs.Report report(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) String semester,
            Authentication a
    ) {
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return service.report(a.getName(), sessionId, semester);
    }
}