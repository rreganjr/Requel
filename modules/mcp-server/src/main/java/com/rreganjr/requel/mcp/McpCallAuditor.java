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

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;

/**
 * Records one {@link McpCallAudit} row per MCP JSON-RPC call: who (triggering user from
 * the security context), what (method + tool/resource name), and the outcome (OK or an
 * error code/summary) with timing. Auditing is best-effort — a failure to record must
 * never break the MCP call, so all exceptions are logged and swallowed.
 */
@Component
public class McpCallAuditor {

	private static final Logger log = LoggerFactory.getLogger(McpCallAuditor.class);

	private static final int MAX_ERROR_SUMMARY = 1000;

	private final McpCallAuditRepository repository;
	private final CurrentUserResolver currentUserResolver;
	private final Clock clock;

	@Autowired
	public McpCallAuditor(McpCallAuditRepository repository,
			CurrentUserResolver currentUserResolver) {
		this(repository, currentUserResolver, Clock.systemUTC());
	}

	McpCallAuditor(McpCallAuditRepository repository, CurrentUserResolver currentUserResolver,
			Clock clock) {
		this.repository = repository;
		this.currentUserResolver = currentUserResolver;
		this.clock = clock;
	}

	/**
	 * Record the outcome of an MCP call. {@code startNanos} is a {@link System#nanoTime()}
	 * reading captured before dispatch, used to derive the call duration.
	 */
	public void record(McpJsonRpcRequest request, McpJsonRpcResponse response, long startNanos) {
		try {
			long durationMs = Math.max(0, (System.nanoTime() - startNanos) / 1_000_000L);
			boolean ok = response == null || response.error() == null;
			Integer errorCode = ok ? null : response.error().code();
			String errorSummary = ok ? null : truncate(response.error().message());
			McpCallAudit audit = new McpCallAudit(resolveTriggeringUserId(), null, null,
					method(request), toolName(request), ok ? "OK" : "ERROR", errorCode, errorSummary,
					durationMs, clock.instant());
			repository.save(audit);
		} catch (RuntimeException e) {
			log.warn("Failed to record MCP call audit: {}", e.getMessage(), e);
		}
	}

	private Long resolveTriggeringUserId() {
		try {
			User user = currentUserResolver.resolve();
			return user == null ? null : user.getId();
		} catch (RuntimeException e) {
			// No authenticated user (e.g. a malformed/anonymous call that slipped through);
			// still record the call, just without a user id.
			return null;
		}
	}

	private static String method(McpJsonRpcRequest request) {
		if (request == null || request.method() == null) {
			return "<malformed>";
		}
		return request.method();
	}

	/**
	 * Best-effort name of the invoked tool / resource: {@code params.name} for
	 * {@code tools/call} and {@code params.uri} for {@code resources/read}; otherwise null.
	 */
	private static String toolName(McpJsonRpcRequest request) {
		if (request == null || request.params() == null) {
			return null;
		}
		JsonNode params = request.params();
		String key = "tools/call".equals(request.method()) ? "name"
				: "resources/read".equals(request.method()) ? "uri" : null;
		if (key == null) {
			return null;
		}
		JsonNode value = params.get(key);
		return value == null || value.isNull() ? null : value.asText();
	}

	private static String truncate(String text) {
		if (text == null) {
			return null;
		}
		return text.length() <= MAX_ERROR_SUMMARY ? text : text.substring(0, MAX_ERROR_SUMMARY);
	}
}
