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
 * Requests matched here: anything that does not start with /api/, /actuator/,
 * or contain a dot (i.e. not a file with an extension like .js, .css, .ico).
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
            "/{path:[^\\.]*}",
            "/{path:[^\\.]*}/**"
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
