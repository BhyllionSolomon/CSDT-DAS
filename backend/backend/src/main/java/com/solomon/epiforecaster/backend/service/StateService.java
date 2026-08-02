package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.State;
import com.solomon.epiforecaster.backend.repository.StateRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StateService {

    private final StateRepository repository;

    public StateService(StateRepository repository) {
        this.repository = repository;
    }

    public List<State> getAllStates() {
        return repository.findAll();
    }

    public State getState(Long id) {
        return repository.findById(id).orElse(null);
    }

    public State saveState(State state) {
        return repository.save(state);
    }

    public void deleteState(Long id) {
        repository.deleteById(id);
    }
}