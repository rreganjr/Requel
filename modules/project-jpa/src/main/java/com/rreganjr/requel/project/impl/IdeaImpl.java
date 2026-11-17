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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.Idea;
import com.rreganjr.requel.user.impl.UserImpl;

/**
 * A row of {@code ideas} (Flyway V39). No foreign keys, like {@code ignored_findings}: the
 * project is referred to by id and the project delete path removes its ideas explicitly.
 */
@Entity
@Table(name = "ideas", indexes = @Index(name = "idx_ideas_project", columnList = "project_id"))
public class IdeaImpl implements Idea {

	private Long id;
	private Long projectId;
	private String text;
	private String sourceFileName;
	private Status status = Status.SUBMITTED;
	private User createdBy;
	private Date dateCreated;

	public IdeaImpl(Long projectId, String text, String sourceFileName, User createdBy) {
		this.projectId = projectId;
		this.text = text;
		this.sourceFileName = sourceFileName;
		this.createdBy = createdBy;
		this.dateCreated = new Date();
	}

	protected IdeaImpl() {
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
	@Lob
	@Column(name = "idea_text", nullable = false)
	public String getText() {
		return text;
	}

	protected void setText(String text) {
		this.text = text;
	}

	@Override
	@Column(name = "source_file_name", length = 255)
	public String getSourceFileName() {
		return sourceFileName;
	}

	protected void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}

	@Override
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	public Status getStatus() {
		return status;
	}

	protected void setStatus(Status status) {
		this.status = status;
	}

	@Override
	@ManyToOne(targetEntity = UserImpl.class)
	@JoinColumn(name = "created_by_id")
	public User getCreatedBy() {
		return createdBy;
	}

	protected void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	@Override
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date_created")
	public Date getDateCreated() {
		return dateCreated;
	}

	protected void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
}
