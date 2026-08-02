package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.Role;
import com.solomon.epiforecaster.backend.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository repository;

    public RoleService(RoleRepository repository) {
        this.repository = repository;
    }

    public List<Role> getAll() {
        return repository.findAll();
    }

    public Role getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Role create(Role role) {
        return repository.save(role);
    }

    public Role update(Long id, Role role) {

        Role existing = repository.findById(id).orElse(null);

        if (existing == null)
            return null;

        existing.setName(role.getName());
        existing.setDescription(role.getDescription());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}