package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByMatricNumber(String matricNumber);

    boolean existsByMatricNumber(String matricNumber);

    @Query("""
        SELECT s
        FROM Student s
        JOIN FETCH s.department d
        JOIN FETCH s.programme p
        JOIN FETCH s.level l
        JOIN FETCH s.academicSession a
        LEFT JOIN FETCH s.admissionSession ads
        WHERE s.id = :id
    """)
    Optional<Student> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT s
        FROM Student s
        JOIN FETCH s.department d
        JOIN FETCH s.programme p
        JOIN FETCH s.level l
        JOIN FETCH s.academicSession a
        LEFT JOIN FETCH s.admissionSession ads
        ORDER BY s.matricNumber
    """)
    List<Student> findAllWithDetails();
}