package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}