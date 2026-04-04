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
