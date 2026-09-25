package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ReportedCgpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportedCgpaRepository extends JpaRepository<ReportedCgpa, Long> {

    @Query("""
        SELECT r FROM ReportedCgpa r
        WHERE r.programme.id = :programmeId
          AND r.level.id = :levelId
          AND r.academicSession.id = :sessionId
        ORDER BY r.matricNumber
    """)
    List<ReportedCgpa> findByProgrammeAndLevelAndSession(
            @Param("programmeId") Long programmeId,
            @Param("levelId") Long levelId,
            @Param("sessionId") Long sessionId
    );
}