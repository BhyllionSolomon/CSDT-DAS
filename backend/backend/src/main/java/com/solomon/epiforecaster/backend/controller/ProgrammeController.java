package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.Programme;
import com.solomon.epiforecaster.backend.service.ProgrammeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programmes")
@CrossOrigin(origins = "*")
public class ProgrammeController {

    private final ProgrammeService programmeService;

    public ProgrammeController(ProgrammeService programmeService) {
        this.programmeService = programmeService;
    }

    @PostMapping
    public ResponseEntity<Programme> createProgramme(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String description
    ) {

        Programme programme =
                programmeService.createProgramme(
                        code,
                        name,
                        departmentId,
                        description
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(programme);
    }

    @GetMapping
    public ResponseEntity<List<Programme>> getAllProgrammes() {

        return ResponseEntity.ok(
                programmeService.getAllProgrammes()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Programme> getProgramme(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                programmeService.getProgramme(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Programme> updateProgramme(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description
    ) {

        Programme programme =
                programmeService.updateProgramme(
                        id,
                        name,
                        description
                );

        return ResponseEntity.ok(programme);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Programme> activateProgramme(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                programmeService.activateProgramme(id)
        );
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Programme> deactivateProgramme(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                programmeService.deactivateProgramme(id)
        );
    }
}