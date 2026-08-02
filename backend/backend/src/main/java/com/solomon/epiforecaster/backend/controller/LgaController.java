package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.Lga;
import com.solomon.epiforecaster.backend.service.LgaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lgas")
@CrossOrigin(origins = "*")
public class LgaController {

    private final LgaService service;

    public LgaController(LgaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Lga> getAllLgas() {
        return service.getAllLgas();
    }

    @GetMapping("/{id}")
    public Lga getLga(@PathVariable Long id) {
        return service.getLga(id);
    }

    @PostMapping
    public Lga saveLga(@RequestBody Lga lga) {
        return service.saveLga(lga);
    }

    @DeleteMapping("/{id}")
    public void deleteLga(@PathVariable Long id) {
        service.deleteLga(id);
    }
}