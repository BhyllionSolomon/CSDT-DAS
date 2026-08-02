package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
}