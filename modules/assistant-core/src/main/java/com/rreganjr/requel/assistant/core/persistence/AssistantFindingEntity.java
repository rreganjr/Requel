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
 * JPA mapping for the {@code assistant_findings} table. Carries the
 * idempotency key, the finding state machine, and a soft pointer to the
 * applied annotation row. Cleanup policy and state transitions are owned
 * by {@code AssistantResultApplicator}; this entity records the result.
 */
@Entity
@Table(name = "assistant_findings")
public class AssistantFindingEntity {

	@Id
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private String id;

	@Column(name = "idempotency_key", length = 255, nullable = false, unique = true)
	private String idempotencyKey;

	@Column(name = "assistant_id", length = 200, nullable = false)
	private String assistantId;

	@Column(name = "project_id")
	private Long projectId;

	@Column(name = "target_type", length = 80, nullable = false)
	private String targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Column(name = "finding_type", length = 120, nullable = false)
	private String findingType;

	@Column(name = "severity", length = 20)
	private String severity;

	@Column(name = "confidence", precision = 4, scale = 3)
	private BigDecimal confidence;

	@Column(name = "summary", length = 500)
	private String summary;

	@Column(name = "evidence_json", columnDefinition = "TEXT")
	private String evidenceJson;

	@Column(name = "applied_annotation_id")
	private Long appliedAnnotationId;

	@Column(name = "state", length = 20, nullable = false)
	private String state;

	@Column(name = "created_run_id", length = 36, nullable = false)
	private String createdRunId;

	@Column(name = "last_seen_run_id", length = 36, nullable = false)
	private String lastSeenRunId;

	@Column(name = "superseded_by_run_id", length = 36)
	private String supersededByRunId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@Column(name = "closed_at")
	private Instant closedAt;

	protected AssistantFindingEntity() {
		// for JPA
	}

	public AssistantFindingEntity(UUID id, String idempotencyKey, String assistantId,
			String targetType, Long targetId, String findingType, String state, UUID createdRunId,
			Instant createdAt) {
		this.id = id.toString();
		this.idempotencyKey = idempotencyKey;
		this.assistantId = assistantId;
		this.targetType = targetType;
		this.targetId = targetId;
		this.findingType = findingType;
		this.state = state;
		this.createdRunId = createdRunId.toString();
		this.lastSeenRunId = this.createdRunId;
		this.createdAt = createdAt;
		this.lastSeenAt = createdAt;
	}

	public UUID getFindingId() {
		return UUID.fromString(id);
	}

	public String getId() {
		return id;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public String getAssistantId() {
		return assistantId;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public String getTargetType() {
		return targetType;
	}

	public Long getTargetId() {
		return targetId;
	}

	public String getFindingType() {
		return findingType;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public BigDecimal getConfidence() {
		return confidence;
	}

	public void setConfidence(BigDecimal confidence) {
		this.confidence = confidence;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getEvidenceJson() {
		return evidenceJson;
	}

	public void setEvidenceJson(String evidenceJson) {
		this.evidenceJson = evidenceJson;
	}

	public Long getAppliedAnnotationId() {
		return appliedAnnotationId;
	}

	public void setAppliedAnnotationId(Long appliedAnnotationId) {
		this.appliedAnnotationId = appliedAnnotationId;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCreatedRunId() {
		return createdRunId;
	}

	public String getLastSeenRunId() {
		return lastSeenRunId;
	}

	public void setLastSeenRunId(String lastSeenRunId) {
		this.lastSeenRunId = lastSeenRunId;
	}

	public String getSupersededByRunId() {
		return supersededByRunId;
	}

	public void setSupersededByRunId(String supersededByRunId) {
		this.supersededByRunId = supersededByRunId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public void setLastSeenAt(Instant lastSeenAt) {
		this.lastSeenAt = lastSeenAt;
	}

	public Instant getClosedAt() {
		return closedAt;
	}

	public void setClosedAt(Instant closedAt) {
		this.closedAt = closedAt;
	}
}
