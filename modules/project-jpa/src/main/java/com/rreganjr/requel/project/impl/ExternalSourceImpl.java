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
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.SourceLocatorType;
import com.rreganjr.requel.user.impl.UserImpl;

/**
 * Issue #272: a row of {@code external_sources} (Flyway V26). The project is referred to by id,
 * like {@code ignored_findings}; {@code DeleteProjectCommandImpl} removes the rows.
 */
@Entity
@Table(name = "external_sources",
		uniqueConstraints = @UniqueConstraint(name = "uq_external_sources",
				columnNames = { "project_id", "source_system", "external_id" }),
		indexes = @Index(name = "idx_external_sources_project", columnList = "project_id"))
public class ExternalSourceImpl implements ExternalSource {

	private Long id;
	private Long projectId;
	private String system;
	private String externalId;
	private SourceLocatorType locatorType;
	private String locator;
	private String title;
	private String kind;
	private String contentHash;
	private Date lastIngestedAt;
	private User createdBy;
	private Date dateCreated;

	public ExternalSourceImpl(Long projectId, String system, String externalId, User createdBy) {
		this.projectId = projectId;
		this.system = system;
		this.externalId = externalId;
		this.createdBy = createdBy;
		this.dateCreated = new Date();
	}

	protected ExternalSourceImpl() {
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
	@Column(name = "source_system", nullable = false, length = 40)
	public String getSystem() {
		return system;
	}

	protected void setSystem(String system) {
		this.system = system;
	}

	@Override
	@Column(name = "external_id", nullable = false, length = 255)
	public String getExternalId() {
		return externalId;
	}

	protected void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@Override
	@Enumerated(EnumType.STRING)
	@Column(name = "locator_type", nullable = true, length = 20)
	public SourceLocatorType getLocatorType() {
		return locatorType;
	}

	public void setLocatorType(SourceLocatorType locatorType) {
		this.locatorType = locatorType;
	}

	@Override
	@Column(name = "locator", nullable = true, length = 2048)
	public String getLocator() {
		return locator;
	}

	public void setLocator(String locator) {
		this.locator = locator;
	}

	@Override
	@Column(name = "title", nullable = true, length = 255)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	@Column(name = "kind", nullable = true, length = 40)
	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	@Override
	@Column(name = "content_hash", nullable = true, length = 128)
	public String getContentHash() {
		return contentHash;
	}

	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}

	@Override
	@Column(name = "last_ingested_at", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getLastIngestedAt() {
		return lastIngestedAt;
	}

	public void setLastIngestedAt(Date lastIngestedAt) {
		this.lastIngestedAt = lastIngestedAt;
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
