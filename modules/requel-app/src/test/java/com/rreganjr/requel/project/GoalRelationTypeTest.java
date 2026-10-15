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
package com.rreganjr.requel.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The goal relation vocabulary and its metadata (issue #257): every value carries what the UI,
 * the gateway schema and validation messages read from it, and parsing is forgiving about case
 * and whitespace but not about spelling.
 */
class GoalRelationTypeTest {

	@Test
	void parseIgnoresCaseAndSurroundingWhitespace() {
		assertEquals(Optional.of(GoalRelationType.Supports), GoalRelationType.parse("supports"));
		assertEquals(Optional.of(GoalRelationType.DependsOn), GoalRelationType.parse("  DEPENDSON "));
		assertEquals(Optional.of(GoalRelationType.Measures), GoalRelationType.parse("Measures"));
	}

	@Test
	void parseIsEmptyForNullBlankUnknownAndLabels() {
		assertEquals(Optional.empty(), GoalRelationType.parse(null));
		assertEquals(Optional.empty(), GoalRelationType.parse("  "));
		assertEquals(Optional.empty(), GoalRelationType.parse("Blocks"));
		// The label is for display; the value name is what a caller sends.
		assertEquals(Optional.empty(), GoalRelationType.parse("Depends on"));
	}

	@Test
	void everyValueCarriesItsMetadata() {
		for (GoalRelationType type : GoalRelationType.values()) {
			assertFalse(type.getLabel().isBlank(), type.name());
			assertFalse(type.getInverseLabel().isBlank(), type.name());
			assertFalse(type.getDescription().isBlank(), type.name());
			if (type.isSymmetric()) {
				assertEquals(type.getLabel(), type.getInverseLabel(), type.name());
			} else {
				assertFalse(type.getLabel().equals(type.getInverseLabel()), type.name());
			}
		}
	}

	@Test
	void onlyConflictsAndDuplicatesAreSymmetric() {
		for (GoalRelationType type : GoalRelationType.values()) {
			boolean expected = type == GoalRelationType.Conflicts || type == GoalRelationType.Duplicates;
			assertEquals(expected, type.isSymmetric(), type.name());
		}
	}

	@Test
	void permittedValuesListsEveryNameInOrder() {
		assertEquals("Supports, Conflicts, Refines, Duplicates, DependsOn, Obstructs, Measures",
				GoalRelationType.permittedValues());
		assertTrue(GoalRelationType.permittedValues().startsWith("Supports"));
	}
}
