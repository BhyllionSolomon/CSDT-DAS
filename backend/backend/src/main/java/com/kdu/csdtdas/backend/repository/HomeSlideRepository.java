package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.HomeSlide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeSlideRepository extends JpaRepository<HomeSlide, Long> {

    List<HomeSlide> findAllByOrderBySortOrderAscIdAsc();
}