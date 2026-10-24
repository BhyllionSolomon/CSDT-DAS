package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.TimetableRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimetableRoomRepository extends JpaRepository<TimetableRoom, Long> {

    Optional<TimetableRoom> findByNameIgnoreCase(String name);

    List<TimetableRoom> findAllByOrderByNameAsc();
}