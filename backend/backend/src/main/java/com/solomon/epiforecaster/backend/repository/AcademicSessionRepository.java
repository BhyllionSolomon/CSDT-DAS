package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {
}