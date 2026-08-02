package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.Lga;
import com.solomon.epiforecaster.backend.repository.LgaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LgaService {

    private final LgaRepository repository;

    public LgaService(LgaRepository repository) {
        this.repository = repository;
    }

    public List<Lga> getAllLgas() {
        return repository.findAll();
    }

    public Lga getLga(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Lga saveLga(Lga lga) {
        return repository.save(lga);
    }

    public void deleteLga(Long id) {
        repository.deleteById(id);
    }
}