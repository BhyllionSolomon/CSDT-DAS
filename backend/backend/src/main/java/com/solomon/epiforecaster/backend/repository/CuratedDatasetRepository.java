package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.CuratedDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CuratedDatasetRepository
        extends JpaRepository<CuratedDataset, Long> {
}