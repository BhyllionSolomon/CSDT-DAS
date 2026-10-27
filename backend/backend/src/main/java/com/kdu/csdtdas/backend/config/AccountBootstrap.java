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
                new Seed("ADMIN", clean(adminUsername), safe(adminPassword),
                        or(adminFullName, "System Administrator"), clean(adminEmail)),
                new Seed("HOD", clean(hodUsername), safe(hodPassword),
                        or(hodFullName, "Head of Department"), clean(hodEmail))
        );
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("ACCOUNT_BOOTSTRAP_START: checking configured initial ADMIN and HOD accounts.");
        for (Seed seed : seeds) {
            try {
                ensure(seed);
            } catch (RuntimeException e) {
                log.error("ACCOUNT_BOOTSTRAP_FAILED: role={}, reason={}", seed.role(), e.getMessage(), e);
            }
        }
        log.info("ACCOUNT_BOOTSTRAP_FINISHED.");
    }

    private void ensure(Seed seed) {
        if (seed.username().isBlank()) {
            log.error("ACCOUNT_BOOTSTRAP_SKIPPED: role={} because its username setting is blank. "
                    + "Check APP_BOOTSTRAP_{}_USERNAME in Render.", seed.role(), seed.role());
            return;
        }

        if (seed.password().isBlank()) {
            log.error("ACCOUNT_BOOTSTRAP_SKIPPED: role={} because its password setting is blank. "
                    + "Check APP_BOOTSTRAP_{}_PASSWORD in Render.", seed.role(), seed.role());
            return;
        }

        if (seed.email().isBlank()) {
            log.error("ACCOUNT_BOOTSTRAP_SKIPPED: role={} because its email setting is blank. "
                    + "Check APP_BOOTSTRAP_{}_EMAIL in Render.", seed.role(), seed.role());
            return;
        }

        if (seed.password().length() < 10) {
            log.error("ACCOUNT_BOOTSTRAP_SKIPPED: role={} because its configured password is shorter than 10 characters.",
                    seed.role());
            return;
        }

        List<User> users = userRepository.findAll();

        User sameRole = users.stream()
                .filter(u -> seed.role().equalsIgnoreCase(safe(u.getRole())))
                .findFirst()
                .orElse(null);

        if (sameRole != null) {
            log.info("ACCOUNT_BOOTSTRAP_EXISTS: role={} username='{}'. No account was changed.",
                    seed.role(), sameRole.getUsername());
            return;
        }

        User sameUsername = users.stream()
                .filter(u -> seed.username().equalsIgnoreCase(safe(u.getUsername())))
                .findFirst()
                .orElse(null);

        if (sameUsername != null) {
            boolean sameEmail = sameUsername.getEmail() != null
                    && sameUsername.getEmail().trim().equalsIgnoreCase(seed.email());

            if (!sameEmail) {
                log.error("ACCOUNT_BOOTSTRAP_SKIPPED: configured username '{}' already belongs to a different email. "
                        + "Choose a different APP_BOOTSTRAP_{}_USERNAME in Render.",
                        seed.username(), seed.role());
                return;
            }

            sameUsername.setRole(seed.role());
            sameUsername.setActive(true);
            userRepository.save(sameUsername);
            log.info("ACCOUNT_BOOTSTRAP_PROMOTED: username='{}', role={}.",
                    sameUsername.getUsername(), seed.role());
            return;
        }

        boolean emailUsed = users.stream()
                .anyMatch(u -> u.getEmail() != null
                        && u.getEmail().trim().equalsIgnoreCase(seed.email()));

        if (emailUsed) {
            log.error("ACCOUNT_BOOTSTRAP_SKIPPED: configured email for role={} already belongs to another account. "
                    + "Use a different APP_BOOTSTRAP_{}_EMAIL in Render.", seed.role(), seed.role());
            return;
        }

        User user = new User();
        user.setUsername(seed.username());
        user.setPassword(passwordEncoder.encode(seed.password()));
        user.setFullName(seed.fullName());
        user.setEmail(seed.email());
        user.setRole(seed.role());
        user.setActive(true);

        User saved = userRepository.saveAndFlush(user);
        log.info("ACCOUNT_BOOTSTRAP_CREATED: role={}, username='{}', id={}.",
                seed.role(), saved.getUsername(), saved.getId());
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String or(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
