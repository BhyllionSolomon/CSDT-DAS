package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.UnitLimitResponse;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.service.SemesterUnitLimitService;
import com.kdu.csdtdas.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/unit-limits")
@CrossOrigin(origins = "*")
public class SemesterUnitLimitController {

    private final SemesterUnitLimitService unitLimitService;
    private final UserService userService;

    public SemesterUnitLimitController(
            SemesterUnitLimitService unitLimitService,
            UserService userService
    ) {
        this.unitLimitService = unitLimitService;
        this.userService = userService;
    }

    @GetMapping("/programme/{programmeId}/level/{levelId}/semester/{semester}")
    public ResponseEntity<UnitLimitResponse> getRequiredUnits(
            @PathVariable Long programmeId, @PathVariable Long levelId, @PathVariable String semester
    ) {
        int required = unitLimitService.getRequiredUnits(programmeId, levelId, semester);
        return ResponseEntity.ok(new UnitLimitResponse(programmeId, levelId, semester, required));
    }

    @PutMapping("/programme/{programmeId}/level/{levelId}/semester/{semester}")
    public ResponseEntity<UnitLimitResponse> setRequiredUnits(
            @PathVariable Long programmeId, @PathVariable Long levelId, @PathVariable String semester,
            @RequestBody UnitLimitResponse request,
            Authentication authentication
    ) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            User currentUser = userService.getByUsername(authentication.getName());
            Long userProgrammeId = currentUser.getProgramme() != null
                    ? currentUser.getProgramme().getId() : null;

            unitLimitService.checkAdviserAuthority(
                    currentUser.getRole(), userProgrammeId, programmeId
            );
        }

        unitLimitService.setRequiredUnits(programmeId, levelId, semester, request.getRequiredUnits());
        return ResponseEntity.ok(new UnitLimitResponse(programmeId, levelId, semester, request.getRequiredUnits()));
    }
}