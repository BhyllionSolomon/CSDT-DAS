package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    boolean existsByAnnouncementIdAndUserId(Long announcementId, Long userId);

    @Query("SELECT r.announcement.id FROM AnnouncementRead r WHERE r.user.id = :userId")
    List<Long> readIds(@Param("userId") Long userId);
}