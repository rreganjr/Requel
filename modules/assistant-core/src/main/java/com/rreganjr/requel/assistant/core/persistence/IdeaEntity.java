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
 * JPA mapping for the {@code idea_generations} table: a product manager's free-text idea for a project, the
 * state of generating requirement drafts from it, and the last error. Soft references (no FK) to
 * the project and user.
 */
@Entity
@Table(name = "idea_generations")
public class IdeaEntity {

	@Id
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private String id;

	@Column(name = "project_id", nullable = false, updatable = false)
	private Long projectId;

	@Column(name = "created_by_user_id", updatable = false)
	private Long createdByUserId;

	@Column(name = "created_by_username", length = 255, updatable = false)
	private String createdByUsername;

	@Column(name = "idea_text", columnDefinition = "LONGTEXT", nullable = false, updatable = false)
	private String ideaText;

	/** The project's goal, actor and term names when the idea was submitted, as JSON. */
	@Column(name = "existing_names_json", columnDefinition = "LONGTEXT", updatable = false)
	private String existingNamesJson;

	@Column(name = "status", length = 20, nullable = false)
	private String status;

	@Column(name = "attempts", nullable = false)
	private int attempts;

	/** The run id of the latest attempt; a result from any other attempt is discarded. */
	@Column(name = "last_run_id", length = 36)
	private String lastRunId;

	@Column(name = "error_summary", length = 1000)
	private String errorSummary;

	@Column(name = "result_summary", length = 2000)
	private String resultSummary;

	@Column(name = "messages_json", columnDefinition = "LONGTEXT")
	private String messagesJson;

	@Column(name = "problems_json", columnDefinition = "LONGTEXT")
	private String problemsJson;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected IdeaEntity() {
		// for JPA
	}

	public IdeaEntity(UUID id, Long projectId, Long createdByUserId, String createdByUsername,
			String ideaText, String existingNamesJson, Instant now) {
		this.id = id.toString();
		this.projectId = projectId;
		this.createdByUserId = createdByUserId;
		this.createdByUsername = createdByUsername;
		this.ideaText = ideaText;
		this.existingNamesJson = existingNamesJson;
		this.status = IdeaStatus.PENDING.name();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public String getId() {
		return id;
	}

	public Long getProjectId() {
		return projectId;
	}

	public Long getCreatedByUserId() {
		return createdByUserId;
	}

	public String getCreatedByUsername() {
		return createdByUsername;
	}

	public String getIdeaText() {
		return ideaText;
	}

	public String getExistingNamesJson() {
		return existingNamesJson;
	}

	public String getStatus() {
		return status;
	}

	public IdeaStatus getIdeaStatus() {
		return IdeaStatus.valueOf(status);
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public String getLastRunId() {
		return lastRunId;
	}

	public void setLastRunId(String lastRunId) {
		this.lastRunId = lastRunId;
	}

	public String getErrorSummary() {
		return errorSummary;
	}

	public void setErrorSummary(String errorSummary) {
		this.errorSummary = errorSummary;
	}

	public String getResultSummary() {
		return resultSummary;
	}

	public void setResultSummary(String resultSummary) {
		this.resultSummary = resultSummary;
	}

	public String getMessagesJson() {
		return messagesJson;
	}

	public void setMessagesJson(String messagesJson) {
		this.messagesJson = messagesJson;
	}

	public String getProblemsJson() {
		return problemsJson;
	}

	public void setProblemsJson(String problemsJson) {
		this.problemsJson = problemsJson;
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
