package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    @Query("""
        SELECT v FROM VerificationCode v
        WHERE v.user.id = :userId AND v.purpose = :purpose AND v.consumed = false
        ORDER BY v.createdAt DESC
    """)
    List<VerificationCode> findOpen(@Param("userId") Long userId, @Param("purpose") String purpose);

    @Query("""
        SELECT COUNT(v) FROM VerificationCode v
        WHERE v.user.id = :userId AND v.purpose = :purpose AND v.createdAt > :since
    """)
    long countRecent(
            @Param("userId") Long userId,
            @Param("purpose") String purpose,
            @Param("since") LocalDateTime since
    );

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expiresAt < :time")
    void deleteExpired(@Param("time") LocalDateTime time);
}