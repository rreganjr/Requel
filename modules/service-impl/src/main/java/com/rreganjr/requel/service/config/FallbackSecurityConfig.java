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

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * #449: the last security chain, for every request no other chain matches (see the layering on
 * {@link AuthorizationServerConfig}). Before it, such requests passed through no security filter
 * at all: Spring Boot's default chain backs off once an application defines its own.
 * <p>
 * What lands here is the Angular SPA ({@code SpaController} forwards its routes to
 * {@code index.html}), static files, {@code /error}, and discovery documents under
 * {@code /.well-known} that the authorization-server chain doesn't claim. So:
 * <ul>
 *   <li>forward and error dispatches are allowed: the SPA's forward to {@code index.html}, and
 *       {@code /error} after a request on another chain failed, so that request's own status and
 *       body still come back instead of a 403 from this chain;</li>
 *   <li>GET and HEAD are allowed;</li>
 *   <li>every other method is refused (403). Every endpoint that changes anything lives under a
 *       chain above ({@code /api/**} or the OAuth paths).</li>
 * </ul>
 */
@Configuration
public class FallbackSecurityConfig {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain fallbackSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/**").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                .anyRequest().denyAll());

        return http.build();
    }
}
