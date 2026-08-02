package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.ValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationResultRepository extends JpaRepository<ValidationResult, Long> {

}