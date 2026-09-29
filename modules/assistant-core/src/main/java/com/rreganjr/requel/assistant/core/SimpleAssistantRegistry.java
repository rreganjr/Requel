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
package com.rreganjr.requel.assistant.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantRegistry;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;

/**
 * Registry implementation that matches assistants by their declared target type. Issue #268: an
 * assistant a project has switched off ({@link ProjectAssistantSettingsStore}) is left out of
 * that project's runs; it is also the {@link SwitchableAssistantCatalog} of the assistants a
 * project can switch.
 */
@Component
public class SimpleAssistantRegistry implements AssistantRegistry, SwitchableAssistantCatalog {

	private static final Logger log = LoggerFactory.getLogger(SimpleAssistantRegistry.class);

	private final List<RequelAssistant<?>> assistants;
	private ProjectAssistantSettingsStore settingsStore;

	@Autowired
	public SimpleAssistantRegistry(List<RequelAssistant<?>> assistants) {
		this.assistants = List.copyOf(assistants);
	}

	/** Optional: with no store every assistant runs everywhere (unit tests, tools). */
	@Autowired(required = false)
	public void setSettingsStore(ProjectAssistantSettingsStore settingsStore) {
		this.settingsStore = settingsStore;
	}

	@Override
	public List<RequelAssistant<?>> findAssistantsFor(Object target, AssistantContext context) {
		Objects.requireNonNull(target, "target");
		Set<String> disabled = disabledFor(context);
		List<RequelAssistant<?>> matches = new ArrayList<RequelAssistant<?>>();
		for (RequelAssistant<?> assistant : assistants) {
			if (!assistant.targetType().isInstance(target)) {
				continue;
			}
			if (assistant.projectSwitchable() && disabled.contains(assistant.assistantId())) {
				log.debug("assistant {} is switched off in {}", assistant.assistantId(),
						context.projectRef());
				continue;
			}
			matches.add(assistant);
		}
		return List.copyOf(matches);
	}

	@Override
	public List<SwitchableAssistant> switchableAssistants() {
		List<SwitchableAssistant> switchable = new ArrayList<>();
		for (RequelAssistant<?> assistant : assistants) {
			if (assistant.projectSwitchable()) {
				switchable.add(new SwitchableAssistant(assistant.assistantId(),
						assistant.displayName()));
			}
		}
		return List.copyOf(switchable);
	}

	private Set<String> disabledFor(AssistantContext context) {
		if (settingsStore == null || context == null || context.projectRef() == null
				|| !"Project".equals(context.projectRef().entityType())) {
			return Set.of();
		}
		return settingsStore.disabledAssistants(context.projectRef().entityId());
	}
}
