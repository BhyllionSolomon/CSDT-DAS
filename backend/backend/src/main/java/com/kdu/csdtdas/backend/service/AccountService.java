package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** The H.O.D sets a temporary password for another account. */
    public void adminResetPassword(Long userId, String newPassword) {

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Choose a password of at least 8 characters.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}