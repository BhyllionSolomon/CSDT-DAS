package com.kdu.csdtdas.backend.config;

import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AccountBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountBootstrap.class);

    private record Seed(String role, String username, String password, String fullName, String email) {}

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final List<Seed> seeds;

    public AccountBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.username:}") String adminUsername,
            @Value("${app.bootstrap.admin.password:}") String adminPassword,
            @Value("${app.bootstrap.admin.fullname:}") String adminFullName,
            @Value("${app.bootstrap.admin.email:}") String adminEmail,
            @Value("${app.bootstrap.hod.username:}") String hodUsername,
            @Value("${app.bootstrap.hod.password:}") String hodPassword,
            @Value("${app.bootstrap.hod.fullname:}") String hodFullName,
            @Value("${app.bootstrap.hod.email:}") String hodEmail
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seeds = List.of(
                new Seed("ADMIN", adminUsername.trim(), adminPassword,
                        or(adminFullName, "System Administrator"), adminEmail.trim()),
                new Seed("HOD", hodUsername.trim(), hodPassword,
                        or(hodFullName, "Head of Department"), hodEmail.trim())
        );
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Seed seed : seeds) {
            try {
                ensure(seed);
            } catch (RuntimeException e) {
                log.error("Account bootstrap for {} failed: {}", seed.role(), e.getMessage());
            }
        }
    }

    private void ensure(Seed seed) {

        // Not configured: nothing to do.
        if (seed.username().isBlank()) return;

        List<User> users = userRepository.findAll();

        // This role already has an account: never touch it.
        if (users.stream().anyMatch(u -> seed.role().equalsIgnoreCase(u.getRole()))) return;

        User sameUsername = users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(seed.username()))
                .findFirst()
                .orElse(null);

        // The person already signed up: promote that account, but only if the email matches.
        if (sameUsername != null) {
            boolean sameEmail = !seed.email().isBlank()
                    && sameUsername.getEmail() != null
                    && sameUsername.getEmail().trim().equalsIgnoreCase(seed.email());

            if (!sameEmail) {
                log.warn("Account bootstrap: username '{}' already belongs to a different account, so no {} account "
                        + "was created. Choose another username in the settings.", seed.username(), seed.role());
                return;
            }

            sameUsername.setRole(seed.role());
            sameUsername.setActive(true);
            userRepository.save(sameUsername);
            log.info("Account bootstrap: promoted existing account '{}' to {}.", seed.username(), seed.role());
            return;
        }

        if (seed.password().length() < 10) {
            log.warn("Account bootstrap: the {} password must be at least 10 characters. No account was created.",
                    seed.role());
            return;
        }
        if (seed.email().isBlank()) {
            log.warn("Account bootstrap: the {} email is missing. No account was created.", seed.role());
            return;
        }
        if (users.stream().anyMatch(u -> u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(seed.email()))) {
            log.warn("Account bootstrap: that {} email already belongs to another account. No account was created.",
                    seed.role());
            return;
        }

        User user = new User();
        user.setUsername(seed.username());
        user.setPassword(passwordEncoder.encode(seed.password()));
        user.setFullName(seed.fullName());
        user.setEmail(seed.email());
        user.setRole(seed.role());
        user.setActive(true);
        userRepository.save(user);

        log.info("Account bootstrap: created {} account '{}'.", seed.role(), seed.username());
    }

    private static String or(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}