package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.LabAssetDTOs;
import com.kdu.csdtdas.backend.service.LabAssetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lab/assets")
public class LabAssetController {
    private final LabAssetService service;

    public LabAssetController(LabAssetService service) { this.service = service; }

    @GetMapping
    public List<LabAssetDTOs.AssetRow> list(Authentication authentication) {
        return service.list(username(authentication));
    }

    @PostMapping
    public ResponseEntity<LabAssetDTOs.AssetRow> create(
            @RequestBody LabAssetDTOs.AssetRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(username(authentication), request));
    }

    @PutMapping("/{id}")
    public LabAssetDTOs.AssetRow update(@PathVariable Long id,
            @RequestBody LabAssetDTOs.AssetRequest request, Authentication authentication) {
        return service.update(username(authentication), id, request);
    }

    @PatchMapping("/{id}/condition")
    public LabAssetDTOs.AssetRow updateCondition(@PathVariable Long id,
            @RequestBody LabAssetDTOs.ConditionRequest request, Authentication authentication) {
        return service.updateCondition(username(authentication), id, request);
    }

    private String username(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}