package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.SemesterUnitLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SemesterUnitLimitRepository extends JpaRepository<SemesterUnitLimit, Long> {
    Optional<SemesterUnitLimit> findByProgrammeIdAndLevelIdAndSemester(
            Long programmeId, Long levelId, String semester
    );
}