package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.service.LevelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/levels")
@CrossOrigin(origins = "*")
public class LevelController {

    private final LevelService levelService;

    public LevelController(LevelService levelService) {
        this.levelService = levelService;
    }

    @PostMapping
    public ResponseEntity<Level> createLevel(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam Integer levelNumber
    ) {

        Level level =
                levelService.createLevel(
                        code,
                        name,
                        levelNumber
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(level);
    }

    @GetMapping
    public ResponseEntity<List<Level>> getAllLevels() {

        return ResponseEntity.ok(
                levelService.getAllLevels()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Level> getLevel(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                levelService.getLevel(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Level> updateLevel(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam Integer levelNumber
    ) {

        Level level =
                levelService.updateLevel(
                        id,
                        name,
                        levelNumber
                );

        return ResponseEntity.ok(level);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Level> activateLevel(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                levelService.activateLevel(id)
        );
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Level> deactivateLevel(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                levelService.deactivateLevel(id)
        );
    }
}
