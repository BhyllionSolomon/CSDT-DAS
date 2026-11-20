package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.CarryOverMonitorService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carryover-monitor")
@CrossOrigin(origins = "*")
public class CarryOverMonitorController {

    private final CarryOverMonitorService service;

    public CarryOverMonitorController(CarryOverMonitorService service) {
        this.service = service;
    }

    @GetMapping
    public CarryOverMonitorService.Report monitor(Authentication a) {
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return service.monitor(a.getName());
    }
}