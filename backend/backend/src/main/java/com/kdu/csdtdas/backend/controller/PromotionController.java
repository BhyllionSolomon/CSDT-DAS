package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.PromotionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotion")
@CrossOrigin(origins = "*")
public class PromotionController {

    public record ApplyRequest(Long fromId, Long toId, List<Long> excludeStudentIds) {}

    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    @GetMapping("/preview")
    public PromotionService.Preview preview(@RequestParam Long fromId, @RequestParam Long toId, Authentication a) {
        return service.preview(name(a), fromId, toId);
    }

    @PostMapping("/apply")
    public PromotionService.Result apply(@RequestBody ApplyRequest body, Authentication a, HttpServletRequest r) {
        return service.apply(name(a), ip(r), body.fromId(), body.toId(), body.excludeStudentIds());
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