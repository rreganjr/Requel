/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Issue #268: whether an assistant runs in a project. No foreign keys, like
 * {@code ignored_findings}: the project delete path removes the rows.
 */
@Entity
@Table(name = "project_assistant_settings")
public class ProjectAssistantSettingImpl {

	/** The key: project and assistant. */
	@Embeddable
	public static class Key implements Serializable {
		private static final long serialVersionUID = 1L;

		@Column(name = "project_id", nullable = false)
		private Long projectId;

		@Column(name = "assistant_id", nullable = false, length = 200)
		private String assistantId;

		protected Key() {
			// for hibernate
		}

		public Key(Long projectId, String assistantId) {
			this.projectId = projectId;
			this.assistantId = assistantId;
		}

		public Long getProjectId() {
			return projectId;
		}

		public String getAssistantId() {
			return assistantId;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof Key key && Objects.equals(projectId, key.projectId)
					&& Objects.equals(assistantId, key.assistantId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(projectId, assistantId);
		}
	}

	@EmbeddedId
	private Key key;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	/** The user who last changed the setting; a soft reference. */
	@Column(name = "updated_by_id")
	private Long updatedById;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date_updated")
	private Date dateUpdated;

	protected ProjectAssistantSettingImpl() {
		// for hibernate
	}

	public ProjectAssistantSettingImpl(Long projectId, String assistantId) {
		this.key = new Key(projectId, assistantId);
		this.enabled = true;
	}

	public Key getKey() {
		return key;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Long getUpdatedById() {
		return updatedById;
	}

	public Date getDateUpdated() {
		return dateUpdated;
	}

	/** Record a change of the setting. */
	public void update(boolean enabled, Long updatedById) {
		this.enabled = enabled;
		this.updatedById = updatedById;
		this.dateUpdated = new Date();
	}
}
