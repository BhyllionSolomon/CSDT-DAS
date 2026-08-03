package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.RawDataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawDatasetRepository extends JpaRepository<RawDataset, Long> {

    List<RawDataset> findAllByOrderByUploadedAtDesc();

}