package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.CuratedDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CuratedDatasetRepository
        extends JpaRepository<CuratedDataset, Long> {

    List<CuratedDataset> findByDiseaseName(String diseaseName);

    List<CuratedDataset> findByState(String state);

    List<CuratedDataset> findByLga(String lga);

    List<CuratedDataset> findByDiseaseNameAndState(
            String diseaseName,
            String state
    );

    List<CuratedDataset> findByDiseaseNameAndStateAndLga(
            String diseaseName,
            String state,
            String lga
    );

    @Query("""
            SELECT DISTINCT c.diseaseName
            FROM CuratedDataset c
            ORDER BY c.diseaseName
            """)
    List<String> findDistinctDiseases();

    @Query("""
            SELECT DISTINCT c.state
            FROM CuratedDataset c
            ORDER BY c.state
            """)
    List<String> findDistinctStates();

    @Query("""
            SELECT DISTINCT c.lga
            FROM CuratedDataset c
            ORDER BY c.lga
            """)
    List<String> findDistinctLgas();
}