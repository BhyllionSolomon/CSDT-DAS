package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.RawDataset;
import com.solomon.epiforecaster.backend.service.RawDatasetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasets")
@CrossOrigin(origins = "*")
public class RawDatasetController {

    private final RawDatasetService service;

    public RawDatasetController(RawDatasetService service) {
        this.service = service;
    }

    @GetMapping
    public List<RawDataset> getAllDatasets() {

        return service.getAllDatasets();

    }

    @GetMapping("/{id}")
    public RawDataset getDataset(@PathVariable Long id) {

        return service.getDataset(id);

    }

}