package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.DirectEntryProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DirectEntryProfileRepository extends JpaRepository<DirectEntryProfile, Long> {

    Optional<DirectEntryProfile> findByStudentId(Long studentId);

    @Query("""
        SELECT d FROM DirectEntryProfile d
        JOIN FETCH d.student s
        JOIN FETCH s.programme
        JOIN FETCH s.level
        ORDER BY s.fullName
    """)
    List<DirectEntryProfile> findAllWithStudent();
}