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
 */package com.rreganjr.requel.service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cache headers for the Angular UI the jar serves (issue #396).
 *
 * <p>Spring Boot serves the UI with no {@code Cache-Control} and a {@code Last-Modified} of
 * 1980 (the jar's entry time), so a browser guessed it could keep {@code index.html} for years.
 * After an upgrade it kept loading the old page, which asks for {@code main-<oldhash>.js}; the
 * new jar doesn't have it, so the page stayed blank until the cache was cleared.
 *
 * <ul>
 *   <li>Files whose name carries an Angular content hash ({@code main-ELPTX4BG.js},
 *       {@code chunk-*.js}, {@code styles-*.css}, {@code media/*-SOXVSX3R.woff2}) never change
 *       under that name: cache them for a year.</li>
 *   <li>Everything else the UI serves ({@code /}, {@code /index.html}, the client-side routes
 *       {@link SpaController} forwards to it, {@code favicon.ico}): {@code no-cache}, so the
 *       browser asks every time. {@code spring.web.resources.cache.use-last-modified=false}
 *       drops the 1980 {@code Last-Modified}; with it the browser's revalidation got a 304 for
 *       a page that had changed.</li>
 * </ul>
 * The API, actuator and OAuth endpoints set their own headers and are left alone.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StaticCacheHeadersFilter extends OncePerRequestFilter {

    static final String HASHED = "public, max-age=31536000, immutable";
    static final String UNHASHED = "no-cache";

    /** Angular's output hashing: a dash, eight upper-case letters or digits, the extension. */
    private static final Pattern HASHED_NAME = Pattern.compile(".*-[A-Z0-9]{8}\\.[a-z0-9]+$");

    private static final String[] NOT_UI = {
            "/api/", "/actuator/", "/oauth2/", "/connect/", "/.well-known/", "/userinfo", "/login/"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (String prefix : NOT_UI) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        response.setHeader(HttpHeaders.CACHE_CONTROL,
                HASHED_NAME.matcher(path).matches() ? HASHED : UNHASHED);
        chain.doFilter(request, response);
    }
}
