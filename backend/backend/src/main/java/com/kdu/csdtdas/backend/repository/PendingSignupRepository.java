package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.PendingSignup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PendingSignupRepository extends JpaRepository<PendingSignup, Long> {

    Optional<PendingSignup> findByStudentId(Long studentId);

    boolean existsByUsernameAndStudentIdNot(String username, Long studentId);

    @Modifying
    @Query("DELETE FROM PendingSignup p WHERE p.expiresAt < :time")
    void deleteExpired(@Param("time") LocalDateTime time);
}