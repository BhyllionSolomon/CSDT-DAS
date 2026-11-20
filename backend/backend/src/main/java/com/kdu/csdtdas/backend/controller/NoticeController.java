package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.NoticeService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class NoticeController {

    private final NoticeService service;

    public NoticeController(NoticeService service) {
        this.service = service;
    }

    // Reading is kept under /academic-sessions, a path students are already allowed to reach.
    // Everyone gets only the notices meant for them; the person comes from the sign-in.
    @GetMapping("/academic-sessions/notices")
    public NoticeService.Board board(Authentication a) {
        return service.board(name(a));
    }

    @GetMapping("/academic-sessions/notices/{id}/read")
    public NoticeService.Message read(@PathVariable Long id, Authentication a) {
        return service.markRead(name(a), id);
    }

    // Posting is for staff only.
    @GetMapping("/notices/targets")
    public NoticeService.Targets targets(Authentication a) {
        return service.targets(name(a));
    }

    @PostMapping("/notices")
    public NoticeService.Message create(@RequestBody NoticeService.Request body, Authentication a) {
        return service.create(name(a), body);
    }

    @DeleteMapping("/notices/{id}")
    public NoticeService.Message delete(@PathVariable Long id, Authentication a) {
        return service.delete(name(a), id);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}