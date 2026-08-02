package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.ForecastModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForecastModelRepository extends JpaRepository<ForecastModel, Long> {
}