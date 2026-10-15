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
package com.rreganjr.requel.utils.jaxb.imports;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Import view of a single {@code <tagAssignment>} element. Holds only strings — the entity
 * discriminator, the entity's XML-id reference (resolved to the imported entity by the project
 * import), and the by-name tag token. On import {@code entityRef} is read as a plain string (not an
 * IDREF), since resolution happens against the import unit-of-work, not JAXB.
 *
 * @author ron
 */
@XmlRootElement(name = "tagAssignment", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.NONE)
public class TagAssignmentImportXml {

	private String entityType;
	private String entityRef;
	private String token;

	@XmlAttribute(name = "entityType")
	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	@XmlAttribute(name = "entityRef")
	public String getEntityRef() {
		return entityRef;
	}

	public void setEntityRef(String entityRef) {
		this.entityRef = entityRef;
	}

	@XmlAttribute(name = "token")
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
