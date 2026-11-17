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

    /**
     * CGPA uploaded through the student roster for the selected allocation
     * session. Match by matric number and session: the roster's programme/level
     * selection can be newer than the student's previously stored details.
     */
    @Query("""
        SELECT r FROM ReportedCgpa r
        WHERE UPPER(TRIM(r.matricNumber)) = UPPER(TRIM(:matricNumber))
          AND r.academicSession.id = :sessionId
        ORDER BY r.createdAt DESC, r.id DESC
    """)
    List<ReportedCgpa> findLatestForAllocation(
            @Param("matricNumber") String matricNumber,
            @Param("sessionId") Long sessionId
    );
    /**
     * Latest uploaded cumulative CGPA for a student, regardless of the session
     * selected when the roster was imported. CGPA is cumulative; a roster
     * attached to the immediately preceding session must remain usable when
     * the student is promoted into the next session.
     */
    @Query("""
        SELECT r FROM ReportedCgpa r
        WHERE UPPER(TRIM(r.matricNumber)) = UPPER(TRIM(:matricNumber))
        ORDER BY r.createdAt DESC, r.id DESC
    """)
    List<ReportedCgpa> findLatestForAllocationAnySession(
            @Param("matricNumber") String matricNumber
    );

}
