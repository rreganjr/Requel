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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Issue #320: export view of one ignored finding. Like {@link TagAssignmentXml} it is a JAXB-only
 * carrier: strings plus IDREFs to the entity and to the resolved issue, filled for export from the
 * {@code IgnoredFindingStore} and read on import by the ignored-finding StAX importer, which
 * rebuilds the key for the entity's new id.
 */
@XmlType(name = "ignoredFinding", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.NONE)
public class IgnoredFindingXml {

	private String entityType;
	private Object entity;
	private String assistant;
	private String findingType;
	private String property;
	private String keySuffix;
	private String subject;
	private Object annotation;
	private String createdBy;
	private String dateCreated;

	public IgnoredFindingXml() {
		// for JAXB
	}

	public IgnoredFindingXml(String entityType, Object entity, String assistant,
			String findingType, String property, String keySuffix, String subject,
			Object annotation, String createdBy, String dateCreated) {
		this.entityType = entityType;
		this.entity = entity;
		this.assistant = assistant;
		this.findingType = findingType;
		this.property = property;
		this.keySuffix = keySuffix;
		this.subject = subject;
		this.annotation = annotation;
		this.createdBy = createdBy;
		this.dateCreated = dateCreated;
	}

	@XmlAttribute(name = "entityType")
	public String getEntityType() {
		return entityType;
	}

	@XmlIDREF
	@XmlAttribute(name = "entityRef")
	public Object getEntity() {
		return entity;
	}

	@XmlAttribute(name = "assistant")
	public String getAssistant() {
		return assistant;
	}

	@XmlAttribute(name = "findingType")
	public String getFindingType() {
		return findingType;
	}

	@XmlAttribute(name = "property")
	public String getProperty() {
		return property;
	}

	@XmlAttribute(name = "keySuffix")
	public String getKeySuffix() {
		return keySuffix;
	}

	@XmlAttribute(name = "subject")
	public String getSubject() {
		return subject;
	}

	/** IDREF to the resolved issue the ignore came from; absent when it is gone. */
	@XmlIDREF
	@XmlAttribute(name = "annotationRef")
	public Object getAnnotation() {
		return annotation;
	}

	/** The username of whoever ignored it. */
	@XmlAttribute(name = "createdBy")
	public String getCreatedBy() {
		return createdBy;
	}

	@XmlAttribute(name = "dateCreated")
	public String getDateCreated() {
		return dateCreated;
	}
}
