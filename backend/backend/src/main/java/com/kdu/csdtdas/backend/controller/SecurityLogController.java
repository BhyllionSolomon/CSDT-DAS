package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.SecurityLogService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security-log")
@CrossOrigin(origins = "*")
public class SecurityLogController {

    private final SecurityLogService service;

    public SecurityLogController(SecurityLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<SecurityLogService.Event> recent(Authentication a) {
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return service.recent(a.getName());
    }
}