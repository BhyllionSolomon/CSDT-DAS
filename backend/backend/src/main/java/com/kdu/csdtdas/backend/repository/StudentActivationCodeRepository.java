package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.StudentActivationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentActivationCodeRepository extends JpaRepository<StudentActivationCode, Long> {

    Optional<StudentActivationCode> findByStudentId(Long studentId);
}