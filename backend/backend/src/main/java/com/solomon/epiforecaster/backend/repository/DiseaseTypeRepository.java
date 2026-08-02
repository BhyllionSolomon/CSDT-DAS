package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.DiseaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiseaseTypeRepository extends JpaRepository<DiseaseType, Long> {

}