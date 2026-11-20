package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("""
        SELECT a FROM Announcement a
        JOIN FETCH a.postedBy
        LEFT JOIN FETCH a.programme
        LEFT JOIN FETCH a.course
        WHERE a.active = true
        ORDER BY a.pinned DESC, a.createdAt DESC
    """)
    List<Announcement> findActiveWithDetails();
}