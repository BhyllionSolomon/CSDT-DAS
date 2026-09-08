package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LevelRepository extends JpaRepository<Level, Long> {
}