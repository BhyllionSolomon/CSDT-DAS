package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.AcademicSessionRequest;
import com.kdu.csdtdas.backend.dto.AcademicSessionResponse;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.mapper.AcademicSessionMapper;
import com.kdu.csdtdas.backend.service.AcademicSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-sessions")
@CrossOrigin(origins = "*")
public class AcademicSessionController {

    private final AcademicSessionService academicSessionService;
    private final AcademicSessionMapper academicSessionMapper;

    public AcademicSessionController(
            AcademicSessionService academicSessionService,
            AcademicSessionMapper academicSessionMapper
    ) {
        this.academicSessionService = academicSessionService;
        this.academicSessionMapper = academicSessionMapper;
    }

    @PostMapping
    public ResponseEntity<AcademicSessionResponse> createSession(
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
                .body(academicSessionMapper.toResponse(session));
    }

    @GetMapping
    public ResponseEntity<List<AcademicSessionResponse>> getAllSessions() {

        List<AcademicSessionResponse> responses =
                academicSessionService.getAllSessions()
                        .stream()
                        .map(academicSessionMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicSessionResponse> getSession(
            @PathVariable Long id
    ) {

        AcademicSession session =
                academicSessionService.getSession(id);

        return ResponseEntity.ok(
                academicSessionMapper.toResponse(session)
        );
    }

    @PutMapping("/{id}/current")
    public ResponseEntity<AcademicSessionResponse> setCurrentSession(
            @PathVariable Long id
    ) {

        AcademicSession session =
                academicSessionService.setCurrentSession(id);

        return ResponseEntity.ok(
                academicSessionMapper.toResponse(session)
        );
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<AcademicSessionResponse> activateSession(
            @PathVariable Long id
    ) {

        AcademicSession session =
                academicSessionService.activateSession(id);

        return ResponseEntity.ok(
                academicSessionMapper.toResponse(session)
        );
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<AcademicSessionResponse> deactivateSession(
            @PathVariable Long id
    ) {

        AcademicSession session =
                academicSessionService.deactivateSession(id);

        return ResponseEntity.ok(
                academicSessionMapper.toResponse(session)
        );
    }
}

