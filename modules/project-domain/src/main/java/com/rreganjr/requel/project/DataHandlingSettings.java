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
package com.rreganjr.requel.project;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Issue #262: a project's data-handling settings for AI analysis. They live in #268's
 * {@link ProjectAssistantSettingsStore} under fixed keys, where no row means on:
 * <ul>
 * <li>{@value #EGRESS_EXTERNAL}: the project may send text to a remote AI provider;</li>
 * <li>{@code redaction.<category>}: that category of sensitive text is masked before any
 * provider sees it.</li>
 * </ul>
 * Not carried in project XML, like the assistant switches.
 */
public final class DataHandlingSettings {

	public static final String EGRESS_EXTERNAL = "egress.external";

	/** What the default redaction policy can mask; each is a per-project switch. */
	public enum RedactionCategory {
		/** API keys, bearer tokens and JWTs, private-key blocks, passwords in URLs and pairs. */
		CREDENTIALS,
		EMAIL,
		PHONE,
		/** US social security numbers. */
		SSN,
		/** Payment card numbers (Luhn-valid). */
		CARD;

		/** The lower-case id used in setting keys and on the wire. */
		public String id() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}

		/** The settings-store key, e.g. {@code redaction.email}. */
		public String key() {
			return "redaction." + id();
		}

		public static Optional<RedactionCategory> fromId(String id) {
			for (RedactionCategory category : values()) {
				if (category.id().equalsIgnoreCase(id)) {
					return Optional.of(category);
				}
			}
			return Optional.empty();
		}
	}

	/** Every key the data-handling command accepts, in display order. */
	public static final List<String> KEYS;

	static {
		java.util.ArrayList<String> keys = new java.util.ArrayList<>();
		keys.add(EGRESS_EXTERNAL);
		for (RedactionCategory category : RedactionCategory.values()) {
			keys.add(category.key());
		}
		KEYS = List.copyOf(keys);
	}

	private static final DataHandlingSettings DEFAULTS = new DataHandlingSettings(true,
			EnumSet.allOf(RedactionCategory.class));

	private final boolean externalProviderAllowed;
	private final Set<RedactionCategory> redactionCategories;

	private DataHandlingSettings(boolean externalProviderAllowed,
			Set<RedactionCategory> redactionCategories) {
		this.externalProviderAllowed = externalProviderAllowed;
		this.redactionCategories = Collections.unmodifiableSet(
				redactionCategories.isEmpty() ? EnumSet.noneOf(RedactionCategory.class)
						: EnumSet.copyOf(redactionCategories));
	}

	/** Everything on: remote providers allowed, every category masked. */
	public static DataHandlingSettings defaults() {
		return DEFAULTS;
	}

	/** Settings from the keys switched off (the store's disabled set; other ids are ignored). */
	public static DataHandlingSettings fromDisabledKeys(Set<String> disabledKeys) {
		if (disabledKeys == null || disabledKeys.isEmpty()) {
			return DEFAULTS;
		}
		EnumSet<RedactionCategory> categories = EnumSet.allOf(RedactionCategory.class);
		for (RedactionCategory category : RedactionCategory.values()) {
			if (disabledKeys.contains(category.key())) {
				categories.remove(category);
			}
		}
		return new DataHandlingSettings(!disabledKeys.contains(EGRESS_EXTERNAL), categories);
	}

	/** The project's settings; defaults when there is no store or no project. */
	public static DataHandlingSettings forProject(Long projectId,
			ProjectAssistantSettingsStore store) {
		if (projectId == null || store == null) {
			return DEFAULTS;
		}
		return fromDisabledKeys(store.disabledAssistants(projectId));
	}

	public static boolean isKey(String key) {
		return key != null && KEYS.contains(key);
	}

	public boolean externalProviderAllowed() {
		return externalProviderAllowed;
	}

	public Set<RedactionCategory> redactionCategories() {
		return redactionCategories;
	}

	public boolean redacts(RedactionCategory category) {
		return redactionCategories.contains(category);
	}

	/** Category id to on/off, in enum order (the read endpoint's shape). */
	public Map<String, Boolean> redactionSwitches() {
		Map<String, Boolean> switches = new LinkedHashMap<>();
		for (RedactionCategory category : RedactionCategory.values()) {
			switches.put(category.id(), redactionCategories.contains(category));
		}
		return switches;
	}
}
