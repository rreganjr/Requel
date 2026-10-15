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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Issue #261: the non-AI similarity used to rank sibling goals. */
class TextSimilarityTest {

	@Test
	void aRewordedGoalScoresAboveUnrelatedOnes() {
		double[] scores = new TextSimilarity().scores(
				"Search results are returned quickly to the user",
				List.of("Invoices are emailed monthly",
						"The user gets search results back fast and quickly",
						"Staff can reset passwords"));

		assertThat(scores[1]).isGreaterThan(scores[0]).isGreaterThan(0.0);
		assertThat(scores[1]).isGreaterThan(scores[2]);
	}

	@Test
	void stemmingMatchesWordForms() {
		double[] scores = new TextSimilarity().scores("Searching invoices",
				List.of("Search invoice", "Pay staff"));

		assertThat(scores[0]).isGreaterThan(0.5);
		assertThat(scores[1]).isZero();
	}

	@Test
	void glossaryAlternatesCountAsTheSameTerm() {
		TextSimilarity withGlossary = new TextSimilarity(
				Map.of("webinar", "webinar", "web conference", "webinar"));

		double[] scores = withGlossary.scores("Schedule a webinar",
				List.of("Book the web conference room", "Order lunch"));
		double[] without = new TextSimilarity().scores("Schedule a webinar",
				List.of("Book the web conference room", "Order lunch"));

		assertThat(scores[0]).isGreaterThan(0.0);
		assertThat(without[0]).isZero();
	}

	@Test
	void stopWordsAloneDoNotMatch() {
		double[] scores = new TextSimilarity().scores("The system shall be able to",
				List.of("It must be the one"));

		assertThat(scores[0]).isZero();
	}

	@Test
	void emptyTextScoresZero() {
		assertThat(new TextSimilarity().scores("", List.of("anything"))[0]).isZero();
		assertThat(new TextSimilarity().scores("anything", List.of(""))[0]).isZero();
	}
}
