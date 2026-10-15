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
import jakarta.xml.bind.annotation.XmlType;

/**
 * Issue #273: export view of one "defers to" edge, nested in the subordinate's
 * {@link ExternalSourceXml}. The superior is named by system and external id, which identify a
 * source within a project.
 */
@XmlType(name = "sourceAuthority", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.NONE)
public class SourceAuthorityXml {

	private String system;
	private String externalId;
	private String note;

	public SourceAuthorityXml() {
		// for JAXB
	}

	public SourceAuthorityXml(String system, String externalId, String note) {
		this.system = system;
		this.externalId = externalId;
		this.note = note;
	}

	@XmlAttribute(name = "system")
	public String getSystem() {
		return system;
	}

	@XmlAttribute(name = "externalId")
	public String getExternalId() {
		return externalId;
	}

	@XmlAttribute(name = "note")
	public String getNote() {
		return note;
	}
}
