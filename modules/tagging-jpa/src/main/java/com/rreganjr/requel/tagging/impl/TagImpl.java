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

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.Taggable;
import com.rreganjr.requel.user.impl.UserImpl;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

/**
 * JPA implementation of {@link Tag}. The polymorphic assignment to project entities
 * reuses the decoupled {@code @ManyToAny} seam proven by
 * {@code com.rreganjr.requel.annotation.impl.AbstractAnnotation}: the discriminator
 * &rarr; class map is contributed at Hibernate bootstrap by the project module's
 * metadata contributor rather than inline {@code @AnyDiscriminatorValue} entries, so
 * this module has no dependency on {@code project-jpa} impl classes.
 *
 * @author ron
 */
@Entity
@Table(name = "tag")
// The (project_id, category, value) uniqueness is enforced by the V13 Flyway migration
// (prod) and by the Phase 2 command layer. It is intentionally NOT declared here as a
// @UniqueConstraint: `value` is a quoted column name and an inline table-level constraint
// referencing it does not round-trip cleanly through Hibernate's create-drop DDL.
public class TagImpl implements Tag, Serializable {

	static final long serialVersionUID = 0L;

	private Long id;
	private int version = 1; // start at 1 so hibernate recognizes the new instance as initial, not stale.
	private String category;
	private String value;
	private Long projectId;
	private String color;
	private User createdBy;
	private Date dateCreated = new Date();
	private Set<Taggable> taggables = new HashSet<>();

	protected TagImpl() {
		// for hibernate
	}

	public TagImpl(String category, String value, Long projectId, User createdBy) {
		setCategory(category);
		setValue(value);
		setProjectId(projectId);
		setCreatedBy(createdBy);
		setDateCreated(new Date());
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
	@Column(name = "category", length = 255)
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	@Column(name = "`value`", length = 255, nullable = false)
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
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

	/**
	 * The polymorphic set of entities this tag is assigned to. The
	 * {@code taggable_type} discriminator values are injected at Hibernate boot by the
	 * project module's metadata contributor; there are deliberately no inline
	 * {@code @AnyDiscriminatorValue} entries here.
	 */
	@Override
	@Column(name = "taggable_type", length = 255, nullable = false)
	@ManyToAny(fetch = FetchType.LAZY)
	@AnyDiscriminator(DiscriminatorType.STRING)
	@AnyKeyJavaClass(Long.class)
	@JoinTable(name = "tag_taggable",
			joinColumns = @JoinColumn(name = "tag_id"),
			inverseJoinColumns = @JoinColumn(name = "taggable_id"))
	public Set<Taggable> getTaggables() {
		return taggables;
	}

	protected void setTaggables(Set<Taggable> taggables) {
		this.taggables = taggables;
	}

	@Override
	@Transient
	public String getDescription() {
		return (getCategory() == null) ? getValue() : (getCategory() + "=" + getValue());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		if (getId() != null) {
			return getId().hashCode();
		}
		int result = 1;
		result = prime * result + ((getCategory() == null) ? 0 : getCategory().hashCode());
		result = prime * result + ((getValue() == null) ? 0 : getValue().hashCode());
		result = prime * result + ((getProjectId() == null) ? 0 : getProjectId().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || !(obj instanceof TagImpl)) {
			return false;
		}
		final TagImpl other = (TagImpl) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if ((getId() != null) || (other.getId() != null)) {
			return false;
		}
		return equalsNullable(getCategory(), other.getCategory())
				&& equalsNullable(getValue(), other.getValue())
				&& equalsNullable(getProjectId(), other.getProjectId());
	}

	private static boolean equalsNullable(Object a, Object b) {
		return (a == null) ? (b == null) : a.equals(b);
	}

	@Override
	public String toString() {
		return "TagImpl[id=" + id + ", " + getDescription() + ", projectId=" + projectId + "]";
	}
}
