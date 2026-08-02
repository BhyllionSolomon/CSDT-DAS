package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiseaseRecordRepository extends JpaRepository<DiseaseRecord, Long> {

}