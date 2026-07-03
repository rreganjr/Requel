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
package com.rreganjr.requel.cli;

import com.rreganjr.requel.gateway.rest.AsMetadata;
import com.rreganjr.requel.gateway.rest.OAuthClient;
import com.rreganjr.requel.gateway.rest.OAuthTokens;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Stores credentials for the current {@code --url}. Two mutually-exclusive modes:
 * <ul>
 *   <li>{@code requel login --token reqpat_…} — headless: save a personal access token (#73).</li>
 *   <li>{@code requel login --oauth} — interactive: run the OAuth 2.1 authorization-code + PKCE flow
 *       (#83 AS, seeded {@code requel-cli} client) via a loopback browser redirect, and save the
 *       resulting access + rotating refresh tokens. Later commands auto-refresh (see
 *       {@link CliTokenSource}).</li>
 * </ul>
 */
@Command(name = "login", description = "Store credentials for the server URL (PAT or OAuth login).")
public class LoginCommand implements Callable<Integer> {

    /** How long to wait for the user to complete the browser login before giving up. */
    private static final long LOGIN_TIMEOUT_MINUTES = 5;

    @ParentCommand
    RequelCli parent;

    @ArgGroup(multiplicity = "1")
    Mode mode = new Mode();

    /** Exactly one of a PAT or interactive OAuth. */
    static class Mode {
        @Option(names = "--token", paramLabel = "TOKEN",
                description = "A Requel personal access token (reqpat_...).")
        String token;

        @Option(names = "--oauth",
                description = "Log in interactively via OAuth (browser); stores refreshable tokens.")
        boolean oauth;
    }

    // ---- Test seams -----------------------------------------------------------------------------

    /** When set, used instead of {@code new OAuthClient(parent.url)}. */
    OAuthClient oauthClientOverride;
    /** How the authorize URL is opened; defaults to the desktop browser (falls back to printing). */
    Consumer<String> browserOpener = LoginCommand::openInBrowser;

    private final SecureRandom random = new SecureRandom();

    @Override
    public Integer call() {
        if (mode.token != null) {
            parent.credentialStore.save(parent.url, mode.token);
            System.out.println("Saved credentials for " + parent.url + " ("
                    + parent.credentialStore.file() + ").");
            return ExitCode.SUCCESS;
        }
        return oauthLogin();
    }

    private Integer oauthLogin() {
        OAuthClient client = (oauthClientOverride != null) ? oauthClientOverride
                : new OAuthClient(parent.url);
        try (LoopbackCallbackServer server = LoopbackCallbackServer.start()) {
            AsMetadata meta = client.discover();
            Pkce pkce = Pkce.generate();
            String state = randomState();
            String authorizeUrl = buildAuthorizeUrl(meta.authorizationEndpoint(),
                    server.redirectUri(), pkce.challenge(), state);

            System.out.println("Opening your browser to log in. If it doesn't open, visit:\n  "
                    + authorizeUrl);
            browserOpener.accept(authorizeUrl);

            String code = server.awaitCode(state, LOGIN_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            OAuthTokens tokens = client.exchangeAuthorizationCode(meta.tokenEndpoint(),
                    CliTokenSource.CLI_CLIENT_ID, server.redirectUri(), code, pkce.verifier());
            parent.credentialStore.saveOAuth(parent.url, tokens);
            System.out.println("Logged in. Tokens saved for " + parent.url + " ("
                    + parent.credentialStore.file() + ").");
            return ExitCode.SUCCESS;
        } catch (java.util.concurrent.TimeoutException e) {
            System.err.println("Login timed out waiting for the browser redirect.");
            return ExitCode.AUTH;
        } catch (IllegalStateException e) {
            System.err.println("Login failed: " + e.getMessage());
            return ExitCode.AUTH;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Login interrupted.");
            return ExitCode.UNEXPECTED;
        } catch (RuntimeException | java.io.IOException e) {
            System.err.println("Login failed: " + e.getMessage());
            return ExitCode.UNEXPECTED;
        }
    }

    private String randomState() {
        byte[] raw = new byte[16];
        random.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private static String buildAuthorizeUrl(String authorizationEndpoint, String redirectUri,
            String codeChallenge, String state) {
        return authorizationEndpoint
                + "?response_type=code"
                + "&client_id=" + enc(CliTokenSource.CLI_CLIENT_ID)
                + "&scope=" + enc("mcp")
                + "&redirect_uri=" + enc(redirectUri)
                + "&code_challenge=" + enc(codeChallenge)
                + "&code_challenge_method=" + enc(Pkce.METHOD)
                + "&state=" + enc(state);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Best-effort desktop browser open; a headless/unsupported environment just relies on the URL. */
    private static void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
            // Fall back to the printed URL the caller already emitted.
        }
    }
}
