package com.kdu.csdtdas.backend.config;

import jakarta.servlet.http.HttpServletRequest;

/** The caller's address. Behind Render's proxy the last X-Forwarded-For entry is the one the proxy added. */
public final class ClientIp {

    private ClientIp() {}

    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
