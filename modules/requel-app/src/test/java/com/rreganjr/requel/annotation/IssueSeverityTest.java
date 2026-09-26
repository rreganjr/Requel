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
package com.rreganjr.requel.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * {@link IssueSeverity} parsing and ranking (issue #271).
 */
class IssueSeverityTest {

	@Test
	void parseIsCaseInsensitiveAndTrims() {
		assertEquals(Optional.of(IssueSeverity.HIGH), IssueSeverity.parse("high"));
		assertEquals(Optional.of(IssueSeverity.MEDIUM), IssueSeverity.parse(" Medium "));
		assertEquals(Optional.of(IssueSeverity.LOW), IssueSeverity.parse("LOW"));
	}

	@Test
	void parseIsEmptyForNullBlankAndUnknown() {
		assertTrue(IssueSeverity.parse(null).isEmpty());
		assertTrue(IssueSeverity.parse("  ").isEmpty());
		assertTrue(IssueSeverity.parse("urgent").isEmpty());
		assertTrue(IssueSeverity.parse("CRITICAL").isEmpty());
	}

	@Test
	void rankOrdersHighAboveMediumAboveLowAboveNothing() {
		assertTrue(IssueSeverity.HIGH.rank() > IssueSeverity.MEDIUM.rank());
		assertTrue(IssueSeverity.MEDIUM.rank() > IssueSeverity.LOW.rank());
		assertTrue(IssueSeverity.LOW.rank() > IssueSeverity.rankOf(null));
	}

	@Test
	void vocabularyMatchesTheAiOutputSchema() {
		// requirements-review-output.v1.json declares ["LOW", "MEDIUM", "HIGH", null].
		assertEquals("[LOW, MEDIUM, HIGH]", Arrays.toString(IssueSeverity.values()));
	}
}
