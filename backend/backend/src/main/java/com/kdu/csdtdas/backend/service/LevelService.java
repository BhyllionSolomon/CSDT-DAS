package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LevelService {

    private final LevelRepository levelRepository;

    public LevelService(LevelRepository levelRepository) {
        this.levelRepository = levelRepository;
    }

    public Level createLevel(
            String code,
            String name,
            Integer levelNumber
    ) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Level code is required."
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Level name is required."
            );
        }

        if (levelNumber == null || levelNumber <= 0) {
            throw new IllegalArgumentException(
                    "Level number must be greater than zero."
            );
        }

        String cleanCode = code.trim().toUpperCase();
        String cleanName = name.trim();

        boolean codeExists = levelRepository.findAll()
                .stream()
                .anyMatch(level ->
                        level.getCode()
                                .equalsIgnoreCase(cleanCode)
                );

        if (codeExists) {
            throw new IllegalArgumentException(
                    "A level with this code already exists."
            );
        }

        boolean levelNumberExists = levelRepository.findAll()
                .stream()
                .anyMatch(level ->
                        level.getLevelNumber().equals(levelNumber)
                );

        if (levelNumberExists) {
            throw new IllegalArgumentException(
                    "A level with this level number already exists."
            );
        }

        Level level = new Level();

        level.setCode(cleanCode);
        level.setName(cleanName);
        level.setLevelNumber(levelNumber);
        level.setActive(true);

        return levelRepository.save(level);
    }

    @Transactional(readOnly = true)
    public Level getLevel(Long levelId) {

        return levelRepository.findById(levelId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Level not found."
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<Level> getAllLevels() {
        return levelRepository.findAll();
    }

    public Level updateLevel(
            Long levelId,
            String name,
            Integer levelNumber
    ) {

        Level level = getLevel(levelId);

        if (name != null && !name.isBlank()) {
            level.setName(name.trim());
        }

        if (levelNumber != null && levelNumber > 0) {
            level.setLevelNumber(levelNumber);
        }

        return levelRepository.save(level);
    }

    public Level deactivateLevel(Long levelId) {

        Level level = getLevel(levelId);

        level.setActive(false);

        return levelRepository.save(level);
    }

    public Level activateLevel(Long levelId) {

        Level level = getLevel(levelId);

        level.setActive(true);

        return levelRepository.save(level);
    }
}
