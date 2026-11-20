package com.kdu.csdtdas.backend.config;

import org.springframework.stereotype.Component;

/**
 * On the live server (Render) the login signing key must be set in the environment. The key written in
 * application.properties is only for your own computer and was visible in a public repository.
 */
@Component
public class ProductionSafetyCheck {

    public ProductionSafetyCheck() {
        if (System.getenv("RENDER") == null) return;

        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set on the server (or is shorter than 32 characters). "
                            + "Set it in the Render Environment settings before starting.");
        }
    }
}