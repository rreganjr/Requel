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
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Renders the OAuth 2.1 consent screen (issue #83, Slice 1). Consent is required for all MCP
 * clients (no auto-approve), so after login the authorization endpoint redirects here; the user
 * sees which client is connecting and which scopes it wants, and approves or denies.
 *
 * <p>The page is emitted as a small self-contained HTML document (no template engine dependency).
 * Its form posts back to the authorization endpoint ({@code /oauth2/authorize}) with the standard
 * parameters ({@code client_id}, {@code state}, and one {@code scope} value per approved scope) plus
 * the CSRF token. Approving with no scopes selected results in an access-denied response from the
 * authorization server — that is the "Deny" path.
 */
@Controller
public class OAuth2ConsentController {

    /** Path of the consent page; referenced by the authorization-server chain configuration. */
    public static final String CONSENT_PAGE_URI = "/oauth2/consent";

    /** Human-readable descriptions for the scopes Requel issues. */
    private static final Map<String, String> SCOPE_DESCRIPTIONS = Map.of(
            "mcp", "Act as you through Requel's MCP tools — read and modify the project data you "
                    + "already have access to. Requel still enforces your per-project permissions.");

    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationConsentService authorizationConsentService;

    public OAuth2ConsentController(RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationConsentService authorizationConsentService) {
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationConsentService = authorizationConsentService;
    }

    @GetMapping(CONSENT_PAGE_URI)
    @ResponseBody
    public String consent(Principal principal,
            @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
            @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
            @RequestParam(OAuth2ParameterNames.STATE) String state,
            HttpServletRequest request) {

        Set<String> scopesToApprove = new LinkedHashSet<>();
        Set<String> previouslyApprovedScopes = new LinkedHashSet<>();
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        OAuth2AuthorizationConsent currentConsent = (registeredClient != null)
                ? authorizationConsentService.findById(registeredClient.getId(), principal.getName())
                : null;
        Set<String> authorizedScopes = (currentConsent != null)
                ? currentConsent.getScopes() : Collections.emptySet();
        for (String requestedScope : StringUtils.delimitedListToStringArray(scope, " ")) {
            if (OidcScopes.OPENID.equals(requestedScope)) {
                continue;
            }
            if (authorizedScopes.contains(requestedScope)) {
                previouslyApprovedScopes.add(requestedScope);
            } else {
                scopesToApprove.add(requestedScope);
            }
        }

        String clientName = (registeredClient != null) ? registeredClient.getClientName() : clientId;
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return renderPage(clientName, clientId, state, scopesToApprove, previouslyApprovedScopes,
                csrfToken);
    }

    private String renderPage(String clientName, String clientId, String state,
            Set<String> scopesToApprove, Set<String> previouslyApproved, CsrfToken csrfToken) {
        StringBuilder scopeItems = new StringBuilder();
        for (String s : scopesToApprove) {
            scopeItems.append("<label class=\"scope\">")
                    .append("<input type=\"checkbox\" name=\"scope\" value=\"").append(attr(s))
                    .append("\" checked> <strong>").append(esc(s)).append("</strong>")
                    .append("<div class=\"desc\">")
                    .append(esc(SCOPE_DESCRIPTIONS.getOrDefault(s, s)))
                    .append("</div></label>");
        }
        StringBuilder priorItems = new StringBuilder();
        for (String s : previouslyApproved) {
            priorItems.append("<li>").append(esc(s)).append("</li>");
        }
        String priorBlock = previouslyApproved.isEmpty() ? ""
                : "<p class=\"prior\">Already approved: <ul>" + priorItems + "</ul></p>";
        String csrfField = (csrfToken == null) ? ""
                : "<input type=\"hidden\" name=\"" + attr(csrfToken.getParameterName())
                        + "\" value=\"" + attr(csrfToken.getToken()) + "\">";

        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>Requel — Authorize</title><style>"
                + "body{font-family:system-ui,Arial,sans-serif;max-width:34rem;margin:3rem auto;"
                + "padding:0 1rem;color:#1a1a1a}h1{font-size:1.25rem}"
                + ".client{font-weight:600}.scope{display:block;border:1px solid #ddd;border-radius:6px;"
                + "padding:.75rem;margin:.5rem 0}.desc{color:#555;font-size:.9rem;margin-top:.25rem}"
                + ".actions{margin-top:1.25rem;display:flex;gap:.75rem}"
                + "button{padding:.5rem 1rem;border-radius:6px;border:1px solid #ccc;cursor:pointer}"
                + "button.allow{background:#2563eb;color:#fff;border-color:#2563eb}"
                + ".prior{color:#555;font-size:.9rem}</style></head><body>"
                + "<h1>Authorize access</h1>"
                + "<p><span class=\"client\">" + esc(clientName) + "</span> is requesting access to "
                + "your Requel account. Review what it can do:</p>"
                + "<form method=\"post\" action=\"/oauth2/authorize\">"
                + csrfField
                + "<input type=\"hidden\" name=\"client_id\" value=\"" + attr(clientId) + "\">"
                + "<input type=\"hidden\" name=\"state\" value=\"" + attr(state) + "\">"
                + scopeItems
                + priorBlock
                + "<div class=\"actions\">"
                + "<button type=\"submit\" class=\"allow\">Allow</button>"
                + "<button type=\"submit\" formnovalidate onclick=\"this.form.querySelectorAll("
                + "'input[name=scope]').forEach(c=>c.checked=false)\">Deny</button>"
                + "</div></form></body></html>";
    }

    /** Escape for HTML text content. */
    private static String esc(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Escape for a double-quoted HTML attribute value. */
    private static String attr(String v) {
        if (v == null) {
            return "";
        }
        return esc(v).replace("\"", "&quot;");
    }
}
