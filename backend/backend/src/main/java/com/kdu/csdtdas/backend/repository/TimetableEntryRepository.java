package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    @Query("""
        SELECT e FROM TimetableEntry e
        JOIN FETCH e.course c
        JOIN FETCH c.level
        JOIN FETCH e.room
        LEFT JOIN FETCH e.lecturer
        WHERE e.window.id = :windowId
        ORDER BY e.day, e.startTime
    """)
    List<TimetableEntry> findForWindow(@Param("windowId") Long windowId);

    @Query("""
        SELECT e FROM TimetableEntry e
        JOIN FETCH e.window w
        JOIN FETCH w.academicSession
        JOIN FETCH e.course c
        JOIN FETCH c.level
        JOIN FETCH e.room
        LEFT JOIN FETCH e.lecturer
        WHERE e.id = :id
    """)
    Optional<TimetableEntry> findByIdWithDetails(@Param("id") Long id);
}