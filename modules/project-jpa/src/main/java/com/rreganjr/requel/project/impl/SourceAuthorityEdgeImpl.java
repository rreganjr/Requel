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
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.SourceAuthorityEdge;
import com.rreganjr.requel.user.impl.UserImpl;

/**
 * Issue #273: a row of {@code source_authority} (Flyway V27): the subordinate source defers to
 * the superior one. Both sides are sources of the same project; the store deletes the edges of a
 * deleted source or project explicitly (the MySQL foreign keys also cascade).
 */
@Entity
@Table(name = "source_authority",
		uniqueConstraints = @UniqueConstraint(name = "uq_source_authority",
				columnNames = { "subordinate_id", "superior_id" }),
		indexes = { @Index(name = "idx_sa_superior", columnList = "superior_id"),
				@Index(name = "idx_sa_project", columnList = "project_id") })
public class SourceAuthorityEdgeImpl implements SourceAuthorityEdge {

	private Long id;
	private Long projectId;
	private ExternalSource subordinate;
	private ExternalSource superior;
	private String note;
	private User createdBy;
	private Date dateCreated;

	public SourceAuthorityEdgeImpl(ExternalSourceImpl subordinate, ExternalSourceImpl superior,
			User createdBy) {
		this.projectId = subordinate.getProjectId();
		this.subordinate = subordinate;
		this.superior = superior;
		this.createdBy = createdBy;
		this.dateCreated = new Date();
	}

	protected SourceAuthorityEdgeImpl() {
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
	@ManyToOne(targetEntity = ExternalSourceImpl.class, optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "subordinate_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_sa_subordinate"))
	public ExternalSource getSubordinate() {
		return subordinate;
	}

	protected void setSubordinate(ExternalSource subordinate) {
		this.subordinate = subordinate;
	}

	@Override
	@ManyToOne(targetEntity = ExternalSourceImpl.class, optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "superior_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_sa_superior"))
	public ExternalSource getSuperior() {
		return superior;
	}

	protected void setSuperior(ExternalSource superior) {
		this.superior = superior;
	}

	@Override
	@Column(name = "note", nullable = true, length = 1000)
	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
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
