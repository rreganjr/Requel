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

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Issue #272: import view of one exported external source and its entity links, read by
 * {@link ExternalSourceStaxImporter}. References stay strings; the project importer resolves them.
 */
@XmlRootElement(name = "externalSource", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExternalSourceImportXml {

	@XmlAttribute(name = "system")
	private String system;

	@XmlAttribute(name = "externalId")
	private String externalId;

	@XmlAttribute(name = "locatorType")
	private String locatorType;

	@XmlAttribute(name = "locator")
	private String locator;

	@XmlAttribute(name = "title")
	private String title;

	@XmlAttribute(name = "kind")
	private String kind;

	@XmlAttribute(name = "contentHash")
	private String contentHash;

	@XmlAttribute(name = "lastIngestedAt")
	private String lastIngestedAt;

	@XmlElement(name = "sourceLink", namespace = "http://www.rreganjr.com/requel")
	private List<Link> links = new ArrayList<>();

	public String getSystem() { return system; }
	public String getExternalId() { return externalId; }
	public String getLocatorType() { return locatorType; }
	public String getLocator() { return locator; }
	public String getTitle() { return title; }
	public String getKind() { return kind; }
	public String getContentHash() { return contentHash; }
	public String getLastIngestedAt() { return lastIngestedAt; }
	public List<Link> getLinks() { return links; }

	/** One exported link. */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Link {

		@XmlAttribute(name = "relation")
		private String relation;

		@XmlAttribute(name = "entityType")
		private String entityType;

		@XmlAttribute(name = "entityRef")
		private String entityRef;

		@XmlAttribute(name = "fragment")
		private String fragment;

		@XmlAttribute(name = "fragmentHash")
		private String fragmentHash;

		@XmlAttribute(name = "sourceHashSeen")
		private String sourceHashSeen;

		@XmlAttribute(name = "entityFingerprint")
		private String entityFingerprint;

		@XmlAttribute(name = "ingestedAt")
		private String ingestedAt;

		public String getRelation() { return relation; }
		public String getEntityType() { return entityType; }
		public String getEntityRef() { return entityRef; }
		public String getFragment() { return fragment; }
		public String getFragmentHash() { return fragmentHash; }
		public String getSourceHashSeen() { return sourceHashSeen; }
		public String getEntityFingerprint() { return entityFingerprint; }
		public String getIngestedAt() { return ingestedAt; }
	}
}
