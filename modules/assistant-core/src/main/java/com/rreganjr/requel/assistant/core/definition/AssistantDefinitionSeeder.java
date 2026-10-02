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
package com.rreganjr.requel.assistant.core.definition;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Issue #260: seeds the bundled assistant definitions once the context's singletons exist (so in
 * every test context too). An invalid bundled file throws, which fails startup with its message.
 */
@Component
public class AssistantDefinitionSeeder implements SmartInitializingSingleton {

	private static final Logger log = LoggerFactory.getLogger(AssistantDefinitionSeeder.class);

	private final AssistantDefinitionStore store;
	private final ObjectMapper objectMapper;

	/** #263: the dev override directory, applied after seeding; absent outside dev. */
	private DevDefinitionOverrides overrides;

	@Autowired
	public AssistantDefinitionSeeder(AssistantDefinitionStore store, ObjectMapper objectMapper) {
		this.store = store;
		this.objectMapper = objectMapper;
	}

	@Autowired(required = false)
	public void setOverrides(DevDefinitionOverrides overrides) {
		this.overrides = overrides;
	}

	@Override
	public void afterSingletonsInstantiated() {
		List<AssistantDefinition> bundled = BundledDefinitions.load(objectMapper, store.validator());
		int changed = 0;
		for (AssistantDefinition definition : bundled) {
			if (store.seedBundled(definition)) {
				changed++;
			}
		}
		log.info("Assistant definitions: {} bundled, {} seeded or upgraded", bundled.size(),
				changed);
		if (overrides != null) {
			overrides.reload();
		}
	}
}
