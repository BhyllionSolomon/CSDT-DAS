package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ClearanceDTOs;
import com.kdu.csdtdas.backend.service.RegistrationClearanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clearance")
@CrossOrigin(origins = "*")
public class ClearanceController {

    private final RegistrationClearanceService service;

    public ClearanceController(RegistrationClearanceService service) {
        this.service = service;
    }

    // ---- student

    @GetMapping("/mine")
    public ClearanceDTOs.Status mine(Authentication authentication) {
        return service.status(name(authentication));
    }

    @PostMapping("/submit")
    public ClearanceDTOs.Status submit(@RequestBody ClearanceDTOs.Submit body, Authentication authentication) {
        return service.submit(name(authentication), body.invoiceCode());
    }

    // ---- H.O.D

    @GetMapping
    public List<ClearanceDTOs.Entry> list(@RequestParam(required = false) String status) {
        return service.list(status);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ClearanceDTOs.Entry> forStudent(@PathVariable Long studentId) {
        return service.forStudent(studentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{id}/approve")
    public ClearanceDTOs.Entry approve(@PathVariable Long id, Authentication authentication) {
        return service.approve(id, name(authentication));
    }

    @PutMapping("/{id}/reject")
    public ClearanceDTOs.Entry reject(
            @PathVariable Long id,
            @RequestBody(required = false) ClearanceDTOs.Decision body,
            Authentication authentication
    ) {
        return service.reject(id, body == null ? null : body.note(), name(authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> reset(@PathVariable Long id) {
        service.reset(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assisted")
    public ClearanceDTOs.Entry assisted(@RequestBody ClearanceDTOs.Assisted body, Authentication authentication) {
        return service.assistedClear(body.studentId(), body.invoiceCode(), name(authentication));
    }

    @PostMapping("/approve-registered")
    public ClearanceDTOs.Count approveRegistered(Authentication authentication) {
        return service.approveRegistered(name(authentication));
    }

    @GetMapping("/settings")
    public ClearanceDTOs.GateSetting settings() {
        return new ClearanceDTOs.GateSetting(service.gateEnabled());
    }

    @PutMapping("/settings")
    public ClearanceDTOs.GateSetting updateSettings(@RequestBody ClearanceDTOs.GateSetting body) {
        return new ClearanceDTOs.GateSetting(service.setGate(body.enabled()));
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}