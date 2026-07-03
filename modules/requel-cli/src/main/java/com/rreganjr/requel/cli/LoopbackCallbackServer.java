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

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A tiny loopback HTTP server that catches the OAuth authorization-code redirect. Binds an ephemeral
 * port on {@code 127.0.0.1} (per RFC 8252 native-app guidance; Requel's AS relaxes the port for
 * loopback redirect URIs), serves the fixed {@code /callback} path, captures the {@code code}/
 * {@code state} query params, shows the user a "you can close this tab" page, and completes a future.
 * The IP literal (not {@code localhost}) is used so the AS's loopback matching applies.
 */
public final class LoopbackCallbackServer implements AutoCloseable {

    /** The callback path; must match the seeded client's registered redirect URI path. */
    public static final String CALLBACK_PATH = "/callback";

    private final HttpServer server;
    private final int port;
    private final CompletableFuture<Map<String, String>> received = new CompletableFuture<>();

    private LoopbackCallbackServer(HttpServer server, int port) {
        this.server = server;
        this.port = port;
    }

    /** Bind and start on an ephemeral loopback port. */
    public static LoopbackCallbackServer start() throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        LoopbackCallbackServer callback = new LoopbackCallbackServer(server, server.getAddress().getPort());
        server.createContext(CALLBACK_PATH, exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI());
            byte[] body = ("<html><body><h3>Requel CLI</h3><p>Login complete — you can close this "
                    + "tab and return to the terminal.</p></body></html>").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
            callback.received.complete(params);
        });
        server.start();
        return callback;
    }

    /** The redirect URI to hand the AS, e.g. {@code http://127.0.0.1:49xxx/callback}. */
    public String redirectUri() {
        return "http://127.0.0.1:" + port + CALLBACK_PATH;
    }

    /**
     * Block until the redirect arrives (or {@code timeout} elapses), validate {@code state}, and return
     * the authorization {@code code}.
     *
     * @throws IllegalStateException if the AS returned an {@code error}, {@code state} mismatched, or no
     *                               {@code code} was present
     */
    public String awaitCode(String expectedState, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        Map<String, String> params;
        try {
            params = received.get(timeout, unit);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("Callback server failed", e.getCause());
        }
        if (params.containsKey("error")) {
            throw new IllegalStateException("Authorization failed: " + params.get("error")
                    + (params.containsKey("error_description") ? " — " + params.get("error_description") : ""));
        }
        if (expectedState != null && !expectedState.equals(params.get("state"))) {
            throw new IllegalStateException("State mismatch — possible CSRF; aborting login.");
        }
        String code = params.get("code");
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("No authorization code in callback.");
        }
        return code;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                params.put(decode(pair), "");
            } else {
                params.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return params;
    }

    private static String decode(String s) {
        return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
