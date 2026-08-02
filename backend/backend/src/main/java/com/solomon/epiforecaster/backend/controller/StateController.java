package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.State;
import com.solomon.epiforecaster.backend.service.StateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/states")
@CrossOrigin(origins = "*")
public class StateController {

    private final StateService service;

    public StateController(StateService service) {
        this.service = service;
    }

    @GetMapping
    public List<State> getAllStates() {
        return service.getAllStates();
    }

    @GetMapping("/{id}")
    public State getState(@PathVariable Long id) {
        return service.getState(id);
    }

    @PostMapping
    public State saveState(@RequestBody State state) {
        return service.saveState(state);
    }

    @DeleteMapping("/{id}")
    public void deleteState(@PathVariable Long id) {
        service.deleteState(id);
    }
}