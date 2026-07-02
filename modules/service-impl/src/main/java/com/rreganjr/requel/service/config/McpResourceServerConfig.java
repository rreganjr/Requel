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

import com.rreganjr.requel.service.auth.JwtAuthenticationFilter;
import com.rreganjr.requel.service.auth.JwtService;
import com.rreganjr.requel.service.auth.McpAuthenticationEntryPoint;
import com.rreganjr.requel.service.auth.McpBearerTokenResolver;
import com.rreganjr.requel.service.auth.McpJwtAuthenticationConverter;
import com.rreganjr.requel.service.auth.UserDtoMapper;
import com.rreganjr.requel.service.auth.ApiTokenRepository;
import com.rreganjr.requel.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth 2.1 resource server for the MCP endpoints (issue #83, Slice 2). Makes {@code /api/mcp/**}
 * validate authorization-server-issued access tokens so agent clients (Cursor, Claude Code, Cowork)
 * authenticate via OAuth, while personal access tokens (#73) and the SPA login JWT continue to work.
 *
 * <p>This is chain {@code @Order(3)} in the layering documented on {@link AuthorizationServerConfig}
 * (1 = AS endpoints, 2 = login/consent, 3 = this MCP chain, 4 = the {@code /api/**} JWT chain). Its
 * matcher {@code /api/mcp/**} is more specific than the {@code /api/**} chain, so it is evaluated
 * first and fully owns MCP requests.
 *
 * <p><b>Three credential kinds coexist</b> (see {@link McpBearerTokenResolver}): the existing
 * {@link JwtAuthenticationFilter} runs before the bearer filter and handles PATs and login JWTs; the
 * bearer-token resolver hands <em>only</em> AS-issued (asymmetric-signed) JWTs to the resource
 * server, which validates them with the AS's own {@link JwtDecoder} and maps the subject to a Requel
 * user via {@link McpJwtAuthenticationConverter}. The 401 entry point is the resource server's
 * {@code BearerTokenAuthenticationEntryPoint} (emits {@code WWW-Authenticate: Bearer}); Slice 3
 * augments it with the RFC 9728 {@code resource_metadata} parameter.
 */
@Configuration
public class McpResourceServerConfig {

    private final JwtService jwtService;
    private final ApiTokenRepository apiTokenRepository;
    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;
    private final JwtDecoder jwtDecoder;

    public McpResourceServerConfig(JwtService jwtService, ApiTokenRepository apiTokenRepository,
            UserRepository userRepository, UserDtoMapper userDtoMapper, JwtDecoder jwtDecoder) {
        this.jwtService = jwtService;
        this.apiTokenRepository = apiTokenRepository;
        this.userRepository = userRepository;
        this.userDtoMapper = userDtoMapper;
        this.jwtDecoder = jwtDecoder;
    }

    @Bean
    @Order(3)
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/mcp/**")
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            // PAT + login-JWT handling (unchanged behavior) runs before the OAuth bearer filter; for
            // an AS token this filter's JWT branch fails (HS256 vs the AS's RS256) and clears the
            // context, so the request falls through to the resource server below.
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, apiTokenRepository,
                    userRepository, userDtoMapper), BearerTokenAuthenticationFilter.class)
            .oauth2ResourceServer(oauth2 -> oauth2
                // RFC 9728: 401s carry WWW-Authenticate: Bearer resource_metadata="…" so clients
                // can discover the authorization server (issue #83, Slice 3).
                .authenticationEntryPoint(new McpAuthenticationEntryPoint())
                .bearerTokenResolver(new McpBearerTokenResolver())
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(
                        new McpJwtAuthenticationConverter(userRepository, userDtoMapper))))
            .anonymous(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
