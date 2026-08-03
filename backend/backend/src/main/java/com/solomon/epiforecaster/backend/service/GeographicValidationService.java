package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.repository.CountryRepository;
import com.solomon.epiforecaster.backend.repository.StateRepository;
import com.solomon.epiforecaster.backend.repository.LgaRepository;
import org.springframework.stereotype.Service;

@Service
public class GeographicValidationService {

    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final LgaRepository lgaRepository;

    public GeographicValidationService(
            CountryRepository countryRepository,
            StateRepository stateRepository,
            LgaRepository lgaRepository) {

        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.lgaRepository = lgaRepository;
    }

    public boolean isValidCountry(String country) {

        if (country == null || country.isBlank()) {
            return false;
        }

        return countryRepository.existsByNameIgnoreCase(country.trim());

    }

    public boolean isValidState(String state) {

        if (state == null || state.isBlank()) {
            return false;
        }

        return stateRepository.existsByNameIgnoreCase(state.trim());

    }

    public boolean isValidLga(String lga) {

        if (lga == null || lga.isBlank()) {
            return false;
        }

        return lgaRepository.existsByNameIgnoreCase(lga.trim());

    }

}