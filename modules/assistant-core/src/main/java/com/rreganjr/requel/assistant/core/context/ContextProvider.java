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
package com.rreganjr.requel.assistant.core.context;

/**
 * Issue #261: adds one typed section to an entity's context pack - a goal's relations, a use
 * case's scenarios, and so on. A definition names the providers it wants by {@link #id()}.
 *
 * <p>{@link #contribute} runs inside the run's analyze transaction, so it may walk lazy
 * associations. It must route every name and text through {@link ProviderContext} (redaction,
 * truncation notes, author pseudonyms) and stay within its {@link ProviderBudget}. It must not
 * include annotations, provenance or references (#272/#273).
 */
public interface ContextProvider {

	/** Stable id a definition names, e.g. {@code goal-relations}. */
	String id();

	/** False when this provider has nothing to say about {@code target}'s type. */
	boolean appliesTo(Object target);

	/** The section for {@code target}. */
	ContextSection contribute(Object target, ProviderBudget budget, ProviderContext context);
}
