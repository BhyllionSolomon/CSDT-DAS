package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.AnalyticsDTOs;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.service.PerformanceAnalyticsService;
import com.kdu.csdtdas.backend.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final PerformanceAnalyticsService performanceService;
    private final UserService userService;

    public AnalyticsController(PerformanceAnalyticsService performanceService, UserService userService) {
        this.performanceService = performanceService;
        this.userService = userService;
    }

    @GetMapping("/performance")
    public AnalyticsDTOs.Cohort performance(
            @RequestParam Long sessionId,
            @RequestParam(required = false) Long programmeId,
            @RequestParam(required = false) Long levelId,
            Authentication authentication
    ) {
        User user = userService.getByUsername(name(authentication));
        String role = String.valueOf(user.getRole()).toUpperCase();

        if (role.equals("HOD") || role.equals("ADMIN")) {
            return performanceService.cohort(sessionId, programmeId, levelId);
        }

        if (role.equals("LEVEL_ADVISER") && user.getProgramme() != null) {
            Long myProgramme = user.getProgramme().getId();

            if (programmeId != null && !programmeId.equals(myProgramme)) {
                throw new IllegalArgumentException("You can only view analytics for your own programme.");
            }

            Long myLevel = user.getLevel() != null ? user.getLevel().getId() : levelId;
            return performanceService.cohort(sessionId, myProgramme, myLevel);
        }

        throw new IllegalArgumentException("You do not have permission to view these analytics.");
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}