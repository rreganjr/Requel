package com.rreganjr.requel.service.config;

import com.rreganjr.requel.service.auth.JwtAuthenticationFilter;
import com.rreganjr.requel.service.auth.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security configuration for the CQRS API layer.
 * Stateless JWT-based auth, CORS for Angular dev server.
 */
@Configuration
public class ApiSecurityConfig {

    private final JwtService jwtService;

    /**
     * Additional CORS allowed origins beyond same-origin (e.g. http://localhost:4200 for the
     * Angular dev server). Empty in production; set via spring.cors.allowed-origins in
     * application-dev.properties or as an environment variable.
     */
    @Value("${spring.cors.allowed-origins:}")
    private List<String> additionalAllowedOrigins;

    public ApiSecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/users/organizations").authenticated()
                .requestMatchers("/api/users/**").hasRole("SystemAdminUserRole")
                .requestMatchers("/api/commands/NewUser").hasRole("SystemAdminUserRole")
                .requestMatchers("/api/projects/**").authenticated()
                .requestMatchers("/api/**").authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                    UsernamePasswordAuthenticationFilter.class)
            .anonymous(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // In production the Angular app is served from the same origin, so no explicit
        // allowed-origins are needed. Add http://localhost:4200 (or other origins) via
        // spring.cors.allowed-origins in application-dev.properties for local development.
        List<String> origins = new ArrayList<>(additionalAllowedOrigins);
        if (!origins.isEmpty()) {
            config.setAllowedOrigins(origins);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
