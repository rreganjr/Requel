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
package com.rreganjr.requel.assistant.api;

import java.util.Optional;

/**
 * A first-class reference to the evidence an assistant used to justify an
 * {@link AnnotationAction} finding.
 *
 * <p>
 * Evidence is promoted to a typed contract field (rather than living in an
 * untyped metadata map) because the assistant finding idempotency key derives
 * from a normalized evidence hash:
 * {@code assistantId + ':' + targetType + ':' + targetId + ':' + findingType +
 * ':' + normalizedEvidenceHash}. Keeping evidence structured lets the core
 * applicator/finding layer compute a stable hash without parsing free-form
 * metadata. See {@code doc/assistant-spi-plan.md}.
 *
 * <p>
 * All three components are optional, but at least one must be present:
 * <ul>
 * <li>{@code entityRef} - the entity the evidence points at (may differ from the
 * action's target, e.g. a conflicting term on another goal).</li>
 * <li>{@code locator} - a stable, normalized position within the source text
 * (e.g. {@code "field=description;tokens=12-14"}) used for hashing.</li>
 * <li>{@code snippet} - a short human-readable excerpt for display; not required
 * to be stable and should not dominate the hash.</li>
 * </ul>
 */
public record EvidenceRef(EntityRef entityRef, String locator, String snippet) {

	public EvidenceRef {
		if (entityRef == null && locator == null && snippet == null) {
			throw new IllegalArgumentException(
					"EvidenceRef requires at least one of entityRef, locator, or snippet");
		}
	}

	public Optional<EntityRef> entityRefValue() {
		return Optional.ofNullable(entityRef);
	}

	public Optional<String> locatorValue() {
		return Optional.ofNullable(locator);
	}

	public Optional<String> snippetValue() {
		return Optional.ofNullable(snippet);
	}

	public static EvidenceRef ofEntity(EntityRef entityRef) {
		return new EvidenceRef(entityRef, null, null);
	}

	public static EvidenceRef ofLocator(String locator) {
		return new EvidenceRef(null, locator, null);
	}

	public static EvidenceRef ofSnippet(String snippet) {
		return new EvidenceRef(null, null, snippet);
	}

	public static EvidenceRef of(EntityRef entityRef, String locator, String snippet) {
		return new EvidenceRef(entityRef, locator, snippet);
	}
}
