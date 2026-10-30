package com.kdu.csdtdas.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Slows down password guessing, sign-up floods and reset-code emails. Counts are kept per address. */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);
    private static final long WINDOW_MS = 15 * 60_000L;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return kindOf(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain
    ) throws ServletException, IOException {

        String kind = kindOf(request.getRequestURI());
        String key = kind + ":" + ClientIp.of(request);
        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        boolean blocked;

        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > WINDOW_MS) queue.pollFirst();
            blocked = queue.size() >= limitOf(kind);
            // Sign-up and reset requests count every time; sign-ins count only when they fail (below).
            if (!blocked && !"login".equals(kind)) queue.addLast(now);
        }

        if (blocked) {
            log.warn("Rate limit reached for {}", key);
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many attempts. Please wait about 15 minutes and try again.\"}");
            return;
        }

        chain.doFilter(request, response);

        if ("login".equals(kind) && response.getStatus() >= 400) {
            synchronized (queue) { queue.addLast(now); }
        }

        if (hits.size() > 5000) {
            hits.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    Long last = e.getValue().peekLast();
                    return last == null || now - last > WINDOW_MS;
                }
            });
        }
    }

    private String kindOf(String path) {
        if (path.equals("/api/auth/login")) return "login";
        if (path.equals("/api/auth/signup")) return "signup";
        if (path.startsWith("/api/auth/forgot-password/")) return "forgot";
        return null;
    }

    private int limitOf(String kind) {
        return switch (kind) {
            case "login" -> 30;
            case "signup" -> 20;
            default -> 10;
        };
    }
}