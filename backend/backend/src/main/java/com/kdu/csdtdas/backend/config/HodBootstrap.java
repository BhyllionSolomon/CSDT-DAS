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

@Component
public class HodBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HodBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String fullName;
    private final String email;

    public HodBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.hod.username:}") String username,
            @Value("${app.bootstrap.hod.password:}") String password,
            @Value("${app.bootstrap.hod.fullname:Head of Department}") String fullName,
            @Value("${app.bootstrap.hod.email:}") String email
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (username.isBlank()) return;

        boolean hodExists = userRepository.findAll().stream()
                .anyMatch(u -> "HOD".equalsIgnoreCase(u.getRole()));
        if (hodExists) return;

        // The H.O.D already signed up (inactive lecturer): just promote that account.
        User existing = userRepository.findByUsername(username.trim()).orElse(null);
        if (existing != null) {
            existing.setRole("HOD");
            existing.setActive(true);
            userRepository.save(existing);
            log.info("H.O.D bootstrap: promoted existing account '{}'.", existing.getUsername());
            return;
        }

        if (password.length() < 8 || email.isBlank()) {
            log.warn("H.O.D bootstrap skipped: set a password of at least 8 characters and an email address.");
            return;
        }

        User hod = new User();
        hod.setUsername(username.trim());
        hod.setPassword(passwordEncoder.encode(password));
        hod.setFullName(fullName);
        hod.setEmail(email.trim());
        hod.setRole("HOD");
        hod.setActive(true);
        userRepository.save(hod);

        log.info("H.O.D bootstrap: created account '{}'.", hod.getUsername());
    }
}