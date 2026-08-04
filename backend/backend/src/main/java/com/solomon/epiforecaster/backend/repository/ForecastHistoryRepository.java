package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.ForecastHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForecastHistoryRepository
        extends JpaRepository<ForecastHistory, Long> {

    List<ForecastHistory> findByDisease(String disease);

    List<ForecastHistory> findByState(String state);

    List<ForecastHistory> findByLga(String lga);

}