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
package com.rreganjr.requel.project.impl;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.EntitySourceLink;
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.ProvenanceStore;
import com.rreganjr.requel.project.SourceLinkRelation;
import com.rreganjr.requel.user.impl.UserImpl;

/**
 * Issue #272: a row of {@code entity_source_links} (Flyway V26). The target entity is a soft
 * reference (type + id), like {@code ignored_findings}; the entity delete path removes the rows.
 * {@code fragment_key} is the fragment or "" so the unique key also covers whole-source links.
 */
@Entity
@Table(name = "entity_source_links",
		uniqueConstraints = @UniqueConstraint(name = "uq_entity_source_links",
				columnNames = { "source_id", "relation", "target_type", "target_id", "fragment_key" }),
		indexes = { @Index(name = "idx_esl_target", columnList = "target_type, target_id"),
				@Index(name = "idx_esl_fragment", columnList = "source_id, fragment_key"),
				@Index(name = "idx_esl_project", columnList = "project_id") })
public class EntitySourceLinkImpl implements EntitySourceLink {

	private Long id;
	private ExternalSource source;
	private Long projectId;
	private SourceLinkRelation relation;
	private String targetType;
	private Long targetId;
	private String fragment;
	private String fragmentKey;
	private String fragmentHash;
	private String sourceHashSeen;
	private String entityFingerprint;
	private Date ingestedAt;
	private User createdBy;

	public EntitySourceLinkImpl(ExternalSource source, SourceLinkRelation relation,
			String targetType, Long targetId, String fragment, User createdBy) {
		this.source = source;
		this.projectId = source.getProjectId();
		this.relation = relation;
		this.targetType = targetType;
		this.targetId = targetId;
		this.fragment = ProvenanceStore.normalizeFragment(fragment);
		this.fragmentKey = ProvenanceStore.fragmentKey(fragment);
		this.createdBy = createdBy;
	}

	protected EntitySourceLinkImpl() {
		// for hibernate
	}

	@Override
	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Override
	@ManyToOne(targetEntity = ExternalSourceImpl.class, optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "source_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_esl_source"))
	public ExternalSource getSource() {
		return source;
	}

	protected void setSource(ExternalSource source) {
		this.source = source;
	}

	@Override
	@Column(name = "project_id", nullable = false)
	public Long getProjectId() {
		return projectId;
	}

	protected void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	@Override
	@Enumerated(EnumType.STRING)
	@Column(name = "relation", nullable = false, length = 20)
	public SourceLinkRelation getRelation() {
		return relation;
	}

	protected void setRelation(SourceLinkRelation relation) {
		this.relation = relation;
	}

	@Override
	@Column(name = "target_type", nullable = false, length = 80)
	public String getTargetType() {
		return targetType;
	}

	protected void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	@Override
	@Column(name = "target_id", nullable = false)
	public Long getTargetId() {
		return targetId;
	}

	protected void setTargetId(Long targetId) {
		this.targetId = targetId;
	}

	@Override
	@Column(name = "fragment", nullable = true, length = 255)
	public String getFragment() {
		return fragment;
	}

	protected void setFragment(String fragment) {
		this.fragment = fragment;
	}

	@Column(name = "fragment_key", nullable = false, length = 255)
	public String getFragmentKey() {
		return fragmentKey;
	}

	protected void setFragmentKey(String fragmentKey) {
		this.fragmentKey = fragmentKey;
	}

	@Override
	@Column(name = "fragment_hash", nullable = true, length = 64)
	public String getFragmentHash() {
		return fragmentHash;
	}

	public void setFragmentHash(String fragmentHash) {
		this.fragmentHash = fragmentHash;
	}

	@Override
	@Column(name = "source_hash_seen", nullable = true, length = 128)
	public String getSourceHashSeen() {
		return sourceHashSeen;
	}

	public void setSourceHashSeen(String sourceHashSeen) {
		this.sourceHashSeen = sourceHashSeen;
	}

	@Override
	@Column(name = "entity_fingerprint", nullable = true, length = 64)
	public String getEntityFingerprint() {
		return entityFingerprint;
	}

	public void setEntityFingerprint(String entityFingerprint) {
		this.entityFingerprint = entityFingerprint;
	}

	@Override
	@Column(name = "ingested_at", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getIngestedAt() {
		return ingestedAt;
	}

	public void setIngestedAt(Date ingestedAt) {
		this.ingestedAt = ingestedAt;
	}

	// #323: no cascade up to the user.
	@Override
	@ManyToOne(targetEntity = UserImpl.class, optional = true)
	@JoinColumn(name = "created_by_id", nullable = true)
	public User getCreatedBy() {
		return createdBy;
	}

	protected void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}
}
