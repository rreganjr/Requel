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

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import com.rreganjr.requel.project.IgnoredFinding;
import com.rreganjr.requel.user.impl.UserImpl;

/**
 * Issue #320: a row of {@code ignored_findings} (Flyway V21). No foreign keys, like
 * {@code assistant_findings}: the project, the entity and the issue are referred to by id, and
 * the project and entity delete paths remove their rows explicitly.
 */
@Entity
@Table(name = "ignored_findings",
		uniqueConstraints = @UniqueConstraint(name = "uq_ignored_findings_key", columnNames = "key_lower"),
		indexes = { @Index(name = "idx_ignored_findings_project", columnList = "project_id"),
				@Index(name = "idx_ignored_findings_target", columnList = "target_type, target_id") })
public class IgnoredFindingImpl implements IgnoredFinding {

	private Long id;
	private Long projectId;
	private String targetType;
	private Long targetId;
	private String assistantId;
	private String findingType;
	private String propertyName;
	private String keySuffix;
	private String keyLower;
	private String subject;
	private Long annotationId;
	private User createdBy;
	private Date dateCreated;

	public IgnoredFindingImpl(Long projectId, String targetType, Long targetId,
			String assistantId, String findingType, String propertyName, String keySuffix,
			String subject, Long annotationId, User createdBy) {
		this.projectId = projectId;
		this.targetType = targetType;
		this.targetId = targetId;
		this.assistantId = assistantId;
		this.findingType = findingType;
		this.propertyName = propertyName;
		this.keySuffix = keySuffix;
		this.keyLower = IgnoredFinding.idempotencyKey(assistantId, targetType, targetId, keySuffix)
				.toLowerCase(java.util.Locale.ROOT);
		this.subject = subject;
		this.annotationId = annotationId;
		this.createdBy = createdBy;
		this.dateCreated = new Date();
	}

	protected IgnoredFindingImpl() {
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
	@Column(name = "project_id", nullable = false)
	public Long getProjectId() {
		return projectId;
	}

	protected void setProjectId(Long projectId) {
		this.projectId = projectId;
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
	@Column(name = "assistant_id", nullable = false, length = 200)
	public String getAssistantId() {
		return assistantId;
	}

	protected void setAssistantId(String assistantId) {
		this.assistantId = assistantId;
	}

	@Override
	@Column(name = "finding_type", nullable = false, length = 120)
	public String getFindingType() {
		return findingType;
	}

	protected void setFindingType(String findingType) {
		this.findingType = findingType;
	}

	@Override
	@Column(name = "property_name", nullable = true, length = 255)
	public String getPropertyName() {
		return propertyName;
	}

	protected void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	@Column(name = "key_suffix", nullable = false, length = 255)
	public String getKeySuffix() {
		return keySuffix;
	}

	protected void setKeySuffix(String keySuffix) {
		this.keySuffix = keySuffix;
	}

	/** The full idempotency key, lower-cased: the column the uniqueness and the lookups use. */
	@Column(name = "key_lower", nullable = false, length = 255)
	public String getKeyLower() {
		return keyLower;
	}

	protected void setKeyLower(String keyLower) {
		this.keyLower = keyLower;
	}

	@Override
	@Column(name = "subject", nullable = true, length = 500)
	public String getSubject() {
		return subject;
	}

	protected void setSubject(String subject) {
		this.subject = subject;
	}

	@Override
	@Column(name = "annotation_id", nullable = true)
	public Long getAnnotationId() {
		return annotationId;
	}

	protected void setAnnotationId(Long annotationId) {
		this.annotationId = annotationId;
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

	@Override
	@Column(name = "date_created", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDateCreated() {
		return dateCreated;
	}

	protected void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
}
