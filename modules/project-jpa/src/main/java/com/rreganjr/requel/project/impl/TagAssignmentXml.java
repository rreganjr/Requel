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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Export/import view of a single tag assignment in the project XML: a taggable entity, its
 * registry discriminator, and the tag as a by-name token ({@code category:value[color]}).
 *
 * <p>This is a JAXB-only carrier — it holds only strings plus an {@code @XmlIDREF} to the tagged
 * entity, so the project marshal graph never references a tag class and {@code project-jpa} stays
 * free of {@code tagging-jpa}. Populated for export by the service layer (which has the tag data)
 * and read on import by the tag StAX importer.</p>
 *
 * @author ron
 */
@XmlType(name = "tagAssignment", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.NONE)
public class TagAssignmentXml {

	private String entityType;
	private Object entity;
	private String token;

	public TagAssignmentXml() {
		// for JAXB
	}

	public TagAssignmentXml(String entityType, Object entity, String token) {
		this.entityType = entityType;
		this.entity = entity;
		this.token = token;
	}

	/** The registry discriminator of the tagged entity, e.g. {@code "Goal"}. */
	@XmlAttribute(name = "entityType")
	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	/** IDREF to the tagged entity; JAXB writes the entity's {@code @XmlID} (e.g. {@code GOL_10}). */
	@XmlIDREF
	@XmlAttribute(name = "entityRef")
	public Object getEntity() {
		return entity;
	}

	public void setEntity(Object entity) {
		this.entity = entity;
	}

	/** The tag as a by-name token: {@code category:value[color]}. */
	@XmlAttribute(name = "token")
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
