package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.programme
        LEFT JOIN FETCH u.level
        LEFT JOIN FETCH u.student st
        LEFT JOIN FETCH st.level
        LEFT JOIN FETCH st.programme
        WHERE u.username = :username
    """)
    Optional<User> findByUsername(@Param("username") String username);

    boolean existsByUsername(String username);

    @Override
    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.programme
        LEFT JOIN FETCH u.level
        LEFT JOIN FETCH u.student st
        LEFT JOIN FETCH st.level
        LEFT JOIN FETCH st.programme
        ORDER BY u.fullName
    """)
    List<User> findAll();

    @Override
    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.programme
        LEFT JOIN FETCH u.level
        LEFT JOIN FETCH u.student st
        LEFT JOIN FETCH st.level
        LEFT JOIN FETCH st.programme
        WHERE u.id = :id
    """)
    Optional<User> findById(@Param("id") Long id);
}