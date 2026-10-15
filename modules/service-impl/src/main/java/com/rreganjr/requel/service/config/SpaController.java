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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-static-resource requests to index.html so that
 * Angular's client-side router handles them when the app is served from the JAR.
 *
 * Each path segment is constrained by [^\\.] (no dot), so static assets whose
 * last segment contains a file extension (e.g. /images/logo.png, /media/font.woff2)
 * never match and fall through to Spring Boot's static-resource handler at /**.
 *
 * Depth covers all current Angular routes (max 4 segments); add another pattern
 * if routes deeper than 5 segments are introduced.
 *
 * Note: the old pattern "/{path:[^\\.]*}/**" only restricted the first segment;
 * "**" matched anything including extensions, causing Spring (order 0) to intercept
 * static assets before the resource handler (order MAX_VALUE-1) could serve them.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
            "/{p1:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}/{p4:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}/{p4:[^\\.]*}/{p5:[^\\.]*}"
    })
    public String forward(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Do not intercept API or actuator routes — those are handled by their own controllers.
        if (path.startsWith("/api/") || path.startsWith("/actuator/")) {
            return null;
        }
        return "forward:/index.html";
    }
}
