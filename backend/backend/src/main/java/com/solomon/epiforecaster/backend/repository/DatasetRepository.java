package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {

}