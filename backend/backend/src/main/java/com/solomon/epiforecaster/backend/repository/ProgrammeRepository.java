
        package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProgrammeRepository extends JpaRepository<Programme, Long> {

    Optional<Programme> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
            SELECT p
            FROM Programme p
            JOIN FETCH p.department
            """)
    List<Programme> findAllWithDepartment();

    @Query("""
            SELECT p
            FROM Programme p
            JOIN FETCH p.department
            WHERE p.id = :id
            """)
    Optional<Programme> findByIdWithDepartment(Long id);
}

