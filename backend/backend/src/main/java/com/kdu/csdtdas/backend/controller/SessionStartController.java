package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.SessionStartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clearance/session-start")
@CrossOrigin(origins = "*")
public class SessionStartController {

    public record ApplyRequest(Long sessionId, List<Long> excludeStudentIds) {}

    public record CorrectRequest(Long fromId, Long toId) {}

    private final SessionStartService service;

    public SessionStartController(SessionStartService service) {
        this.service = service;
    }

    @GetMapping("/preview")
    public SessionStartService.Preview preview(@RequestParam Long sessionId) {
        return service.preview(sessionId);
    }

    @PostMapping("/apply")
    public SessionStartService.Result apply(@RequestBody ApplyRequest body, Authentication authentication, HttpServletRequest request) {
        return service.apply(body.sessionId(), body.excludeStudentIds(), authentication.getName(), ip(request));
    }

    @GetMapping("/correct/preview")
    public SessionStartService.CorrectPreview correctPreview(@RequestParam Long fromId, @RequestParam Long toId) {
        return service.correctPreview(fromId, toId);
    }

    @PostMapping("/correct/apply")
    public SessionStartService.Message correctApply(@RequestBody CorrectRequest body, Authentication authentication, HttpServletRequest request) {
        return service.correctApply(body.fromId(), body.toId(), authentication.getName(), ip(request));
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}