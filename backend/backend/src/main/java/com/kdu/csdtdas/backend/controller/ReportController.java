package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ReportDTOs;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.service.ReportService;
import com.kdu.csdtdas.backend.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    public ReportController(ReportService reportService, UserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    @GetMapping("/broadsheet")
    public ReportDTOs.Broadsheet broadsheet(
            @RequestParam Long programmeId,
            @RequestParam Long levelId,
            @RequestParam Long sessionId,
            @RequestParam String semester,
            Authentication authentication
    ) {
        authorize(authentication, programmeId, levelId);
        return reportService.broadsheet(programmeId, levelId, sessionId, semester);
    }

    @GetMapping("/standing")
    public ReportDTOs.Standing standing(
            @RequestParam Long programmeId,
            @RequestParam Long levelId,
            @RequestParam Long sessionId,
            Authentication authentication
    ) {
        authorize(authentication, programmeId, levelId);
        return reportService.standing(programmeId, levelId, sessionId);
    }

    private void authorize(Authentication authentication, Long programmeId, Long levelId) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be logged in.");
        }

        User user = userService.getByUsername(authentication.getName());
        String role = user.getRole();

        if ("HOD".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            return;
        }

        if ("LEVEL_ADVISER".equalsIgnoreCase(role)) {
            boolean sameProgramme = user.getProgramme() != null
                    && user.getProgramme().getId().equals(programmeId);
            boolean sameLevel = user.getLevel() == null
                    || user.getLevel().getId().equals(levelId);

            if (sameProgramme && sameLevel) return;

            throw new IllegalArgumentException("You can only view reports for your own programme and level.");
        }

        throw new IllegalArgumentException("You do not have permission to view reports.");
    }
}