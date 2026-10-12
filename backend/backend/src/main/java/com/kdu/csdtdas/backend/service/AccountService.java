package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {

    private static final String NOT_VERIFIED =
            "We could not verify those details. Check your matric number or username and the email you signed up with.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void resetPassword(String identifier, String email, String newPassword) {

        if (identifier == null || identifier.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException(NOT_VERIFIED);
        }
        checkPassword(newPassword);

        String id = identifier.trim();

        User user = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(id)
                        || (u.getStudent() != null && u.getStudent().getMatricNumber().equalsIgnoreCase(id)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(NOT_VERIFIED));

        if (user.getEmail() == null || !user.getEmail().trim().equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException(NOT_VERIFIED);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /** H.O.D sets a temporary password for any account. */
    public void adminResetPassword(Long userId, String newPassword) {
        checkPassword(newPassword);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void checkPassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Choose a password of at least 6 characters.");
        }
    }
}