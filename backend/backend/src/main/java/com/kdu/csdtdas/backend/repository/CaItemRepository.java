package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.CaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaItemRepository extends JpaRepository<CaItem, Long> {

    List<CaItem> findByCourseIdAndAcademicSessionIdAndSemesterOrderByIdAsc(Long courseId, Long sessionId, String semester);
}