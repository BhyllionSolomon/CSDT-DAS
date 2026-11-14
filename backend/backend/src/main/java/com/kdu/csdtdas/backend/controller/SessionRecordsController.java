package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.SessionRecordsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session-records")
@CrossOrigin(origins = "*")
public class SessionRecordsController {

    public record AssignRequest(Long sessionId) {}

    private final SessionRecordsService service;

    public SessionRecordsController(SessionRecordsService service) {
        this.service = service;
    }

    @GetMapping("/check")
    public SessionRecordsService.Check check(Authentication a) {
        return service.check(name(a));
    }

    @PostMapping("/assign-session")
    public SessionRecordsService.Message assign(@RequestBody AssignRequest body, Authentication a, HttpServletRequest r) {
        return service.assignSession(name(a), ip(r), body.sessionId());
    }

    @PostMapping("/sync")
    public SessionRecordsService.SyncReport sync(Authentication a, HttpServletRequest r) {
        return service.sync(name(a), ip(r));
    }

    @PostMapping("/estimate")
    public SessionRecordsService.EstimateReport estimate(@RequestParam(defaultValue = "false") boolean apply, Authentication a, HttpServletRequest r) {
        return service.estimate(name(a), ip(r), apply);
    }

    @PostMapping("/estimate/undo")
    public SessionRecordsService.Message undo(@RequestParam Long sessionId, Authentication a, HttpServletRequest r) {
        return service.undoEstimate(name(a), ip(r), sessionId);
    }

    @GetMapping("/session/{sessionId}")
    public List<SessionRecordsService.Row> forSession(@PathVariable Long sessionId, Authentication a) {
        return service.forSession(name(a), sessionId);
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}