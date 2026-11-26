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
package com.rreganjr.requel.service.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** #504: the LIKE escaping and the snippet window. */
class ProjectSearchServiceTest {

	@Test
	void likePatternLowerCasesAndEscapesTheWildcardsAndTheEscape() {
		assertThat(ProjectSearchService.likePattern("50% Off_Now!")).isEqualTo("%50!% off!_now!!%");
	}

	@Test
	void aShortFieldIsTheWholeSnippetWithTheMatchPlaced() {
		ProjectSearchService.Snippet snippet = ProjectSearchService.snippet("Speed up Login", "login");
		assertThat(snippet.text()).isEqualTo("Speed up Login");
		assertThat(snippet.text().substring(snippet.start(), snippet.start() + snippet.length()))
				.isEqualTo("Login");
	}

	@Test
	void aLongFieldIsCutAroundTheMatchWithEllipses() {
		String text = "a".repeat(500) + " needle " + "b".repeat(500);
		ProjectSearchService.Snippet snippet = ProjectSearchService.snippet(text, "NEEDLE");
		assertThat(snippet.text()).startsWith("…").endsWith("…");
		assertThat(snippet.text().length()).isLessThan(200);
		assertThat(snippet.text().substring(snippet.start(), snippet.start() + snippet.length()))
				.isEqualTo("needle");
	}

	@Test
	void newlinesBecomeSpacesAndAnUnplacedMatchShowsTheStart() {
		ProjectSearchService.Snippet snippet = ProjectSearchService.snippet("one\ntwo", "café");
		assertThat(snippet.text()).isEqualTo("one two");
		assertThat(snippet.start()).isEqualTo(-1);
		assertThat(snippet.length()).isZero();
	}
}
