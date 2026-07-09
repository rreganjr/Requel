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
package com.rreganjr.requel.gateway.rest;

/**
 * Supplies the bearer token the REST gateway attaches to each request — a personal access token
 * (PAT, #73) for headless use, or an OAuth access token (#83) for interactive use. Resolved per
 * request so an OAuth caller can transparently refresh an expired access token behind this source.
 *
 * @see RestCommandGateway
 */
@FunctionalInterface
public interface BearerTokenSource {

    /**
     * @return the current bearer token to send as {@code Authorization: Bearer <token>}, or
     *         {@code null} to send no Authorization header (the server will then answer 401 for
     *         protected endpoints).
     */
    String currentToken();
}
