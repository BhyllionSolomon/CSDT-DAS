package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.OnlineClassJoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OnlineClassJoinRepository extends JpaRepository<OnlineClassJoin, Long> {

    Optional<OnlineClassJoin> findByOnlineClassIdAndUserId(Long onlineClassId, Long userId);

    long countByOnlineClassId(Long onlineClassId);

    @Query("""
        SELECT j FROM OnlineClassJoin j
        JOIN FETCH j.user u
        LEFT JOIN FETCH u.student st
        WHERE j.onlineClass.id = :onlineClassId
        ORDER BY j.joinedAt
    """)
    List<OnlineClassJoin> findByOnlineClassIdWithUsers(@Param("onlineClassId") Long onlineClassId);
}