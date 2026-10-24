package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    List<SecurityEvent> findTop200ByOrderByOccurredAtDesc();
}