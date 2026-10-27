package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.TimetableWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimetableWindowRepository extends JpaRepository<TimetableWindow, Long> {

    Optional<TimetableWindow> findByAcademicSessionIdAndSemester(Long sessionId, String semester);

    List<TimetableWindow> findByStatus(String status);
}