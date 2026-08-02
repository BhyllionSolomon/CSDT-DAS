package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.Country;
import com.solomon.epiforecaster.backend.repository.CountryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CountryService {

    private final CountryRepository repository;

    public CountryService(CountryRepository repository) {
        this.repository = repository;
    }

    public List<Country> getAllCountries() {
        return repository.findAll();
    }

    public Country getCountryById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Country saveCountry(Country country) {
        return repository.save(country);
    }

    public Country updateCountry(Long id, Country country) {

        Country existing = repository.findById(id).orElse(null);

        if (existing == null) {
            return null;
        }

        existing.setName(country.getName());
        existing.setIsoCode(country.getIsoCode());

        return repository.save(existing);
    }

    public void deleteCountry(Long id) {
        repository.deleteById(id);
    }
}