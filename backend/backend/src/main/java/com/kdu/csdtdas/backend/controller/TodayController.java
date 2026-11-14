package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.TodayService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TodayController {

    private final TodayService service;

    public TodayController(TodayService service) {
        this.service = service;
    }

    // Kept under /academic-sessions, a path students are already allowed to read.
    // Everyone gets only their own cards; the person comes from the sign-in, never from the request.
    @GetMapping("/academic-sessions/today")
    public TodayService.Today today(Authentication a) {
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return service.today(a.getName());
    }
}