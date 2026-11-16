package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.SupervisionExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/export")
@CrossOrigin(origins = "*")
public class SupervisionExportController {

    private final SupervisionExportService service;

    public SupervisionExportController(SupervisionExportService service) {
        this.service = service;
    }

    @GetMapping("/preview")
    public SupervisionExportService.Preview preview(@RequestParam(required = false) Long sessionId, Authentication a) {
        return service.preview(name(a), sessionId);
    }

    @GetMapping("/word")
    public ResponseEntity<byte[]> word(@RequestParam(required = false) Long sessionId, Authentication a) throws IOException {
        byte[] file = service.word(name(a), sessionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + service.fileName(sessionId) + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(file);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}