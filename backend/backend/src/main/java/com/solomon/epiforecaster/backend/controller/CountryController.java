package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.entity.Country;
import com.solomon.epiforecaster.backend.service.CountryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final CountryService service;

    public CountryController(CountryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Country> getAllCountries() {
        return service.getAllCountries();
    }

    @GetMapping("/{id}")
    public Country getCountry(@PathVariable Long id) {
        return service.getCountryById(id);
    }

    @PostMapping
    public Country createCountry(@RequestBody Country country) {
        return service.saveCountry(country);
    }

    @PutMapping("/{id}")
    public Country updateCountry(@PathVariable Long id,
                                 @RequestBody Country country) {
        return service.updateCountry(id, country);
    }

    @DeleteMapping("/{id}")
    public void deleteCountry(@PathVariable Long id) {
        service.deleteCountry(id);
    }
}