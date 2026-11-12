package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.GraduatesService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graduates")
@CrossOrigin(origins = "*")
public class GraduatesController {

    private final GraduatesService service;

    public GraduatesController(GraduatesService service) {
        this.service = service;
    }

    @GetMapping
    public List<GraduatesService.Row> list(Authentication a) {
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return service.list(a.getName());
    }
}