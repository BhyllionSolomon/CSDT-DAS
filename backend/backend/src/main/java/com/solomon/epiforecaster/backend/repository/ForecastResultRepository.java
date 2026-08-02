package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.ForecastResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForecastResultRepository extends JpaRepository<ForecastResult, Long> {
}