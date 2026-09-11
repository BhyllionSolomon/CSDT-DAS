package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByMatricNumber(String matricNumber);

    boolean existsByMatricNumber(String matricNumber);
}
