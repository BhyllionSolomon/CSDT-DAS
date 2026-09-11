package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ProgrammeResponse;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.mapper.ProgrammeMapper;
import com.kdu.csdtdas.backend.service.ProgrammeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programmes")
@CrossOrigin(origins = "*")
public class ProgrammeController {

    private final ProgrammeService programmeService;
    private final ProgrammeMapper programmeMapper;

    public ProgrammeController(
            ProgrammeService programmeService,
            ProgrammeMapper programmeMapper
    ) {
        this.programmeService = programmeService;
        this.programmeMapper = programmeMapper;
    }

    @PostMapping
    public ResponseEntity<ProgrammeResponse> createProgramme(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String description
    ) {

        Programme programme = programmeService.createProgramme(
                code,
                name,
                departmentId,
                description
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(programmeMapper.toResponse(programme));
    }

    @GetMapping
    public ResponseEntity<List<ProgrammeResponse>> getAllProgrammes() {

        List<ProgrammeResponse> responses =
                programmeService.getAllProgrammes()
                        .stream()
                        .map(programmeMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgrammeResponse> getProgramme(
            @PathVariable Long id
    ) {

        Programme programme = programmeService.getProgramme(id);

        return ResponseEntity.ok(
                programmeMapper.toResponse(programme)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProgrammeResponse> updateProgramme(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description
    ) {

        Programme programme = programmeService.updateProgramme(
                id,
                name,
                description
        );

        return ResponseEntity.ok(
                programmeMapper.toResponse(programme)
        );
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ProgrammeResponse> activateProgramme(
            @PathVariable Long id
    ) {

        Programme programme =
                programmeService.activateProgramme(id);

        return ResponseEntity.ok(
                programmeMapper.toResponse(programme)
        );
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ProgrammeResponse> deactivateProgramme(
            @PathVariable Long id
    ) {

        Programme programme =
                programmeService.deactivateProgramme(id);

        return ResponseEntity.ok(
                programmeMapper.toResponse(programme)
        );
    }
}

