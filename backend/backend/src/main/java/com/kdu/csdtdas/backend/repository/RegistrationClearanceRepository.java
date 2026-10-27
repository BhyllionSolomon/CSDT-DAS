package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.RegistrationClearance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationClearanceRepository extends JpaRepository<RegistrationClearance, Long> {

    Optional<RegistrationClearance> findByStudentIdAndAcademicSessionId(Long studentId, Long sessionId);

    boolean existsByRegistrationCode(String registrationCode);

    List<RegistrationClearance> findByAcademicSessionIdAndInvoiceKey(Long sessionId, String invoiceKey);

    @Query("""
        SELECT c FROM RegistrationClearance c
        JOIN FETCH c.student s
        JOIN FETCH s.programme
        JOIN FETCH s.level
        JOIN FETCH c.academicSession a
        WHERE a.id = :sessionId
        ORDER BY c.submittedAt DESC
    """)
    List<RegistrationClearance> findForSession(@Param("sessionId") Long sessionId);

    @Query("""
        SELECT c FROM RegistrationClearance c
        JOIN FETCH c.student s
        JOIN FETCH s.programme
        JOIN FETCH s.level
        JOIN FETCH c.academicSession a
        WHERE c.id = :id
    """)
    Optional<RegistrationClearance> findByIdWithDetails(@Param("id") Long id);
}