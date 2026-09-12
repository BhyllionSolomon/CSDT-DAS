package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
        SELECT c
        FROM Course c
        JOIN FETCH c.department d
        JOIN FETCH c.level l
        WHERE c.id = :id
    """)
    Optional<Course> findByIdWithDetails(Long id);

    @Query("""
        SELECT c
        FROM Course c
        JOIN FETCH c.department d
        JOIN FETCH c.level l
        ORDER BY c.code
    """)
    List<Course> findAllWithDetails();
}