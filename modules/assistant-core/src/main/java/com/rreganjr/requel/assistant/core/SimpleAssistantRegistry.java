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

import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantRegistry;
import com.rreganjr.requel.assistant.api.RequelAssistant;

/**
 * Registry implementation that matches assistants by their declared target
 * type. Settings and feature flags are layered in later phases.
 */
@Component
public class SimpleAssistantRegistry implements AssistantRegistry {

	private final List<RequelAssistant<?>> assistants;

	public SimpleAssistantRegistry(List<RequelAssistant<?>> assistants) {
		this.assistants = List.copyOf(assistants);
	}

	@Override
	public List<RequelAssistant<?>> findAssistantsFor(Object target, AssistantContext context) {
		Objects.requireNonNull(target, "target");
		List<RequelAssistant<?>> matches = new ArrayList<RequelAssistant<?>>();
		for (RequelAssistant<?> assistant : assistants) {
			if (assistant.targetType().isInstance(target)) {
				matches.add(assistant);
			}
		}
		return List.copyOf(matches);
	}
}
