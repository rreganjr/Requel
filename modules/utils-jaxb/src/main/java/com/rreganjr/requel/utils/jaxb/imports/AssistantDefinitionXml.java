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
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * Issue #371: one of a project's own assistant definitions in project XML - a customized copy of
 * a bundled definition (with {@code forkedFromVersion}) or the project's own. The export writes it
 * and the import reads it back, so the two can't disagree on the shape.
 * <p>
 * The task type and the output schema aren't written: they follow from the kind, or from the
 * bundled definition a copy customizes. Nor is an executor bean, which a project definition can't
 * name.
 */
@XmlRootElement(name = "assistantDefinition", namespace = AssistantDefinitionXml.NS)
@XmlType(name = "assistantDefinition", namespace = AssistantDefinitionXml.NS, propOrder = {
		"displayName", "scope", "contextProviders", "contextBudgets", "instructions",
		"vocabulary" })
@XmlAccessorType(XmlAccessType.FIELD)
public class AssistantDefinitionXml {

	static final String NS = "http://www.rreganjr.com/requel";

	@XmlAttribute(name = "key", required = true)
	private String key;

	@XmlAttribute(name = "kind", required = true)
	private String kind;

	@XmlAttribute(name = "forkedFromVersion")
	private Integer forkedFromVersion;

	@XmlAttribute(name = "localOnly")
	private Boolean localOnly;

	@XmlElement(name = "displayName", namespace = NS)
	private String displayName;

	@XmlElementWrapper(name = "scope", namespace = NS)
	@XmlElement(name = "type", namespace = NS)
	private List<String> scope = new ArrayList<>();

	@XmlElementWrapper(name = "contextProviders", namespace = NS)
	@XmlElement(name = "provider", namespace = NS)
	private List<String> contextProviders = new ArrayList<>();

	@XmlElementWrapper(name = "contextBudgets", namespace = NS)
	@XmlElement(name = "budget", namespace = NS)
	private List<Budget> contextBudgets = new ArrayList<>();

	@XmlElement(name = "instructions", namespace = NS)
	private String instructions;

	@XmlElementWrapper(name = "vocabulary", namespace = NS)
	@XmlElement(name = "entry", namespace = NS)
	private List<Entry> vocabulary = new ArrayList<>();

	public AssistantDefinitionXml() {
		// for JAXB
	}

	public AssistantDefinitionXml(String key, String kind, Integer forkedFromVersion,
			boolean localOnly, String displayName, List<String> scope,
			List<String> contextProviders, List<Budget> contextBudgets, String instructions,
			List<Entry> vocabulary) {
		this.key = key;
		this.kind = kind;
		this.forkedFromVersion = forkedFromVersion;
		this.localOnly = localOnly;
		this.displayName = displayName;
		this.scope = new ArrayList<>(scope);
		this.contextProviders = new ArrayList<>(contextProviders);
		this.contextBudgets = new ArrayList<>(contextBudgets);
		this.instructions = instructions;
		this.vocabulary = new ArrayList<>(vocabulary);
	}

	public String getKey() { return key; }
	public String getKind() { return kind; }
	public Integer getForkedFromVersion() { return forkedFromVersion; }
	public boolean isLocalOnly() { return Boolean.TRUE.equals(localOnly); }
	public String getDisplayName() { return displayName; }
	public List<String> getScope() { return scope == null ? List.of() : scope; }
	public List<String> getContextProviders() {
		return contextProviders == null ? List.of() : contextProviders;
	}
	public List<Budget> getContextBudgets() {
		return contextBudgets == null ? List.of() : contextBudgets;
	}
	public String getInstructions() { return instructions; }
	public List<Entry> getVocabulary() { return vocabulary == null ? List.of() : vocabulary; }

	/** A context provider's share of the pack, in characters (#261). */
	@XmlType(name = "assistantDefinitionBudget", namespace = NS)
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Budget {
		@XmlAttribute(name = "provider", required = true)
		private String provider;
		@XmlAttribute(name = "characters", required = true)
		private Integer characters;

		public Budget() {
			// for JAXB
		}

		public Budget(String provider, int characters) {
			this.provider = provider;
			this.characters = characters;
		}

		public String getProvider() { return provider; }
		public Integer getCharacters() { return characters; }
	}

	/** A finding type the definition may report; the element's text is its description. */
	@XmlType(name = "assistantDefinitionVocabulary", namespace = NS)
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Entry {
		@XmlAttribute(name = "type", required = true)
		private String type;
		@XmlAttribute(name = "category")
		private String category;
		@XmlValue
		private String description;

		public Entry() {
			// for JAXB
		}

		public Entry(String type, String description, String category) {
			this.type = type;
			this.description = description;
			this.category = category;
		}

		public String getType() { return type; }
		public String getCategory() { return category; }
		public String getDescription() { return description; }
	}
}
