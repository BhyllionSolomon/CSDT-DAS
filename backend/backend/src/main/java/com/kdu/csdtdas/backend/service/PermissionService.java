package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.Permission;
import com.kdu.csdtdas.backend.repository.PermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository repository;

    public PermissionService(PermissionRepository repository) {
        this.repository = repository;
    }

    public List<Permission> getAll() {
        return repository.findAll();
    }

    public Permission getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Permission create(Permission permission) {
        return repository.save(permission);
    }

    public Permission update(Long id, Permission permission) {

        Permission existing = repository.findById(id).orElse(null);

        if (existing == null)
            return null;

        existing.setName(permission.getName());
        existing.setDescription(permission.getDescription());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
