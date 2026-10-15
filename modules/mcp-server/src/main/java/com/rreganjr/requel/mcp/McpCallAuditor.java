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

import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;

/**
 * Records one {@link McpCallAudit} row per MCP {@code tools/call}: who (triggering user from
 * the security context), what (the tool name), and the outcome (OK or an error code/summary)
 * with timing. Auditing is best-effort — a failure to record must never break the MCP call, so
 * all exceptions are logged and swallowed.
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
	 * Transport-neutral audit of a single {@code tools/call} on the Spring AI MCP server (Streamable
	 * HTTP) transport. Records the triggering user (from the security context), the tool name, and
	 * the OK/ERROR outcome with timing. {@code assistantUserId} is left null until real per-client
	 * identities exist (#73). Best-effort — never throws.
	 *
	 * @param startNanos a {@link System#nanoTime()} reading captured before the tool executed
	 */
	public void recordToolCall(String toolName, boolean ok, Integer errorCode, String errorSummary,
			long startNanos) {
		try {
			long durationMs = Math.max(0, (System.nanoTime() - startNanos) / 1_000_000L);
			McpCallAudit audit = new McpCallAudit(resolveTriggeringUserId(), null, null,
					"tools/call", toolName, ok ? "OK" : "ERROR", errorCode, truncate(errorSummary),
					durationMs, clock.instant());
			repository.save(audit);
		} catch (RuntimeException e) {
			log.warn("Failed to record MCP tool-call audit: {}", e.getMessage(), e);
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

	private static String truncate(String text) {
		if (text == null) {
			return null;
		}
		return text.length() <= MAX_ERROR_SUMMARY ? text : text.substring(0, MAX_ERROR_SUMMARY);
	}
}
