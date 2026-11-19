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
package com.rreganjr.requel.service.api.dto;

/**
 * Issue #449: which build of Requel a server is running, for gateway callers (MCP's
 * {@code getServerInfo}, {@code requel server-info}). Read from the same build and git data as
 * {@code /actuator/info}, which also carries Java and OS details but needs a system admin.
 *
 * @param version    the Requel version, e.g. {@code 2.2.0}; never null ({@code "unknown"} when the
 *                   build recorded none)
 * @param commit     the short git commit id, or null when the build had no git repository
 * @param commitTime when that commit was made (ISO-8601), or null
 * @param buildTime  when the jar was built (ISO-8601), or null
 */
public record ServerInfoDto(String version, String commit, String commitTime, String buildTime) {
}
