package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.DiseaseRelationship;
import com.solomon.epiforecaster.backend.repository.DiseaseRelationshipRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiseaseRelationshipService {

    private final DiseaseRelationshipRepository repository;

    public DiseaseRelationshipService(DiseaseRelationshipRepository repository) {
        this.repository = repository;
    }

    public List<DiseaseRelationship> getAll() {
        return repository.findAll();
    }

    public DiseaseRelationship getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public DiseaseRelationship create(DiseaseRelationship relationship) {
        return repository.save(relationship);
    }

    public DiseaseRelationship update(Long id, DiseaseRelationship relationship) {

        DiseaseRelationship existing = repository.findById(id).orElse(null);

        if (existing == null)
            return null;

        existing.setSourceDisease(relationship.getSourceDisease());
        existing.setTargetDisease(relationship.getTargetDisease());
        existing.setLagWeeks(relationship.getLagWeeks());
        existing.setMethod(relationship.getMethod());
        existing.setStrengthValue(relationship.getStrengthValue());
        existing.setPValue(relationship.getPValue());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}