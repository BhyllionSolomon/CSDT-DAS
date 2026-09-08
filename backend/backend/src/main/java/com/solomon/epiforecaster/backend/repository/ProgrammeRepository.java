package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, Long> {
}