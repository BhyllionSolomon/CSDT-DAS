package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.SemesterUnitLimit;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
import com.kdu.csdtdas.backend.repository.SemesterUnitLimitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SemesterUnitLimitService {

    private static final int DEFAULT_REQUIRED_UNITS = 24;

    private final SemesterUnitLimitRepository unitLimitRepository;
    private final LevelRepository levelRepository;
    private final ProgrammeRepository programmeRepository;

    public SemesterUnitLimitService(
            SemesterUnitLimitRepository unitLimitRepository,
            LevelRepository levelRepository,
            ProgrammeRepository programmeRepository
    ) {
        this.unitLimitRepository = unitLimitRepository;
        this.levelRepository = levelRepository;
        this.programmeRepository = programmeRepository;
    }

    @Transactional(readOnly = true)
    public int getRequiredUnits(Long programmeId, Long levelId, String semester) {
        String cleanSemester = semester.trim().toUpperCase();
        return unitLimitRepository
                .findByProgrammeIdAndLevelIdAndSemester(programmeId, levelId, cleanSemester)
                .map(SemesterUnitLimit::getRequiredUnits)
                .orElse(DEFAULT_REQUIRED_UNITS);
    }

    public SemesterUnitLimit setRequiredUnits(
            Long programmeId, Long levelId, String semester, Integer requiredUnits
    ) {
        if (requiredUnits == null || requiredUnits <= 0) {
            throw new IllegalArgumentException("Required units must be a positive number.");
        }

        String cleanSemester = semester.trim().toUpperCase();

        SemesterUnitLimit limit = unitLimitRepository
                .findByProgrammeIdAndLevelIdAndSemester(programmeId, levelId, cleanSemester)
                .orElseGet(() -> {
                    Programme programme = programmeRepository.findById(programmeId)
                            .orElseThrow(() -> new IllegalArgumentException("Programme not found."));
                    Level level = levelRepository.findById(levelId)
                            .orElseThrow(() -> new IllegalArgumentException("Level not found."));
                    SemesterUnitLimit newLimit = new SemesterUnitLimit();
                    newLimit.setProgramme(programme);
                    newLimit.setLevel(level);
                    newLimit.setSemester(cleanSemester);
                    return newLimit;
                });

        limit.setRequiredUnits(requiredUnits);
        return unitLimitRepository.save(limit);
    }

    public void checkAdviserAuthority(String role, Long userProgrammeId, Long targetProgrammeId) {

        if ("ADMIN".equalsIgnoreCase(role) || "HOD".equalsIgnoreCase(role)) {
            return;
        }

        if ("LEVEL_ADVISER".equalsIgnoreCase(role)) {
            if (userProgrammeId == null || !userProgrammeId.equals(targetProgrammeId)) {
                throw new IllegalArgumentException(
                        "You can only set required units for your own programme."
                );
            }
            return;
        }

        throw new IllegalArgumentException(
                "You do not have permission to set required units."
        );
    }
}