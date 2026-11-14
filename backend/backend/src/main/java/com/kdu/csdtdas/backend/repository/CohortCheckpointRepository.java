package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.CohortCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CohortCheckpointRepository extends JpaRepository<CohortCheckpoint, Long> {

    List<CohortCheckpoint> findByIntakeYear(int intakeYear);

    Optional<CohortCheckpoint> findByIntakeYearAndSessionYearAndKind(int intakeYear, int sessionYear, String kind);
}