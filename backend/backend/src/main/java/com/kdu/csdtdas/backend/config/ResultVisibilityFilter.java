package com.kdu.csdtdas.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Final results are shown on the school portal, never on CSDT-DAS. A student can still see the list of
 * courses they have to carry over, because registration needs it.
 */
@Component
public class ResultVisibilityFilter extends OncePerRequestFilter {

    private static final Pattern OFFICIAL = Pattern.compile("^/api/results/student/\\d+(/.*)?$");
    private static final Pattern ALLOWED = Pattern.compile("^/api/results/student/\\d+/outstanding-courses$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean student = auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));

        String path = request.getRequestURI();
        if (student && OFFICIAL.matcher(path).matches() && !ALLOWED.matcher(path).matches()) {
            response.setStatus(403);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Final results are on the school portal. Here you can see your C.A. scores, attendance and carry-over courses.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}