package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.StudentLevelHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentLevelHistoryRepository extends JpaRepository<StudentLevelHistory, Long> {

    @Query("""
        SELECT h FROM StudentLevelHistory h
        JOIN FETCH h.student s
        JOIN FETCH h.level l
        JOIN FETCH h.programme p
        WHERE h.academicSession.id = :sessionId
        ORDER BY p.code, l.levelNumber, s.matricNumber
    """)
    List<StudentLevelHistory> findByAcademicSessionId(@Param("sessionId") Long sessionId);

    Optional<StudentLevelHistory> findByStudentIdAndAcademicSessionId(Long studentId, Long academicSessionId);
}