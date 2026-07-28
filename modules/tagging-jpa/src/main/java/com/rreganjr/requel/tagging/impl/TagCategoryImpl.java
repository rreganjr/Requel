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
package com.rreganjr.requel.tagging.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.tagging.TagCategory;
import com.rreganjr.requel.user.impl.UserImpl;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * JPA implementation of {@link TagCategory} — the optional rules overlay for a tag category.
 *
 * @author ron
 */
@Entity
@Table(name = "tag_category",
		uniqueConstraints = { @UniqueConstraint(columnNames = { "project_id", "name" }) })
public class TagCategoryImpl implements TagCategory, Serializable {

	static final long serialVersionUID = 0L;

	private Long id;
	private int version = 1;
	private Long projectId;
	private String name;
	private boolean exclusive;
	private String color;
	private User createdBy;
	private Date dateCreated = new Date();
	private Set<String> allowedEntityTypes = new HashSet<>();
	private Set<String> values = new HashSet<>();

	protected TagCategoryImpl() {
		// for hibernate
	}

	public TagCategoryImpl(String name, Long projectId, boolean exclusive, User createdBy) {
		this.name = name;
		this.projectId = projectId;
		this.exclusive = exclusive;
		this.createdBy = createdBy;
		this.dateCreated = new Date();
	}

	@Override
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Override
	@Version
	public int getVersion() {
		return version;
	}

	protected void setVersion(int version) {
		this.version = version;
	}

	@Override
	@Column(name = "project_id")
	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	@Override
	@Column(name = "name", nullable = false, length = 255)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	@Column(name = "exclusive", nullable = false)
	public boolean isExclusive() {
		return exclusive;
	}

	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	@Override
	@Column(name = "color", length = 16)
	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@Override
	@ManyToOne(targetEntity = UserImpl.class, cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinColumn(name = "created_by_id")
	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	@Override
	@Column(name = "date_created", updatable = false)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDateCreated() {
		return dateCreated;
	}

	protected void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	@Override
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "tag_category_allowed_type",
			joinColumns = @JoinColumn(name = "tag_category_id"))
	@Column(name = "entity_type", length = 255)
	public Set<String> getAllowedEntityTypes() {
		return allowedEntityTypes;
	}

	public void setAllowedEntityTypes(Set<String> allowedEntityTypes) {
		this.allowedEntityTypes = allowedEntityTypes;
	}

	@Override
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "tag_category_value",
			joinColumns = @JoinColumn(name = "tag_category_id"))
	@Column(name = "tag_value", length = 255)
	public Set<String> getValues() {
		return values;
	}

	public void setValues(Set<String> values) {
		this.values = values;
	}
}
