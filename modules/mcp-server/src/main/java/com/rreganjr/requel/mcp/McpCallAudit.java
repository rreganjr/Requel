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

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Audit record for one MCP JSON-RPC call. References the triggering user by id (not
 * username) so the association survives username changes.
 *
 * <p>
 * {@code assistantUserId} and {@code runId} carry the assistant pseudo-user and the
 * assistant run that a future internal-AI session token will bind a call to
 * (issue #43 Step 8 dual identity). They are nullable until that path exists; today
 * an MCP call is authenticated as the triggering user via the existing JWT chain.
 */
@Entity
@Table(name = "mcp_calls")
public class McpCallAudit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "triggering_user_id")
	private Long triggeringUserId;

	@Column(name = "assistant_user_id")
	private Long assistantUserId;

	@Column(name = "run_id", length = 36)
	private String runId;

	@Column(name = "method", nullable = false, length = 120)
	private String method;

	@Column(name = "tool_name", length = 200)
	private String toolName;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "error_code")
	private Integer errorCode;

	@Column(name = "error_summary", length = 1000)
	private String errorSummary;

	@Column(name = "duration_ms")
	private Long durationMs;

	@Column(name = "called_at", nullable = false)
	private Instant calledAt;

	protected McpCallAudit() {
		// for JPA
	}

	public McpCallAudit(Long triggeringUserId, Long assistantUserId, String runId, String method,
			String toolName, String status, Integer errorCode, String errorSummary, Long durationMs,
			Instant calledAt) {
		this.triggeringUserId = triggeringUserId;
		this.assistantUserId = assistantUserId;
		this.runId = runId;
		this.method = method;
		this.toolName = toolName;
		this.status = status;
		this.errorCode = errorCode;
		this.errorSummary = errorSummary;
		this.durationMs = durationMs;
		this.calledAt = calledAt;
	}

	public Long getId() {
		return id;
	}

	public Long getTriggeringUserId() {
		return triggeringUserId;
	}

	public Long getAssistantUserId() {
		return assistantUserId;
	}

	public String getRunId() {
		return runId;
	}

	public String getMethod() {
		return method;
	}

	public String getToolName() {
		return toolName;
	}

	public String getStatus() {
		return status;
	}

	public Integer getErrorCode() {
		return errorCode;
	}

	public String getErrorSummary() {
		return errorSummary;
	}

	public Long getDurationMs() {
		return durationMs;
	}

	public Instant getCalledAt() {
		return calledAt;
	}
}
