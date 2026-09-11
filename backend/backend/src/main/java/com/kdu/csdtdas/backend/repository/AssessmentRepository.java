package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    Optional<Assessment> findByCourseRegistrationId(Long courseRegistrationId);

    boolean existsByCourseRegistrationId(Long courseRegistrationId);
}
