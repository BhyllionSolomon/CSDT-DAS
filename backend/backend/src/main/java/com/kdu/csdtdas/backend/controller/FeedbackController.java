package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.FeedbackDTOs;
import com.kdu.csdtdas.backend.service.FeedbackService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @GetMapping("/window")
    public FeedbackDTOs.WindowInfo window(@RequestParam Long sessionId, @RequestParam String semester) {
        return service.window(sessionId, semester);
    }

    @PutMapping("/window")
    public FeedbackDTOs.WindowInfo setWindow(@RequestBody FeedbackDTOs.WindowRequest body, Authentication a) {
        return service.setWindow(name(a), body.sessionId(), body.semester(), Boolean.TRUE.equals(body.open()));
    }

    @GetMapping("/mine")
    public FeedbackDTOs.Mine mine(@RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.mine(name(a), sessionId, semester);
    }

    @PostMapping("/submit")
    public FeedbackDTOs.Message submit(@RequestBody FeedbackDTOs.SubmitRequest body, Authentication a) {
        return service.submit(name(a), body);
    }

    @GetMapping("/results")
    public FeedbackDTOs.Results results(@RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.results(name(a), sessionId, semester);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}