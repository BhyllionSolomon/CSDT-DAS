package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.dto.AcademicSessionRequest;
import com.solomon.epiforecaster.backend.entity.AcademicSession;
import com.solomon.epiforecaster.backend.service.AcademicSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-sessions")
@CrossOrigin(origins = "*")
public class AcademicSessionController {

    private final AcademicSessionService academicSessionService;

    public AcademicSessionController(
            AcademicSessionService academicSessionService
    ) {
        this.academicSessionService = academicSessionService;
    }

    @PostMapping
    public ResponseEntity<AcademicSession> createSession(
            @RequestBody AcademicSessionRequest request
    ) {

        AcademicSession session =
                academicSessionService.createSession(
                        request.getName(),
                        request.getStartDate(),
                        request.getEndDate()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(session);
    }

    @GetMapping
    public ResponseEntity<List<AcademicSession>> getAllSessions() {

        return ResponseEntity.ok(
                academicSessionService.getAllSessions()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicSession> getSession(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                academicSessionService.getSession(id)
        );
    }

    @PutMapping("/{id}/current")
    public ResponseEntity<AcademicSession> setCurrentSession(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                academicSessionService.setCurrentSession(id)
        );
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<AcademicSession> activateSession(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                academicSessionService.activateSession(id)
        );
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<AcademicSession> deactivateSession(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                academicSessionService.deactivateSession(id)
        );
    }
}