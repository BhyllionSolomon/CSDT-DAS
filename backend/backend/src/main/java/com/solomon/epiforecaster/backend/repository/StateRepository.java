package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {

}