package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.AdviserDTOs;
import com.kdu.csdtdas.backend.service.AdviserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/advisers")
@CrossOrigin(origins = "*")
public class AdviserController {

    private final AdviserService adviserService;

    public AdviserController(AdviserService adviserService) {
        this.adviserService = adviserService;
    }

    @PutMapping("/{userId}/assign")
    public AdviserDTOs.Message assign(
            @PathVariable Long userId, @RequestBody AdviserDTOs.Assign body, Authentication authentication
    ) {
        return adviserService.assign(userId, body.programmeId(), body.levelId(), actor(authentication));
    }

    @PutMapping("/{userId}/remove")
    public AdviserDTOs.Message remove(
            @PathVariable Long userId,
            @RequestBody(required = false) AdviserDTOs.Remove body,
            Authentication authentication
    ) {
        return adviserService.remove(userId, body == null ? null : body.reason(), actor(authentication));
    }

    @GetMapping("/history")
    public List<AdviserDTOs.Entry> history() {
        return adviserService.history();
    }

    @GetMapping("/mine")
    public ResponseEntity<AdviserDTOs.Contact> mine(Authentication authentication) {
        return adviserService.contactForStudent(actor(authentication))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private String actor(Authentication authentication) {
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be logged in.");
        }
        return authentication.getName();
    }
}