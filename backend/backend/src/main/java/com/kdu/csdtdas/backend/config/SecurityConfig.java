package com.kdu.csdtdas.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    // Lab Officers and Technical Staff are deliberately NOT in this list.
    private static final String[] ACADEMIC =
            {"HOD", "ADMIN", "LECTURER", "ADJUNCT", "LEVEL_ADVISER", "STUDENT"};

    private static final String[] RESULT_WRITERS =
            {"HOD", "ADMIN", "LECTURER", "ADJUNCT", "LEVEL_ADVISER"};

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final AccessControlFilter accessControlFilter;
    private final RegistrationGateFilter registrationGateFilter;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            AuthRateLimitFilter authRateLimitFilter,
            AccessControlFilter accessControlFilter,
            RegistrationGateFilter registrationGateFilter
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authRateLimitFilter = authRateLimitFilter;
        this.accessControlFilter = accessControlFilter;
        this.registrationGateFilter = registrationGateFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Missing/expired login -> 401 (the frontend then returns to sign-in).
                // Logged in but not allowed -> 403.
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Spring's own error page. Without this, every error message arrives empty.
                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/api/auth/**").permitAll()

                        // Only the simple "is it running" check is public. Every other actuator page is shut.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").denyAll()

                        // Every signed-in person (all roles) manages their own account.
                        .requestMatchers("/api/account/**").authenticated()

                        // Nobody, including the H.O.D, can set another person's password.
                        .requestMatchers("/api/users/*/reset-password").denyAll()

                        // Own phone number (the controller checks self-or-H.O.D).
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/phone").hasAnyRole(ACADEMIC)

                        // Registration clearance: students submit, the H.O.D decides.
                        .requestMatchers("/api/clearance/mine", "/api/clearance/submit").hasRole("STUDENT")
                        .requestMatchers("/api/clearance/**").hasAnyRole("HOD", "ADMIN")

                        // H.O.D only
                        .requestMatchers("/api/users/**").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers("/api/advisers/mine").hasRole("STUDENT")
                        .requestMatchers("/api/advisers/**").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers("/api/allocations/docx/**").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers("/api/allocations/offer").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers("/api/allocations/add-lecturer").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/allocations/*/link/*").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/allocations/**").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/allocations/claim").hasAnyRole("LECTURER", "ADJUNCT", "LEVEL_ADVISER")

                        // Laboratory area (separate from academics)
                        .requestMatchers("/api/lab/**").hasAnyRole("HOD", "ADMIN", "LAB_OFFICER", "TECHNICAL_STAFF")

                        // Results: only the H.O.D verifies; staff enter scores; students cannot write.
                        .requestMatchers(HttpMethod.PUT, "/api/results/*/approve").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/results/*/reject").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/results/course/*/session/*/semester/*/approve-all").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/results/**").hasAnyRole("HOD", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/results/**").hasAnyRole(RESULT_WRITERS)
                        .requestMatchers(HttpMethod.PUT, "/api/results/**").hasAnyRole(RESULT_WRITERS)

                        // Everything else academic
                        .requestMatchers("/api/**").hasAnyRole(ACADEMIC)
                        .anyRequest().authenticated()
                )
                // Order: limit sign-in attempts, read the sign-in, check access to the specific record, then the registration lock.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authRateLimitFilter, JwtAuthFilter.class)
                .addFilterAfter(accessControlFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(registrationGateFilter, AccessControlFilter.class);

        return http.build();
    }
}