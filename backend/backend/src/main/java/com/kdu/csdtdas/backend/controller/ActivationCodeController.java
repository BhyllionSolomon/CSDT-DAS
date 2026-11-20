package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.config.ClientIp;
import com.kdu.csdtdas.backend.service.StudentSignupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activation-codes")
public class ActivationCodeController {

    public record IssueRequest(Long programmeId, Long levelId, Boolean regenerate) {}

    private final StudentSignupService service;

    public ActivationCodeController(StudentSignupService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public StudentSignupService.Status status(@RequestParam Long programmeId, @RequestParam Long levelId, Authentication a) {
        return service.status(name(a), programmeId, levelId);
    }

    @PostMapping("/issue")
    public ResponseEntity<StudentSignupService.IssueResult> issue(@RequestBody IssueRequest body, Authentication a, HttpServletRequest request) {
        StudentSignupService.IssueResult result = service.issue(
                name(a), ClientIp.of(request), body.programmeId(), body.levelId(), Boolean.TRUE.equals(body.regenerate()));
        // The codes are shown once, so nothing may keep a copy of this reply.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}