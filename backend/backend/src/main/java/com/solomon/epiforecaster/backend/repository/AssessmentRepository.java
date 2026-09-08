package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    Optional<Assessment> findByCourseRegistrationId(Long courseRegistrationId);

    boolean existsByCourseRegistrationId(Long courseRegistrationId);
}