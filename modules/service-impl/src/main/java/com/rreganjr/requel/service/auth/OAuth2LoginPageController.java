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
package com.rreganjr.requel.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Login page for the OAuth 2.1 authorization server (issue #83).
 *
 * <p><b>Why its own path.</b> The Angular SPA owns the client-side route {@code /login} (its
 * {@code LoginComponent}); if the authorization server's form login also lived at {@code /login} it
 * would shadow the SPA's page with Spring Security's generated form. So the AS login page is served
 * at {@link #LOGIN_PAGE_URI} instead, and the AS filter chain uses it as both the login page and the
 * form-login processing URL. Credentials are checked by {@link RequelUserAuthenticationProvider}
 * against the Requel user store.
 *
 * <p>Rendered as a small self-contained HTML page (no template-engine dependency), matching
 * {@link OAuth2ConsentController}.
 */
@Controller
public class OAuth2LoginPageController {

    /** Path of the authorization-server login page (NOT the SPA's {@code /login}). */
    public static final String LOGIN_PAGE_URI = "/oauth2/login";

    @GetMapping(LOGIN_PAGE_URI)
    @ResponseBody
    public String loginPage(@RequestParam(required = false) String error,
            @RequestParam(required = false) String logout, HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String csrfField = (csrfToken == null) ? ""
                : "<input type=\"hidden\" name=\"" + attr(csrfToken.getParameterName())
                        + "\" value=\"" + attr(csrfToken.getToken()) + "\">";
        String banner = "";
        if (error != null) {
            banner = "<p class=\"msg err\">Invalid username or password.</p>";
        } else if (logout != null) {
            banner = "<p class=\"msg\">You have been signed out.</p>";
        }

        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>Requel — Sign in</title><style>"
                + "body{font-family:system-ui,Arial,sans-serif;max-width:22rem;margin:4rem auto;"
                + "padding:0 1rem;color:#1a1a1a}h1{font-size:1.25rem}"
                + "label{display:block;margin:.75rem 0 .25rem;font-size:.9rem}"
                + "input[type=text],input[type=password]{width:100%;padding:.5rem;border:1px solid #ccc;"
                + "border-radius:6px;box-sizing:border-box}"
                + "button{margin-top:1.25rem;padding:.5rem 1rem;border-radius:6px;border:1px solid #2563eb;"
                + "background:#2563eb;color:#fff;cursor:pointer}"
                + ".msg{font-size:.9rem}.err{color:#b91c1c}</style></head><body>"
                + "<h1>Sign in to Requel</h1>"
                + "<p class=\"msg\">An application is requesting access to your Requel account.</p>"
                + banner
                + "<form method=\"post\" action=\"" + LOGIN_PAGE_URI + "\">"
                + csrfField
                + "<label for=\"username\">Username</label>"
                + "<input type=\"text\" id=\"username\" name=\"username\" autofocus>"
                + "<label for=\"password\">Password</label>"
                + "<input type=\"password\" id=\"password\" name=\"password\">"
                + "<button type=\"submit\">Sign in</button>"
                + "</form></body></html>";
    }

    private static String attr(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
