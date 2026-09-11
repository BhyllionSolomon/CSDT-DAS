package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.RolePermission;
import com.kdu.csdtdas.backend.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolePermissionService {

    private final RolePermissionRepository repository;

    public RolePermissionService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    public List<RolePermission> getAll() {
        return repository.findAll();
    }

    public RolePermission getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public RolePermission create(RolePermission rolePermission) {
        return repository.save(rolePermission);
    }

    public RolePermission update(Long id, RolePermission rolePermission) {

        RolePermission existing = repository.findById(id).orElse(null);

        if (existing == null)
            return null;

        existing.setRole(rolePermission.getRole());
        existing.setPermission(rolePermission.getPermission());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
