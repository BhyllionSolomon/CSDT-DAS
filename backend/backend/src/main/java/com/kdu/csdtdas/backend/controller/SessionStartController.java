package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.SessionStartService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clearance/session-start")
@CrossOrigin(origins = "*")
public class SessionStartController {

    public record ApplyRequest(Long sessionId, List<Long> excludeStudentIds) {}

    private final SessionStartService service;

    public SessionStartController(SessionStartService service) {
        this.service = service;
    }

    @GetMapping("/preview")
    public SessionStartService.Preview preview(@RequestParam Long sessionId) {
        return service.preview(sessionId);
    }

    @PostMapping("/apply")
    public SessionStartService.Result apply(@RequestBody ApplyRequest body, Authentication authentication) {
        return service.apply(body.sessionId(), body.excludeStudentIds(), authentication.getName());
    }
}