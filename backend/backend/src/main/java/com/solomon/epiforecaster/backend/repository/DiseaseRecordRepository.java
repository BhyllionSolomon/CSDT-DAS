package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DiseaseRecordRepository
        extends JpaRepository<DiseaseRecord, Long> {

    /*
     * Existing Dashboard Statistics
     */

    @Query("SELECT COUNT(DISTINCT d.disease) FROM DiseaseRecord d")
    Long countDistinctDiseases();

    @Query("SELECT COUNT(DISTINCT d.state) FROM DiseaseRecord d")
    Long countDistinctStates();

    @Query("SELECT COUNT(DISTINCT d.lga) FROM DiseaseRecord d")
    Long countDistinctLgas();

    /*
     * Dashboard Charts
     */

    @Query("""
            SELECT d.disease, COUNT(d)
            FROM DiseaseRecord d
            GROUP BY d.disease
            ORDER BY COUNT(d) DESC
            """)
    List<Object[]> getDiseaseDistribution();

    @Query("""
            SELECT d.state, COUNT(d)
            FROM DiseaseRecord d
            GROUP BY d.state
            ORDER BY COUNT(d) DESC
            """)
    List<Object[]> getStateDistribution();

    @Query("""
            SELECT d.year,
                   d.epiWeek,
                   SUM(d.confirmedCases)
            FROM DiseaseRecord d
            GROUP BY d.year, d.epiWeek
            ORDER BY d.year, d.epiWeek
            """)
    List<Object[]> getWeeklyTrend();

    /*
     * Disease Queries
     */

    List<DiseaseRecord> findByDisease(String disease);

    List<DiseaseRecord> findByState(String state);

    List<DiseaseRecord> findByLga(String lga);

    List<DiseaseRecord> findByDiseaseAndState(
            String disease,
            String state
    );

    List<DiseaseRecord> findByDiseaseAndStateAndLga(
            String disease,
            String state,
            String lga
    );

}