/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel.service.config;

import com.rreganjr.requel.service.auth.ApiTokenRepository;
import com.rreganjr.requel.service.auth.JwtAuthenticationFilter;
import com.rreganjr.requel.service.auth.JwtService;
import com.rreganjr.requel.service.auth.UserDtoMapper;
import com.rreganjr.requel.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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
    private final ApiTokenRepository apiTokenRepository;
    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;

    /**
     * Additional CORS allowed origins beyond same-origin (e.g. http://localhost:4200 for the
     * Angular dev server). Empty in production; set via spring.cors.allowed-origins in
     * application-dev.properties or as an environment variable.
     */
    @Value("${spring.cors.allowed-origins:}")
    private List<String> additionalAllowedOrigins;

    public ApiSecurityConfig(JwtService jwtService, ApiTokenRepository apiTokenRepository,
            UserRepository userRepository, UserDtoMapper userDtoMapper) {
        this.jwtService = jwtService;
        this.apiTokenRepository = apiTokenRepository;
        this.userRepository = userRepository;
        this.userDtoMapper = userDtoMapper;
    }

    // @Order(4): after the OAuth chains (see AuthorizationServerConfig). This chain's matcher is
    // /api/**, which is broader than the MCP resource-server chain's /api/mcp/** (Slice 2), so it
    // must have a HIGHER order number (lower precedence) than that chain. Chain layering:
    //   1 = AS endpoints, 2 = interactive login/consent, 3 = /api/mcp/** resource server (Slice 2),
    //   4 = this /api/** JWT chain.
    @Bean
    @Order(4)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/dev/**").permitAll()
                // Personal access token management (#73): project users only. PATs act against
                // project data, so a pure system admin (no project role) has no use for one; an
                // admin who also holds ProjectUserRole still qualifies through that role.
                .requestMatchers("/api/auth/tokens", "/api/auth/tokens/**")
                    .hasRole("ProjectUserRole")
                .requestMatchers("/api/users/organizations").authenticated()
                .requestMatchers("/api/users/**").hasRole("SystemAdminUserRole")
                .requestMatchers("/api/commands/NewUser").hasRole("SystemAdminUserRole")
                .requestMatchers("/api/projects/**").authenticated()
                .requestMatchers("/api/**").authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, apiTokenRepository,
                    userRepository, userDtoMapper),
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
