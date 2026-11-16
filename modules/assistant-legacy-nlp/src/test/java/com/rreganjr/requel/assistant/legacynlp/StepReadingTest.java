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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Issue #486: when the parser can't read a step, a name that starts with "the system" or a known
 * actor reads as that subject.
 */
class StepReadingTest {

	private static final List<String> ACTORS = List.of("Member", "Library member", "Clerk");

	@Test
	void aKnownActorAtTheStartIsTheSubjectAsWritten() {
		assertEquals(StepReading.subject("Member"),
				StepReading.withKnownSubjects(StepReading.UNREADABLE, "Member downloads the invoice", ACTORS));
		assertEquals(StepReading.subject("The clerk"),
				StepReading.withKnownSubjects(StepReading.UNREADABLE, "The clerk stamps the slip", ACTORS));
		assertEquals(StepReading.subject("The system"),
				StepReading.withKnownSubjects(StepReading.UNREADABLE, "The system downloads it", ACTORS));
	}

	@Test
	void theLongestFittingNameWins() {
		assertEquals(StepReading.subject("Library member"), StepReading.withKnownSubjects(
				StepReading.UNREADABLE, "Library member renews the loan", ACTORS));
	}

	@Test
	void aReadableStepAndANameWithNothingAfterTheSubjectAreLeftAlone() {
		StepReading command = StepReading.command("Download");
		assertSame(command, StepReading.withKnownSubjects(command, "Member downloads it", ACTORS));
		assertSame(StepReading.UNREADABLE,
				StepReading.withKnownSubjects(StepReading.UNREADABLE, "Member", ACTORS));
		assertSame(StepReading.UNREADABLE,
				StepReading.withKnownSubjects(StepReading.UNREADABLE, "Members download it", ACTORS));
		assertSame(StepReading.UNREADABLE,
				StepReading.withKnownSubjects(StepReading.UNREADABLE, "Webinar", ACTORS));
	}
}
