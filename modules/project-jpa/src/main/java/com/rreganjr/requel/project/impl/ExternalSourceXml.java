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

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Issue #272: export view of one external source and its entity links. A JAXB-only carrier,
 * like {@link IgnoredFindingXml}: filled for export from the {@code ProvenanceStore} and read on
 * import by the external-source StAX importer. The file never carries a database id.
 */
@XmlType(name = "externalSource", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.NONE)
public class ExternalSourceXml {

	private String system;
	private String externalId;
	private String locatorType;
	private String locator;
	private String title;
	private String kind;
	private String contentHash;
	private String lastIngestedAt;
	private List<SourceLinkXml> links = new ArrayList<>();

	public ExternalSourceXml() {
		// for JAXB
	}

	public ExternalSourceXml(String system, String externalId, String locatorType, String locator,
			String title, String kind, String contentHash, String lastIngestedAt) {
		this.system = system;
		this.externalId = externalId;
		this.locatorType = locatorType;
		this.locator = locator;
		this.title = title;
		this.kind = kind;
		this.contentHash = contentHash;
		this.lastIngestedAt = lastIngestedAt;
	}

	@XmlAttribute(name = "system")
	public String getSystem() {
		return system;
	}

	@XmlAttribute(name = "externalId")
	public String getExternalId() {
		return externalId;
	}

	@XmlAttribute(name = "locatorType")
	public String getLocatorType() {
		return locatorType;
	}

	@XmlAttribute(name = "locator")
	public String getLocator() {
		return locator;
	}

	@XmlAttribute(name = "title")
	public String getTitle() {
		return title;
	}

	@XmlAttribute(name = "kind")
	public String getKind() {
		return kind;
	}

	@XmlAttribute(name = "contentHash")
	public String getContentHash() {
		return contentHash;
	}

	@XmlAttribute(name = "lastIngestedAt")
	public String getLastIngestedAt() {
		return lastIngestedAt;
	}

	@XmlElement(name = "sourceLink", namespace = "http://www.rreganjr.com/requel")
	public List<SourceLinkXml> getLinks() {
		return links;
	}
}
