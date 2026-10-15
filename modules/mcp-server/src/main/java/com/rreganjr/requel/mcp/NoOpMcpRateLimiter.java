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

import org.springframework.stereotype.Component;

/**
 * Default {@link McpRateLimiter}: allows every call. Present so the call site always has a bean to
 * invoke; a real limiter overrides it as a {@code @Primary} bean (issue #69 Slice 5 leaves only
 * the hook).
 */
@Component
public class NoOpMcpRateLimiter implements McpRateLimiter {

	@Override
	public void check(String clientId, String toolName) {
		// no-op: allow all
	}
}
