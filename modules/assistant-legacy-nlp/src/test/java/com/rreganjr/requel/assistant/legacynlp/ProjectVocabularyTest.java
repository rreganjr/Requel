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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;

/** Issue #268: a project's own words, from its glossary term and actor names. */
class ProjectVocabularyTest {

	@Test
	void namesAndTheirWordsAreVocabularyCaseInsensitively() {
		ProjectVocabulary vocabulary = ProjectVocabulary.of(List.of("Test room", "Dry-run event"),
				List.of("Zoom Host"));
		assertThat(vocabulary.contains("ROOM")).isTrue();
		assertThat(vocabulary.contains("test room")).isTrue();
		assertThat(vocabulary.contains("dry-run")).isTrue();
		assertThat(vocabulary.contains("run")).isTrue();
		assertThat(vocabulary.contains("host")).isTrue();
		assertThat(vocabulary.contains("webinar")).isFalse();
		assertThat(vocabulary.contains(null)).isFalse();
	}

	@Test
	void termAndActorNamesAreKeptApart() {
		ProjectVocabulary vocabulary = ProjectVocabulary.of(List.of("Test room"),
				List.of("Zoom Host"));
		assertThat(vocabulary.isTermName(" test ROOM ")).isTrue();
		assertThat(vocabulary.isTermName("Zoom Host")).isFalse();
		assertThat(vocabulary.isActorName("zoom host")).isTrue();
		assertThat(vocabulary.isActorName("room")).isFalse();
	}

	@Test
	void readsTheGlossaryAndActorsOfAProject() {
		GlossaryTerm room = mock(GlossaryTerm.class);
		when(room.getName()).thenReturn("Room");
		SortedSet<GlossaryTerm> terms = new TreeSet<>(Comparator.comparing(GlossaryTerm::getName));
		terms.add(room);
		Actor operator = mock(Actor.class);
		when(operator.getName()).thenReturn("Operator");
		ProjectOrDomain project = mock(ProjectOrDomain.class);
		doReturn(terms).when(project).getGlossaryTerms();
		doReturn(Set.of(operator)).when(project).getActors();

		ProjectVocabulary vocabulary = ProjectVocabulary.of(project);

		assertThat(vocabulary.isTermName("room")).isTrue();
		assertThat(vocabulary.isActorName("operator")).isTrue();
	}

	@Test
	void noProjectMeansNoVocabulary() {
		assertThat(ProjectVocabulary.of((ProjectOrDomain) null).contains("room")).isFalse();
	}
}
