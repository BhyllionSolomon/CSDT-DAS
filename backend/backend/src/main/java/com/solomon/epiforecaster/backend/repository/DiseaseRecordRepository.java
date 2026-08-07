package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.entity.RawDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DiseaseRecordRepository
        extends JpaRepository<DiseaseRecord, Long> {

    /*
     * Dashboard Statistics
     */

    @Query("""
            SELECT COUNT(DISTINCT d.diseaseName)
            FROM DiseaseRecord d
            """)
    Long countDistinctDiseases();

    @Query("""
            SELECT COUNT(DISTINCT d.state)
            FROM DiseaseRecord d
            """)
    Long countDistinctStates();

    @Query("""
            SELECT COUNT(DISTINCT d.lga)
            FROM DiseaseRecord d
            """)
    Long countDistinctLgas();

    /*
     * Dashboard Charts
     */

    @Query("""
            SELECT d.diseaseName, COUNT(d)
            FROM DiseaseRecord d
            GROUP BY d.diseaseName
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
     * Search
     */

    List<DiseaseRecord> findByDiseaseName(String diseaseName);

    List<DiseaseRecord> findByState(String state);

    List<DiseaseRecord> findByLga(String lga);

    List<DiseaseRecord> findByDiseaseNameAndState(
            String diseaseName,
            String state
    );

    List<DiseaseRecord> findByDiseaseNameAndStateAndLga(
            String diseaseName,
            String state,
            String lga
    );

    /*
     * Curated Dataset Generator
     */

    List<DiseaseRecord> findByDataset(RawDataset dataset);

    /*
     * Dashboard Drop-down
     */

    @Query("""
            SELECT DISTINCT d.diseaseName
            FROM DiseaseRecord d
            ORDER BY d.diseaseName
            """)
    List<String> findDistinctDiseases();

    /*
     * Duplicate Detection
     */

    boolean existsByDiseaseNameAndCountryAndStateAndLgaAndYearAndEpiWeek(
            String diseaseName,
            String country,
            String state,
            String lga,
            Integer year,
            Integer epiWeek
    );
}