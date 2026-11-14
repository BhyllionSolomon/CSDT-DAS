package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ExtraUnitGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExtraUnitGrantRepository extends JpaRepository<ExtraUnitGrant, Long> {

    Optional<ExtraUnitGrant> findByStudentIdAndAcademicSessionIdAndSemester(Long studentId, Long sessionId, String semester);

    @Query("""
        SELECT g FROM ExtraUnitGrant g
        JOIN FETCH g.student s
        JOIN FETCH s.programme
        JOIN FETCH s.level
        JOIN FETCH g.academicSession a
        WHERE a.id = :sessionId
        ORDER BY s.fullName
    """)
    List<ExtraUnitGrant> findForSession(@Param("sessionId") Long sessionId);
}