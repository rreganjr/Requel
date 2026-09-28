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
package com.rreganjr.requel.assistant.legacynlp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;

/**
 * Issue #268: runs one lexical check per property of an entity and remembers which properties
 * finished. A check that throws on one property logs it and moves on, so the other property's
 * findings still count, and the result says it is incomplete ({@code metadata.incomplete} and
 * {@code metadata.failedProperties}) instead of looking complete. The applicator then skips
 * auto-resolving for it, and the worker records the run as {@code PARTIAL}.
 */
final class PropertyChecks {

	/** One property's analysis. */
	@FunctionalInterface
	interface Check {
		void run();
	}

	private final Logger log;
	private final String checkName;
	private final EntityRef targetRef;
	private final Set<String> completed = new HashSet<>();
	private final List<String> failed = new ArrayList<>();

	PropertyChecks(Logger log, String checkName, EntityRef targetRef) {
		this.log = log;
		this.checkName = checkName;
		this.targetRef = targetRef;
	}

	/**
	 * Run {@code check} for {@code propertyName}. A blank property has nothing to analyze and
	 * counts as complete.
	 */
	void run(String propertyName, String text, Check check) {
		if (text == null || text.isBlank()) {
			completed.add(propertyName);
			return;
		}
		try {
			check.run();
			completed.add(propertyName);
		} catch (RuntimeException e) {
			log.warn("{} analysis of the {} of {} failed; the result is incomplete: {}", checkName,
					propertyName, targetRef, e.toString());
			failed.add(propertyName);
		}
	}

	boolean completed(String propertyName) {
		return completed.contains(propertyName);
	}

	boolean complete() {
		return failed.isEmpty();
	}

	List<String> failedProperties() {
		return List.copyOf(failed);
	}

	/** Mark the builder's result incomplete when a property failed. */
	AssistantResult.Builder finish(AssistantResult.Builder builder) {
		if (!failed.isEmpty()) {
			builder.metadata(Map.of("incomplete", Boolean.TRUE, "failedProperties",
					List.copyOf(failed)));
		}
		return builder;
	}
}
