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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link TermTextMatcher}: whole-word, case-insensitive find and replace (issue #348). */
class TermTextMatcherTest {

	@Test
	void findsAWholePhraseInAnyCase() {
		assertTrue(TermTextMatcher.contains("Schedule the dry-run event.", "Dry-run event"));
		assertTrue(TermTextMatcher.contains("DRY-RUN EVENT first", "dry-run event"));
		assertTrue(TermTextMatcher.contains("the dry-run event's slides", "dry-run event"));
		assertTrue(TermTextMatcher.contains("dry-run event", "dry-run event"));
	}

	@Test
	void doesNotMatchInsideALongerWord() {
		assertFalse(TermTextMatcher.contains("two dry-run events", "dry-run event"));
		assertFalse(TermTextMatcher.contains("an eventual outcome", "event"));
		assertFalse(TermTextMatcher.contains("prevent it", "event"));
		assertFalse(TermTextMatcher.contains("event2", "event"));
	}

	@Test
	void blankInputsNeverMatch() {
		assertFalse(TermTextMatcher.contains(null, "event"));
		assertFalse(TermTextMatcher.contains("", "event"));
		assertFalse(TermTextMatcher.contains("event", " "));
		assertFalse(TermTextMatcher.contains("event", null));
	}

	@Test
	void replacesEveryUseKeepingALeadingCapital() {
		assertEquals("Dry-run webinar first, then the dry-run webinar's recap.",
				TermTextMatcher.replace("Dry-run event first, then the dry-run event's recap.",
						"dry-run event", "dry-run webinar"));
		assertEquals("Dry-run webinar", TermTextMatcher.replace("DRY-RUN EVENT", "dry-run event",
				"dry-run webinar"));
	}

	@Test
	void aCapitalisedTermNameFollowsTheTextsCase() {
		assertEquals("Before the dry-run webinar. Dry-run webinar notes.",
				TermTextMatcher.replace("Before the dry-run event. Dry-run event notes.",
						"Dry-run event", "Dry-run webinar"));
		assertEquals("call the API gateway", TermTextMatcher.replace("call the proxy", "proxy",
				"API gateway"));
	}

	@Test
	void replacementTextIsTakenLiterally() {
		assertEquals("costs $5 now", TermTextMatcher.replace("costs fee now", "fee", "$5"));
	}

	@Test
	void textWithoutTheTermComesBackAsIs() {
		String text = "two dry-run events";
		assertSame(text, TermTextMatcher.replace(text, "dry-run event", "dry-run webinar"));
		assertEquals(null, TermTextMatcher.replace(null, "x", "y"));
	}
}
