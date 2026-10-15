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
 * Issue #272: export view of one entity's link to an {@link ExternalSourceXml}. The entity is an
 * IDREF. The recorded hashes and the entity's fingerprint travel as they are: an imported entity
 * has the same content, so they still hold.
 */
@XmlType(name = "sourceLink", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.NONE)
public class SourceLinkXml {

	private String relation;
	private String entityType;
	private Object entity;
	private String fragment;
	private String fragmentHash;
	private String sourceHashSeen;
	private String entityFingerprint;
	private String ingestedAt;

	public SourceLinkXml() {
		// for JAXB
	}

	public SourceLinkXml(String relation, String entityType, Object entity, String fragment,
			String fragmentHash, String sourceHashSeen, String entityFingerprint,
			String ingestedAt) {
		this.relation = relation;
		this.entityType = entityType;
		this.entity = entity;
		this.fragment = fragment;
		this.fragmentHash = fragmentHash;
		this.sourceHashSeen = sourceHashSeen;
		this.entityFingerprint = entityFingerprint;
		this.ingestedAt = ingestedAt;
	}

	@XmlAttribute(name = "relation")
	public String getRelation() {
		return relation;
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

	@XmlAttribute(name = "fragment")
	public String getFragment() {
		return fragment;
	}

	@XmlAttribute(name = "fragmentHash")
	public String getFragmentHash() {
		return fragmentHash;
	}

	@XmlAttribute(name = "sourceHashSeen")
	public String getSourceHashSeen() {
		return sourceHashSeen;
	}

	@XmlAttribute(name = "entityFingerprint")
	public String getEntityFingerprint() {
		return entityFingerprint;
	}

	@XmlAttribute(name = "ingestedAt")
	public String getIngestedAt() {
		return ingestedAt;
	}
}
