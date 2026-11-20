package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.CohortService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cohorts")
@CrossOrigin(origins = "*")
public class CohortController {

    private final CohortService service;

    public CohortController(CohortService service) {
        this.service = service;
    }

    @GetMapping("/coverage")
    public CohortService.Coverage coverage(@RequestParam int intakeYear, Authentication a) {
        return service.coverage(name(a), intakeYear);
    }

    @PostMapping("/verify")
    public CohortService.Message verify(@RequestBody CohortService.VerifyRequest body, Authentication a) {
        return service.verify(name(a), body);
    }

    @PostMapping("/check")
    public CohortService.CheckReport check(@RequestBody CohortService.CheckRequest body, Authentication a) {
        return service.check(name(a), body);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}