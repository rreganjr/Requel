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
package com.rreganjr.requel.assistant.core.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA mapping for the {@code assistant_usages} table. Stores per-provider-call
 * telemetry plus optional request/response bodies. Bodies are present only
 * when the parent {@link AssistantRunEntity#getBodyCaptureReason() body
 * capture reason} is non-null and the TTL has not elapsed; the application
 * is responsible for encryption/decryption at the boundary.
 */
@Entity
@Table(name = "assistant_usages")
public class AssistantUsageEntity {

	@Id
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private String id;

	@Column(name = "run_id", length = 36, nullable = false)
	private String runId;

	@Column(name = "provider", length = 80)
	private String provider;

	@Column(name = "model", length = 120)
	private String model;

	@Column(name = "input_tokens")
	private Integer inputTokens;

	@Column(name = "output_tokens")
	private Integer outputTokens;

	@Column(name = "cached_input_tokens")
	private Integer cachedInputTokens;

	@Column(name = "cost_estimate", precision = 12, scale = 6)
	private BigDecimal costEstimate;

	@Column(name = "latency_ms")
	private Long latencyMs;

	@Column(name = "request_body", columnDefinition = "LONGTEXT")
	private String requestBody;

	@Column(name = "response_body", columnDefinition = "LONGTEXT")
	private String responseBody;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AssistantUsageEntity() {
		// for JPA
	}

	public AssistantUsageEntity(UUID id, UUID runId, Instant createdAt) {
		this.id = id.toString();
		this.runId = runId.toString();
		this.createdAt = createdAt;
	}

	public UUID getUsageId() {
		return UUID.fromString(id);
	}

	public String getId() {
		return id;
	}

	public UUID getRunUuid() {
		return UUID.fromString(runId);
	}

	public String getRunId() {
		return runId;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Integer getInputTokens() {
		return inputTokens;
	}

	public void setInputTokens(Integer inputTokens) {
		this.inputTokens = inputTokens;
	}

	public Integer getOutputTokens() {
		return outputTokens;
	}

	public void setOutputTokens(Integer outputTokens) {
		this.outputTokens = outputTokens;
	}

	public Integer getCachedInputTokens() {
		return cachedInputTokens;
	}

	public void setCachedInputTokens(Integer cachedInputTokens) {
		this.cachedInputTokens = cachedInputTokens;
	}

	public BigDecimal getCostEstimate() {
		return costEstimate;
	}

	public void setCostEstimate(BigDecimal costEstimate) {
		this.costEstimate = costEstimate;
	}

	public Long getLatencyMs() {
		return latencyMs;
	}

	public void setLatencyMs(Long latencyMs) {
		this.latencyMs = latencyMs;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}

	public String getResponseBody() {
		return responseBody;
	}

	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
