package com.kdu.csdtdas.backend.config;

import com.kdu.csdtdas.backend.service.RegistrationClearanceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Keeps a student out of materials, online classes, results and analytics until the H.O.D
 * has approved their registration. Does nothing while the lock is switched off.
 */
@Component
public class RegistrationGateFilter extends OncePerRequestFilter {

    private static final List<String> LOCKED_AREAS = List.of(
            "/api/materials", "/api/online-classes", "/api/results",
            "/api/self-results", "/api/analytics", "/api/reports"
    );

    private final RegistrationClearanceService clearanceService;

    public RegistrationGateFilter(RegistrationClearanceService clearanceService) {
        this.clearanceService = clearanceService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain
    ) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && isStudent(auth) && isLocked(request.getRequestURI())) {
            if (!clearanceService.studentIsCleared(auth.getName())) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"code\":\"REGISTRATION_NOT_APPROVED\",\"message\":"
                                + "\"Your course registration has not been approved by the H.O.D yet. "
                                + "Open Course Registration to see what to do next.\"}"
                );
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isStudent(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));
    }

    private boolean isLocked(String uri) {
        for (String area : LOCKED_AREAS) {
            if (uri.equals(area) || uri.startsWith(area + "/")) return true;
        }
        return false;
    }
}