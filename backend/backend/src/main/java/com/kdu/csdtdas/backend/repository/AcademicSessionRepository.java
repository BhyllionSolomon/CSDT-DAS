package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {
}
