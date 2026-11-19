package com.kdu.csdtdas.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Only the department's own website may call this server from a browser. It runs before everything else,
 * so it also overrides the wide-open @CrossOrigin(origins = "*") on the controllers.
 */
@Configuration
public class StrictCorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> strictCorsFilter() {
        String configured = System.getenv("APP_ALLOWED_ORIGINS");
        List<String> origins = configured == null || configured.isBlank()
                ? List.of("https://csdt-das.onrender.com", "http://localhost:5173")
                : Arrays.stream(configured.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}