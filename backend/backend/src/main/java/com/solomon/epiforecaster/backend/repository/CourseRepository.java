package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);
}