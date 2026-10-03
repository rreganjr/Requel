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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Issue #260: JPA mapping for the {@code assistant_definitions} table (V31). Scope, context
 * providers and vocabulary are JSON text, read and validated by
 * {@link com.rreganjr.requel.assistant.core.definition.AssistantDefinitionStore}.
 */
@Entity
@Table(name = "assistant_definitions")
public class AssistantDefinitionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "definition_key", length = 120, nullable = false)
	private String definitionKey;

	@Column(name = "display_name", length = 200, nullable = false)
	private String displayName;

	@Column(name = "kind", length = 20, nullable = false)
	private String kind;

	@Column(name = "task_type", length = 80, nullable = false)
	private String taskType;

	@Column(name = "scope_json", nullable = false, columnDefinition = "TEXT")
	private String scopeJson;

	@Column(name = "context_providers_json", nullable = false, columnDefinition = "TEXT")
	private String contextProvidersJson;

	/** #261: per-provider character shares, a JSON object; null = defaults. */
	@Column(name = "context_budgets_json", columnDefinition = "TEXT")
	private String contextBudgetsJson;

	@Column(name = "instructions", nullable = false, columnDefinition = "TEXT")
	private String instructions;

	/** #265: never send this definition's work to a remote provider. */
	@Column(name = "local_only", nullable = false)
	private boolean localOnly;

	@Column(name = "vocabulary_json", nullable = false, columnDefinition = "TEXT")
	private String vocabularyJson;

	@Column(name = "output_schema_name", length = 120, nullable = false)
	private String outputSchemaName;

	@Column(name = "output_schema_version", length = 40, nullable = false)
	private String outputSchemaVersion;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	@Column(name = "definition_version", nullable = false)
	private int definitionVersion;

	@Column(name = "source", length = 20, nullable = false)
	private String source;

	@Column(name = "project_id")
	private Long projectId;

	@Column(name = "forked_from_version")
	private Integer forkedFromVersion;

	@Column(name = "executor_bean", length = 200)
	private String executorBean;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "created_by", length = 255)
	private String createdBy;

	@Column(name = "updated_by", length = 255)
	private String updatedBy;

	protected AssistantDefinitionEntity() {
	}

	public AssistantDefinitionEntity(String definitionKey, Long projectId, Instant createdAt) {
		this.definitionKey = definitionKey;
		this.projectId = projectId;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getDefinitionKey() {
		return definitionKey;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getScopeJson() {
		return scopeJson;
	}

	public void setScopeJson(String scopeJson) {
		this.scopeJson = scopeJson;
	}

	public String getContextProvidersJson() {
		return contextProvidersJson;
	}

	public void setContextProvidersJson(String contextProvidersJson) {
		this.contextProvidersJson = contextProvidersJson;
	}

	public String getContextBudgetsJson() {
		return contextBudgetsJson;
	}

	public void setContextBudgetsJson(String contextBudgetsJson) {
		this.contextBudgetsJson = contextBudgetsJson;
	}

	public boolean isLocalOnly() {
		return localOnly;
	}

	public void setLocalOnly(boolean localOnly) {
		this.localOnly = localOnly;
	}

	public String getInstructions() {
		return instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public String getVocabularyJson() {
		return vocabularyJson;
	}

	public void setVocabularyJson(String vocabularyJson) {
		this.vocabularyJson = vocabularyJson;
	}

	public String getOutputSchemaName() {
		return outputSchemaName;
	}

	public void setOutputSchemaName(String outputSchemaName) {
		this.outputSchemaName = outputSchemaName;
	}

	public String getOutputSchemaVersion() {
		return outputSchemaVersion;
	}

	public void setOutputSchemaVersion(String outputSchemaVersion) {
		this.outputSchemaVersion = outputSchemaVersion;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getDefinitionVersion() {
		return definitionVersion;
	}

	public void setDefinitionVersion(int definitionVersion) {
		this.definitionVersion = definitionVersion;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public Long getProjectId() {
		return projectId;
	}

	public Integer getForkedFromVersion() {
		return forkedFromVersion;
	}

	public void setForkedFromVersion(Integer forkedFromVersion) {
		this.forkedFromVersion = forkedFromVersion;
	}

	public String getExecutorBean() {
		return executorBean;
	}

	public void setExecutorBean(String executorBean) {
		this.executorBean = executorBean;
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

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
}
