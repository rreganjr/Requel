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

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA mapping for the {@code assistant_runs} table. Backs
 * {@link com.rreganjr.requel.assistant.core.AssistantRunStore}'s production
 * implementation. Soft references (no FK) to users / projects keep this entity
 * decoupled from {@code user-jpa} and {@code project-jpa}.
 */
@Entity
@Table(name = "assistant_runs")
public class AssistantRunEntity {

	@Id
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private String id;

	@Column(name = "assistant_id", length = 200, nullable = false)
	private String assistantId;

	@Column(name = "assistant_user_id")
	private Long assistantUserId;

	@Column(name = "triggered_by_user_id")
	private Long triggeredByUserId;

	@Column(name = "project_id")
	private Long projectId;

	@Column(name = "target_type", length = 80)
	private String targetType;

	@Column(name = "target_id")
	private Long targetId;

	@Column(name = "task_type", length = 80)
	private String taskType;

	@Column(name = "triggered_by_username", length = 255)
	private String triggeredByUsername;

	@Column(name = "assistant_username", length = 255)
	private String assistantUsername;

	@Column(name = "locale", length = 40)
	private String locale;

	@Column(name = "attributes_json")
	private String attributesJson;

	@Column(name = "provider", length = 80)
	private String provider;

	@Column(name = "model", length = 120)
	private String model;

	@Column(name = "template_id", length = 120)
	private String templateId;

	@Column(name = "template_version", length = 40)
	private String templateVersion;

	@Column(name = "template_source", length = 80)
	private String templateSource;

	@Column(name = "status", length = 20, nullable = false)
	private String status;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "latency_ms")
	private Long latencyMs;

	@Column(name = "error_kind", length = 80)
	private String errorKind;

	@Column(name = "error_summary", length = 1000)
	private String errorSummary;

	@Column(name = "findings_count", nullable = false)
	private int findingsCount;

	@Column(name = "body_capture_reason", length = 40)
	private String bodyCaptureReason;

	@Column(name = "body_retained_until")
	private Instant bodyRetainedUntil;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AssistantRunEntity() {
		// for JPA
	}

	public AssistantRunEntity(UUID runId, String assistantId, String status, Instant createdAt,
			Instant updatedAt) {
		this.id = runId.toString();
		this.assistantId = assistantId;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.findingsCount = 0;
	}

	public UUID getRunId() {
		return UUID.fromString(id);
	}

	public String getId() {
		return id;
	}

	public String getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(String assistantId) {
		this.assistantId = assistantId;
	}

	public Long getAssistantUserId() {
		return assistantUserId;
	}

	public void setAssistantUserId(Long assistantUserId) {
		this.assistantUserId = assistantUserId;
	}

	public Long getTriggeredByUserId() {
		return triggeredByUserId;
	}

	public void setTriggeredByUserId(Long triggeredByUserId) {
		this.triggeredByUserId = triggeredByUserId;
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

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public Long getTargetId() {
		return targetId;
	}

	public void setTargetId(Long targetId) {
		this.targetId = targetId;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getTriggeredByUsername() {
		return triggeredByUsername;
	}

	public void setTriggeredByUsername(String triggeredByUsername) {
		this.triggeredByUsername = triggeredByUsername;
	}

	public String getAssistantUsername() {
		return assistantUsername;
	}

	public void setAssistantUsername(String assistantUsername) {
		this.assistantUsername = assistantUsername;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getAttributesJson() {
		return attributesJson;
	}

	public void setAttributesJson(String attributesJson) {
		this.attributesJson = attributesJson;
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

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public String getTemplateVersion() {
		return templateVersion;
	}

	public void setTemplateVersion(String templateVersion) {
		this.templateVersion = templateVersion;
	}

	public String getTemplateSource() {
		return templateSource;
	}

	public void setTemplateSource(String templateSource) {
		this.templateSource = templateSource;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Instant completedAt) {
		this.completedAt = completedAt;
	}

	public Long getLatencyMs() {
		return latencyMs;
	}

	public void setLatencyMs(Long latencyMs) {
		this.latencyMs = latencyMs;
	}

	public String getErrorKind() {
		return errorKind;
	}

	public void setErrorKind(String errorKind) {
		this.errorKind = errorKind;
	}

	public String getErrorSummary() {
		return errorSummary;
	}

	public void setErrorSummary(String errorSummary) {
		this.errorSummary = errorSummary;
	}

	public int getFindingsCount() {
		return findingsCount;
	}

	public void setFindingsCount(int findingsCount) {
		this.findingsCount = findingsCount;
	}

	public String getBodyCaptureReason() {
		return bodyCaptureReason;
	}

	public void setBodyCaptureReason(String bodyCaptureReason) {
		this.bodyCaptureReason = bodyCaptureReason;
	}

	public Instant getBodyRetainedUntil() {
		return bodyRetainedUntil;
	}

	public void setBodyRetainedUntil(Instant bodyRetainedUntil) {
		this.bodyRetainedUntil = bodyRetainedUntil;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
