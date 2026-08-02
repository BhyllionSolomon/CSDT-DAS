package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.DiseaseMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiseaseMasterRepository
        extends JpaRepository<DiseaseMaster, Long> {

    Optional<DiseaseMaster> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

}