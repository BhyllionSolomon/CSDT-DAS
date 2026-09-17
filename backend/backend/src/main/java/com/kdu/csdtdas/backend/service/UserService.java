package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import com.kdu.csdtdas.backend.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ProgrammeRepository programmeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(
            UserRepository userRepository,
            ProgrammeRepository programmeRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.programmeRepository = programmeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly = true)
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public User create(
            String username,
            String password,
            String fullName,
            String email,
            String role,
            Long programmeId
    ) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw new IllegalArgumentException("Username already exists.");
        }

        String cleanRole = role.trim().toUpperCase();

        if ("LEVEL_ADVISER".equals(cleanRole) && programmeId == null) {
            throw new IllegalArgumentException(
                    "A Level Adviser must be assigned to a programme."
            );
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(cleanRole);
        user.setActive(true);

        if (programmeId != null) {
            Programme programme = programmeRepository.findById(programmeId)
                    .orElseThrow(() -> new IllegalArgumentException("Programme not found."));
            user.setProgramme(programme);
        }

        return userRepository.save(user);
    }

    public User update(
            Long id,
            String fullName,
            String email,
            String role,
            Boolean active,
            Long programmeId
    ) {

        User existing = getById(id);

        if (fullName != null) existing.setFullName(fullName);
        if (email != null) existing.setEmail(email);
        if (role != null) existing.setRole(role.trim().toUpperCase());
        if (active != null) existing.setActive(active);

        if (programmeId != null) {
            Programme programme = programmeRepository.findById(programmeId)
                    .orElseThrow(() -> new IllegalArgumentException("Programme not found."));
            existing.setProgramme(programme);
        }

        return userRepository.save(existing);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public String authenticate(String username, String password) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("This account is inactive.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        return jwtUtil.generateToken(user.getUsername(), user.getRole());
    }
}