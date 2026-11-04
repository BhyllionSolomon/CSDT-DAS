package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectSupervision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectSupervisionRepository extends JpaRepository<ProjectSupervision, Long> {

    @Query("""
        SELECT p FROM ProjectSupervision p
        JOIN FETCH p.student s
        JOIN FETCH s.programme
        JOIN FETCH p.supervisor sup
        JOIN FETCH p.academicSession a
        WHERE a.id = :sessionId
        ORDER BY sup.fullName, s.fullName
    """)
    List<ProjectSupervision> findForSession(@Param("sessionId") Long sessionId);

    @Query("""
        SELECT p FROM ProjectSupervision p
        JOIN FETCH p.student s
        JOIN FETCH p.supervisor sup
        JOIN FETCH p.academicSession a
        WHERE s.id = :studentId
        ORDER BY p.assignedAt DESC
    """)
    List<ProjectSupervision> findForStudent(@Param("studentId") Long studentId);
}