package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.LabStudentDTOs;
import com.kdu.csdtdas.backend.service.LabManualStudentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/lab/student/manuals")
public class LabManualStudentController {
    private final LabManualStudentService service;
    public LabManualStudentController(LabManualStudentService service) { this.service=service; }

    @GetMapping
    public List<LabStudentDTOs.ManualOption> catalogue(Authentication a) {
        return service.catalogue(username(a));
    }
    @PostMapping("/orders")
    public LabStudentDTOs.OrderRow request(@RequestBody LabStudentDTOs.OrderRequest body, Authentication a) {
        return service.request(username(a), body);
    }
    private String username(Authentication a) {
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal()))
            throw new IllegalArgumentException("Please sign in.");
        return a.getName();
    }
}
