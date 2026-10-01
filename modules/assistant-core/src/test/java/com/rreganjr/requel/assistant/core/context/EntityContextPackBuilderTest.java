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

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;

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
	void excludesAssistantSourcedAnnotationsFromContext() {
		Goal goal = mock(Goal.class);
		when(goal.getId()).thenReturn(42L);
		when(goal.getVersion()).thenReturn(1);
		when(goal.getName()).thenReturn("G");
		when(goal.getText()).thenReturn("text");
		LinkedHashSet<Annotation> annotations = new LinkedHashSet<>();
		Issue human = stubIssue(301L, 1, "human concern", false, false); // source null = human
		Note machine = stubNote(302L, 1, "machine echo");
		when(machine.getSource()).thenReturn("ASSISTANT:ai-requirements-review");
		annotations.add(human);
		annotations.add(machine);
		when(goal.getAnnotations()).thenReturn(annotations);
		when(goal.getGlossaryTerms()).thenReturn(Set.of());

		EntityContextPack pack = builder.build(goal);

		// Only the human annotation survives; the assistant-sourced one is filtered out.
		assertThat(pack.annotations()).extracting(AnnotationSnapshot::id).containsExactly(301L);
	}

	/** #270: the old lexical path wrote no source, but wrote as the assistant user. */
	@Test
	void excludesOldPathIssuesTheAssistantUserWroteWithoutASource() {
		Goal goal = mock(Goal.class);
		when(goal.getId()).thenReturn(42L);
		when(goal.getVersion()).thenReturn(1);
		when(goal.getName()).thenReturn("G");
		when(goal.getText()).thenReturn("text");
		LinkedHashSet<Annotation> annotations = new LinkedHashSet<>();
		Issue human = stubIssue(301L, 1, "human concern", false, false);
		com.rreganjr.platform.identity.User ron = mock(com.rreganjr.platform.identity.User.class);
		when(ron.getUsername()).thenReturn("ron");
		when(human.getCreatedBy()).thenReturn(ron);
		Issue oldPath = stubIssue(303L, 1, "The word \"Zzz\" may be misspelled", false, false);
		com.rreganjr.platform.identity.User assistant = mock(com.rreganjr.platform.identity.User.class);
		when(assistant.getUsername()).thenReturn("assistant");
		when(oldPath.getCreatedBy()).thenReturn(assistant);
		annotations.add(human);
		annotations.add(oldPath);
		when(goal.getAnnotations()).thenReturn(annotations);
		when(goal.getGlossaryTerms()).thenReturn(Set.of());

		EntityContextPack pack = builder.build(goal);

		assertThat(pack.annotations()).extracting(AnnotationSnapshot::id).containsExactly(301L);
	}

	@Test
	void throwsForUnsupportedTargetType() {
		assertThatThrownBy(() -> builder.build("not a domain entity"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unsupported target type");
	}

	// ---- #262: redaction, names, authors ---------------------------------------------

	private EntityContextPackBuilder redacting(ContextPackSizeLimits limits) {
		return new EntityContextPackBuilder(new DefaultRedactionPolicy(), limits, fixedClock);
	}

	private static Goal goal(String name, String text) {
		Goal goal = mock(Goal.class);
		when(goal.getId()).thenReturn(42L);
		when(goal.getVersion()).thenReturn(1);
		when(goal.getName()).thenReturn(name);
		when(goal.getText()).thenReturn(text);
		when(goal.getAnnotations()).thenReturn(new LinkedHashSet<>());
		when(goal.getGlossaryTerms()).thenReturn(Set.of());
		return goal;
	}

	@Test
	void theDefaultPolicyMasksTextAndNameAndNotesEveryPath() {
		Goal goal = goal("Notify ron@example.com",
				"Use key sk-proj-AbCdEfGhIjKlMnOpQrStUvWx and mail ops@example.com");

		EntityContextPack pack = redacting(new ContextPackSizeLimits()).build(goal);

		GoalSnapshot snapshot = (GoalSnapshot) pack.snapshot();
		assertThat(snapshot.name()).isEqualTo("Notify [REDACTED:EMAIL]");
		assertThat(snapshot.text()).isEqualTo(
				"Use key [REDACTED:CREDENTIALS] and mail [REDACTED:EMAIL]");
		assertThat(pack.metadata().redactedFields()).containsExactlyInAnyOrder(
				"goal.text: CREDENTIALS x1, EMAIL x1", "goal.name: EMAIL x1");
		assertThat(pack.metadata().redactionCount()).isEqualTo(3);
		assertThat(pack.metadata().redactionCategories())
				.containsExactlyInAnyOrder("CREDENTIALS", "EMAIL");
	}

	@Test
	void authorsBecomeRolesStableWithinThePack() {
		Goal goal = goal("G", "text");
		User alice = mock(User.class);
		when(alice.getUsername()).thenReturn("alice@example.com");
		User bob = mock(User.class);
		when(bob.getUsername()).thenReturn("bob");
		Issue first = stubIssue(1L, 1, "one", false, false);
		when(first.getCreatedBy()).thenReturn(alice);
		Note second = stubNote(2L, 1, "two");
		when(second.getCreatedBy()).thenReturn(bob);
		Note third = stubNote(3L, 1, "three");
		when(third.getCreatedBy()).thenReturn(alice);
		LinkedHashSet<Annotation> annotations = new LinkedHashSet<>();
		annotations.add(first);
		annotations.add(second);
		annotations.add(third);
		when(goal.getAnnotations()).thenReturn(annotations);

		EntityContextPack pack = builder.build(goal); // even with the no-op policy

		assertThat(pack.annotations()).extracting(AnnotationSnapshot::createdByUsername)
				.containsExactly("user-1", "user-2", "user-1");
	}

	@Test
	void redactionRunsBeforeTheFieldIsCappedSoAMaskIsNeverCut() {
		ContextPackSizeLimits limits = new ContextPackSizeLimits();
		limits.setMaxTextCharsPerField(30);
		// the email straddles character 30; truncating first would leave half an address
		Goal goal = goal("G", "Contact the owner at ron.regan@example.com today");

		EntityContextPack pack = redacting(limits).build(goal);

		String text = ((GoalSnapshot) pack.snapshot()).text();
		assertThat(text).hasSize(30).doesNotContain("ron.regan").doesNotContain("@");
		assertThat(pack.metadata().redactedFields()).containsExactly("goal.text: EMAIL x1");
	}

	@Test
	void theProjectsSwitchesApply() {
		Goal goal = goal("G", "call 555-123-4567 or ron@example.com");
		Project project = mock(Project.class);
		when(project.getId()).thenReturn(9L);
		when(goal.getProjectOrDomain()).thenReturn(project);
		ProjectAssistantSettingsStore store = mock(ProjectAssistantSettingsStore.class);
		when(store.disabledAssistants(9L)).thenReturn(Set.of("redaction.phone"));
		DefaultRedactionPolicy policy = new DefaultRedactionPolicy();
		policy.setSettingsStore(store);

		EntityContextPack pack = new EntityContextPackBuilder(policy, new ContextPackSizeLimits(),
				fixedClock).build(goal);

		assertThat(((GoalSnapshot) pack.snapshot()).text())
				.isEqualTo("call 555-123-4567 or [REDACTED:EMAIL]");
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
