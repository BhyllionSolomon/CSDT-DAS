package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.SystemCheckService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-check")
@CrossOrigin(origins = "*")
public class SystemCheckController {

    private final SystemCheckService service;

    public SystemCheckController(SystemCheckService service) {
        this.service = service;
    }

    @GetMapping
    public SystemCheckService.Report run(Authentication a) {
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return service.run(a.getName());
    }
}