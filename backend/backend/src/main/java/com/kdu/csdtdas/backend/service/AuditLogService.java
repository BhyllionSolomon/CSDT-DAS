package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AuditLog;
import com.kdu.csdtdas.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public List<AuditLog> getAll() {
        return repository.findAll();
    }

    public AuditLog getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public AuditLog create(AuditLog auditLog) {
        return repository.save(auditLog);
    }

    public AuditLog update(Long id, AuditLog auditLog) {

        AuditLog existing = repository.findById(id).orElse(null);

        if (existing == null)
            return null;

        existing.setUser(auditLog.getUser());
        existing.setAction(auditLog.getAction());
        existing.setEntityType(auditLog.getEntityType());
        existing.setEntityId(auditLog.getEntityId());
        existing.setDescription(auditLog.getDescription());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
