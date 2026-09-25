package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("""
        SELECT u
        FROM User u
        LEFT JOIN FETCH u.programme p
        LEFT JOIN FETCH u.student s
        ORDER BY u.fullName
    """)
    List<User> findAllWithDetails();

    @Query("""
        SELECT u
        FROM User u
        LEFT JOIN FETCH u.programme p
        LEFT JOIN FETCH u.student s
        WHERE u.id = :id
    """)
    Optional<User> findByIdWithDetails(@org.springframework.data.repository.query.Param("id") Long id);
}