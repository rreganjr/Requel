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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * {@link IssueSeverity} parsing and ranking (issue #271), and the minimum-severity filter (#338).
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

	// #338: the minimum-severity filter's parse and test.

	@Test
	void parseMinimumIsNullForNullAndBlank() {
		assertNull(IssueSeverity.parseMinimum("minSeverity", null));
		assertNull(IssueSeverity.parseMinimum("minSeverity", " "));
	}

	@Test
	void parseMinimumAcceptsEveryNameInAnyCase() {
		assertEquals(IssueSeverity.LOW, IssueSeverity.parseMinimum("minSeverity", "low"));
		assertEquals(IssueSeverity.MEDIUM, IssueSeverity.parseMinimum("minSeverity", " Medium "));
		assertEquals(IssueSeverity.HIGH, IssueSeverity.parseMinimum("minSeverity", "HIGH"));
	}

	@Test
	void parseMinimumRefusesAnUnknownValueNamingTheParameterAndTheChoices() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> IssueSeverity.parseMinimum("minSeverity", "urgent"));
		assertEquals("Unsupported minSeverity value: urgent; expected LOW, MEDIUM or HIGH",
				e.getMessage());
	}

	@Test
	void atLeastLetsThroughTheMinimumAndAbove() {
		assertTrue(IssueSeverity.atLeast(IssueSeverity.LOW, null));
		assertTrue(IssueSeverity.atLeast(null, null));
		assertFalse(IssueSeverity.atLeast(IssueSeverity.LOW, IssueSeverity.MEDIUM));
		assertTrue(IssueSeverity.atLeast(IssueSeverity.MEDIUM, IssueSeverity.MEDIUM));
		assertTrue(IssueSeverity.atLeast(IssueSeverity.HIGH, IssueSeverity.MEDIUM));
		assertFalse(IssueSeverity.atLeast(null, IssueSeverity.LOW));
	}

	@Test
	void vocabularyMatchesTheAiOutputSchema() {
		// requirements-review-output.v1.json declares ["LOW", "MEDIUM", "HIGH", null].
		assertEquals("[LOW, MEDIUM, HIGH]", Arrays.toString(IssueSeverity.values()));
	}
}
