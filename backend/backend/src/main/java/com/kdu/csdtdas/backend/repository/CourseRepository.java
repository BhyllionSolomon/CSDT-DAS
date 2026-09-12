package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
        SELECT DISTINCT c
        FROM Course c
        JOIN FETCH c.department d
        JOIN FETCH c.level l
        LEFT JOIN FETCH c.programmes p
        WHERE c.id = :id
    """)
    Optional<Course> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT c
        FROM Course c
        JOIN FETCH c.department d
        JOIN FETCH c.level l
        LEFT JOIN FETCH c.programmes p
        ORDER BY c.code
    """)
    List<Course> findAllWithDetails();

    @Query("""
        SELECT DISTINCT c
        FROM Course c
        JOIN FETCH c.department d
        JOIN FETCH c.level l
        LEFT JOIN FETCH c.programmes p
        JOIN c.programmes pf
        WHERE pf.id = :programmeId
        ORDER BY c.level.id, c.code
    """)
    List<Course> findByProgrammeId(@Param("programmeId") Long programmeId);
}