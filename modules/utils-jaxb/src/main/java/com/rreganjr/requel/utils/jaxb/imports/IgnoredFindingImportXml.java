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
package com.rreganjr.requel.utils.jaxb.imports;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Issue #320: import view of one {@code <ignoredFinding>}. Strings only; {@code entityRef} and
 * {@code annotationRef} are the XML ids, resolved against the import unit of work.
 */
@XmlRootElement(name = "ignoredFinding", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class IgnoredFindingImportXml {

	@XmlAttribute(name = "entityType")
	private String entityType;

	@XmlAttribute(name = "entityRef")
	private String entityRef;

	@XmlAttribute(name = "assistant")
	private String assistant;

	@XmlAttribute(name = "findingType")
	private String findingType;

	@XmlAttribute(name = "property")
	private String property;

	@XmlAttribute(name = "keySuffix")
	private String keySuffix;

	@XmlAttribute(name = "subject")
	private String subject;

	@XmlAttribute(name = "annotationRef")
	private String annotationRef;

	@XmlAttribute(name = "createdBy")
	private String createdBy;

	public String getEntityType() { return entityType; }
	public String getEntityRef() { return entityRef; }
	public String getAssistant() { return assistant; }
	public String getFindingType() { return findingType; }
	public String getProperty() { return property; }
	public String getKeySuffix() { return keySuffix; }
	public String getSubject() { return subject; }
	public String getAnnotationRef() { return annotationRef; }
	public String getCreatedBy() { return createdBy; }
}
