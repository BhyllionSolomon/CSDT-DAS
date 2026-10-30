package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.SecurityEvent;
import com.kdu.csdtdas.backend.repository.SecurityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    private final SecurityEventRepository repository;

    public SecurityAuditService(SecurityEventRepository repository) {
        this.repository = repository;
    }

    /** Saved in its own transaction so the record survives even when the request is refused. Never throws. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, String role, String ip, String action, String detail) {
        try {
            SecurityEvent e = new SecurityEvent();
            e.setUsername(username);
            e.setRole(role);
            e.setIp(ip);
            e.setAction(action);
            e.setDetail(detail != null && detail.length() > 300 ? detail.substring(0, 300) : detail);
            repository.save(e);
        } catch (RuntimeException ex) {
            log.warn("Could not save a security event: {}", ex.getMessage());
        }
        log.warn("SECURITY {} user={} role={} ip={} {}", action, username, role, ip, detail);
    }
}