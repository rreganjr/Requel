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
 * JPA mapping for the {@code idea_drafts} table: one proposed requirement entity generated from an
 * idea. {@code evidenceJson} and {@code referencesJson} are JSON lists, always read with the row.
 */
@Entity
@Table(name = "idea_drafts")
public class IdeaDraftEntity {

	@Id
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private String id;

	@Column(name = "idea_id", length = 36, nullable = false, updatable = false)
	private String ideaId;

	@Column(name = "sort_order", nullable = false, updatable = false)
	private int sortOrder;

	@Column(name = "kind", length = 20, nullable = false)
	private String kind;

	@Column(name = "name", length = 255, nullable = false)
	private String name;

	@Column(name = "draft_text", columnDefinition = "LONGTEXT", nullable = false)
	private String draftText;

	@Column(name = "note", columnDefinition = "LONGTEXT")
	private String note;

	@Column(name = "assumption", nullable = false)
	private boolean assumption;

	@Column(name = "evidence_json", columnDefinition = "LONGTEXT")
	private String evidenceJson;

	@Column(name = "evidence_unverified", nullable = false)
	private boolean evidenceUnverified;

	@Column(name = "confidence")
	private Double confidence;

	@Column(name = "references_json", columnDefinition = "LONGTEXT")
	private String referencesJson;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected IdeaDraftEntity() {
		// for JPA
	}

	public IdeaDraftEntity(UUID id, String ideaId, int sortOrder, String kind, String name,
			String draftText, String note, boolean assumption, String evidenceJson,
			boolean evidenceUnverified, Double confidence, String referencesJson,
			Instant createdAt) {
		this.id = id.toString();
		this.ideaId = ideaId;
		this.sortOrder = sortOrder;
		this.kind = kind;
		this.name = name;
		this.draftText = draftText;
		this.note = note;
		this.assumption = assumption;
		this.evidenceJson = evidenceJson;
		this.evidenceUnverified = evidenceUnverified;
		this.confidence = confidence;
		this.referencesJson = referencesJson;
		this.createdAt = createdAt;
	}

	public String getId() {
		return id;
	}

	public String getIdeaId() {
		return ideaId;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public String getKind() {
		return kind;
	}

	public String getName() {
		return name;
	}

	public String getDraftText() {
		return draftText;
	}

	public String getNote() {
		return note;
	}

	public boolean isAssumption() {
		return assumption;
	}

	public String getEvidenceJson() {
		return evidenceJson;
	}

	public boolean isEvidenceUnverified() {
		return evidenceUnverified;
	}

	public Double getConfidence() {
		return confidence;
	}

	public String getReferencesJson() {
		return referencesJson;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
