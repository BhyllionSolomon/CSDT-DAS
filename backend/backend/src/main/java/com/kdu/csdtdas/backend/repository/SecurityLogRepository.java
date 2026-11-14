package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read-only view of the security events, for the Security Log page. */
public interface SecurityLogRepository extends JpaRepository<SecurityEvent, Long> {
}