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

import java.util.List;
import java.util.Optional;

/**
 * Issue #268: the assistants a project can switch on and off. Implemented by the assistant
 * registry, so the project layer can check a setting names a real assistant without depending
 * on the assistant SPI.
 */
public interface SwitchableAssistantCatalog {

	/** The group a bean assistant's switch shows under (#263). */
	String LEXICAL_CHECKS = "Lexical checks";
	/** The group an AI review definition's switch shows under (#263). */
	String AI_REVIEW = "AI review";

	/**
	 * An assistant a project can switch off. {@code group} is the heading its switch shows
	 * under (#263).
	 */
	record SwitchableAssistant(String assistantId, String displayName, String group) {

		public SwitchableAssistant(String assistantId, String displayName) {
			this(assistantId, displayName, LEXICAL_CHECKS);
		}
	}

	/** @return the switchable assistants, in registration order. */
	List<SwitchableAssistant> switchableAssistants();

	default Optional<SwitchableAssistant> find(String assistantId) {
		return switchableAssistants().stream()
				.filter(assistant -> assistant.assistantId().equals(assistantId)).findFirst();
	}
}
