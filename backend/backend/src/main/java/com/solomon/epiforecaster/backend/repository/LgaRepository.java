package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.Lga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LgaRepository extends JpaRepository<Lga, Long> {

    boolean existsByNameIgnoreCase(String name);

}