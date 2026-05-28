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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;

class EntityContextPackBuilderTest {

	private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"),
			ZoneOffset.UTC);
	private final EntityContextPackBuilder builder = new EntityContextPackBuilder(
			new NoOpRedactionPolicy(), new ContextPackSizeLimits(), fixedClock);

	@Test
	void buildsGoalSnapshotWithAnnotationsAndRelatedTerms() {
		Goal goal = mock(Goal.class);
		when(goal.getId()).thenReturn(42L);
		when(goal.getVersion()).thenReturn(1);
		when(goal.getName()).thenReturn("Reduce churn");
		when(goal.getText()).thenReturn("Churn target");
		LinkedHashSet<Annotation> annotations = new LinkedHashSet<>();
		annotations.add(stubIssue(101L, 2, "Ambiguous wording", false, false));
		annotations.add(stubNote(102L, 1, "nice phrasing"));
		when(goal.getAnnotations()).thenReturn(annotations);
		GlossaryTerm term = mock(GlossaryTerm.class);
		when(term.getId()).thenReturn(7L);
		when(term.getVersion()).thenReturn(1);
		when(term.getName()).thenReturn("Churn");
		when(term.getText()).thenReturn("rate at which customers leave");
		when(goal.getGlossaryTerms()).thenReturn(Set.of(term));

		EntityContextPack pack = builder.build(goal);

		assertThat(pack.target().entityType()).isEqualTo("Goal");
		assertThat(pack.target().entityId()).isEqualTo(42L);
		assertThat(pack.snapshot()).isInstanceOf(GoalSnapshot.class);
		assertThat(((GoalSnapshot) pack.snapshot()).name()).isEqualTo("Reduce churn");
		assertThat(pack.annotations()).extracting(AnnotationSnapshot::kind)
				.containsExactly(AnnotationKind.ISSUE, AnnotationKind.NOTE);
		assertThat(pack.annotations().get(0).id()).isEqualTo(101L);
		assertThat(pack.annotations().get(0).version()).isEqualTo(2);
		assertThat(pack.annotations().get(0).mustBeResolved()).isFalse();
		assertThat(pack.annotations().get(1).id()).isEqualTo(102L);
		assertThat(pack.annotations().get(1).version()).isEqualTo(1);
		assertThat(pack.relatedTerms()).extracting(GlossaryTermSnapshot::name)
				.containsExactly("Churn");
	}

	@Test
	void capsAnnotationsAtConfiguredLimit() {
		ContextPackSizeLimits limits = new ContextPackSizeLimits();
		limits.setMaxAnnotationsPerEntity(2);
		EntityContextPackBuilder capped = new EntityContextPackBuilder(new NoOpRedactionPolicy(),
				limits, fixedClock);
		Goal goal = mock(Goal.class);
		when(goal.getId()).thenReturn(42L);
		when(goal.getVersion()).thenReturn(1);
		when(goal.getName()).thenReturn("G");
		when(goal.getText()).thenReturn("text");
		LinkedHashSet<Annotation> annotations = new LinkedHashSet<>();
		annotations.add(stubNote(201L, 1, "note 1"));
		annotations.add(stubNote(202L, 1, "note 2"));
		annotations.add(stubNote(203L, 1, "note 3"));
		when(goal.getAnnotations()).thenReturn(annotations);
		when(goal.getGlossaryTerms()).thenReturn(Set.of());

		EntityContextPack pack = capped.build(goal);

		assertThat(pack.annotations()).hasSize(2);
		assertThat(pack.metadata().truncated()).isTrue();
		assertThat(pack.metadata().truncationNotes())
				.anyMatch(note -> note.contains("annotations list capped at 2"));
	}

	@Test
	void throwsForUnsupportedTargetType() {
		assertThatThrownBy(() -> builder.build("not a domain entity"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unsupported target type");
	}

	private static Issue stubIssue(long id, int version, String text, boolean mustBeResolved,
			boolean resolved) {
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(id);
		when(issue.getVersion()).thenReturn(version);
		when(issue.getText()).thenReturn(text);
		when(issue.isMustBeResolved()).thenReturn(mustBeResolved);
		when(issue.isResolved()).thenReturn(resolved);
		when(issue.getCreatedBy()).thenReturn(null);
		when(issue.getDateCreated()).thenReturn(new Date());
		return issue;
	}

	private static Note stubNote(long id, int version, String text) {
		Note note = mock(Note.class);
		when(note.getId()).thenReturn(id);
		when(note.getVersion()).thenReturn(version);
		when(note.getText()).thenReturn(text);
		when(note.isMustBeResolved()).thenReturn(false);
		when(note.isResolved()).thenReturn(false);
		when(note.getCreatedBy()).thenReturn(null);
		when(note.getDateCreated()).thenReturn(new Date());
		return note;
	}
}
