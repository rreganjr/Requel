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
package com.rreganjr.requel.mcp;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Populates {@link McpClientContext} from the optional {@code X-Requel-Client} header on MCP
 * requests ({@code /api/mcp/**}) so the tool dispatch can attribute the call to a client and carry
 * it into the gateway (issue #69 Slice 5). Scoped to the MCP paths and always cleared after the
 * request so the thread-local never leaks across pooled threads.
 */
@Component
public class McpClientContextFilter extends OncePerRequestFilter {

	static final String CLIENT_HEADER = "X-Requel-Client";

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri == null || !uri.startsWith("/api/mcp");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		try {
			String clientId = request.getHeader(CLIENT_HEADER);
			if (clientId != null && !clientId.isBlank()) {
				McpClientContext.setClientId(clientId.trim());
			}
			filterChain.doFilter(request, response);
		} finally {
			McpClientContext.clear();
		}
	}
}
