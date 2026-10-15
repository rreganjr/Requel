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
package com.rreganjr.requel.assistant.core.corpus;

import static com.rreganjr.requel.assistant.core.corpus.CorpusFixture.referers;
import static com.rreganjr.requel.assistant.core.corpus.CorpusFixture.relate;
import static com.rreganjr.requel.assistant.core.corpus.CorpusFixture.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Member;
import com.rreganjr.requel.assistant.core.corpus.CorpusMembers.CorpusSet;
import com.rreganjr.requel.assistant.core.corpus.CorpusMembers.SetKind;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.TargetFingerprint;
import com.rreganjr.requel.project.UseCase;

/** Issue #266: the members of a corpus set and the structural links between them. */
class CorpusMembersTest {

	private static final EntityRef G1 = EntityRef.of("Goal", 1L);
	private static final EntityRef G2 = EntityRef.of("Goal", 2L);
	private static final EntityRef G3 = EntityRef.of("Goal", 3L);
	private static final EntityRef S10 = EntityRef.of("Story", 10L);
	private static final EntityRef A20 = EntityRef.of("Actor", 20L);
	private static final EntityRef A21 = EntityRef.of("Actor", 21L);
	private static final EntityRef T30 = EntityRef.of("GlossaryTerm", 30L);
	private static final EntityRef T31 = EntityRef.of("GlossaryTerm", 31L);
	private static final EntityRef U40 = EntityRef.of("UseCase", 40L);
	private static final EntityRef U41 = EntityRef.of("UseCase", 41L);
	private static final EntityRef SC50 = EntityRef.of("Scenario", 50L);
	private static final EntityRef SC51 = EntityRef.of("Scenario", 51L);
	private static final EntityRef SC52 = EntityRef.of("Scenario", 52L);
	private static final EntityRef ST60 = EntityRef.of("Step", 60L);
	private static final EntityRef ST61 = EntityRef.of("Step", 61L);
	private static final EntityRef ST62 = EntityRef.of("Step", 62L);
	private static final EntityRef ST63 = EntityRef.of("Step", 63L);

	private final CorpusFixture fixture = new CorpusFixture();
	private Goal goal1;
	private Goal goal2;
	private UseCase useCase40;
	private Project project;

	/**
	 * Three goals (1 relates to 2; story 10 refers to 1; 3 is referred to by a story outside the
	 * project), two actors, use case 40 (scenario 50 with steps 60, 61 and nested scenario 51;
	 * additional scenario 51 with step 62), use case 41 (scenario 52, step 63), and the glossary
	 * term "Librarian" (30) with its alternate "Clerk" (31), plus a term with no id.
	 */
	@BeforeEach
	void setUp() {
		goal1 = fixture.goal(1, "Lend books", "Members borrow books from the library");
		goal2 = fixture.goal(2, "Track loans", "Staff see every open loan");
		Goal goal3 = fixture.goal(3, "Run events", "The library hosts reading events");
		relate(goal1, goal2);
		Story story = fixture.story(10, "Borrowing", "A member borrows a book");
		referers(goal1, story);
		referers(goal3, CorpusFixture.entity(Story.class, 99L, "Elsewhere", "Not here"));
		Actor librarian = fixture.actor(20, "Librarian");
		fixture.actor(21, "Member");
		when(story.getPrimaryActor()).thenReturn(librarian);

		Scenario additional = fixture.scenario(51, "Renew instead", step(62, "Member renews"));
		Scenario main = fixture.scenario(50, "Borrow a book", step(60, "Member picks a book"),
				step(61, "Librarian scans the card"), additional);
		// scenario 50 is iterated before 51, as a project lists them
		fixture.scenarios.remove(additional);
		fixture.scenarios.add(additional);
		Scenario other = fixture.scenario(52, "Book a room", step(63, "Member picks a room"));
		useCase40 = fixture.useCase(40, "Borrow a book", "A member borrows a book");
		when(useCase40.getPrimaryActor()).thenReturn(librarian);
		when(useCase40.getScenario()).thenReturn(main);
		doReturn(Set.of(additional)).when(useCase40).getAdditionalScenarios();
		UseCase useCase41 = fixture.useCase(41, "Book a room", "A member books a room");
		when(useCase41.getScenario()).thenReturn(other);

		GlossaryTerm canonical = fixture.term(30L, "Librarian");
		GlossaryTerm alternate = fixture.term(31L, "Clerk");
		when(alternate.getCanonicalTerm()).thenReturn(canonical);
		doReturn(Set.of(alternate)).when(canonical).getAlternateTerms();
		referers(canonical, useCase40);
		fixture.term(null, "Ghost");
		project = fixture.project(7);
	}

	@Test
	void aProjectSetHoldsEveryEntityOnce() {
		CorpusSet set = CorpusMembers.project(project);

		assertThat(set.members()).extracting(Member::ref).containsExactly(G1, G2, G3, S10, A20,
				A21, U40, U41, SC50, ST60, ST61, SC51, ST62, SC52, ST63, T30, T31);
		assertThat(set.member(G1)).isEqualTo(new Member(G1, "Lend books",
				"Members borrow books from the library"));
		assertThat(set.member(EntityRef.of("Goal", 404L))).isNull();
		assertThat(set.scope()).startsWith("Goal:1", "Goal:2").contains("Step:63")
				.hasSize(17);
	}

	@Test
	void structuralNeighboursAreLinked() {
		CorpusSet set = CorpusMembers.project(project);

		assertThat(set.links()).containsExactlyInAnyOrder(
				Set.of(G1, S10), Set.of(S10, A20), Set.of(U40, A20), Set.of(U40, SC50),
				Set.of(U40, SC51), Set.of(U41, SC52), Set.of(SC50, ST60), Set.of(SC50, ST61),
				Set.of(SC50, SC51), Set.of(ST60, ST61), Set.of(ST60, SC51), Set.of(ST61, SC51),
				Set.of(SC51, ST62), Set.of(SC52, ST63), Set.of(T30, U40), Set.of(T30, T31),
				Set.of(T30, A20));
		assertThat(set.linked(ST61, ST60)).isTrue();
		// related goals are compared, not linked
		assertThat(set.linked(G1, G2)).isFalse();
	}

	@Test
	void everyMemberIsFingerprintedAsRead() {
		CorpusSet set = CorpusMembers.project(project);

		assertThat(set.fingerprints()).hasSize(17)
				.containsEntry(G1, TargetFingerprint.of("Lend books",
						"Members borrow books from the library"))
				.containsEntry(SC51, TargetFingerprint.of("Renew instead", "Renew instead"))
				.doesNotContainKey(EntityRef.of("Story", 99L));
	}

	@Test
	void glossaryNamesMapToTheirCanonicalToken() {
		assertThat(CorpusMembers.project(project).glossaryTokens()).isEqualTo(Map.of(
				"Librarian", "librarian", "Clerk", "librarian", "Ghost", "ghost"));
	}

	@Test
	void aGoalSetFollowsRelationsBothWaysAndTakesReferers() {
		CorpusSet fromParent = CorpusMembers.of(goal1, project);
		CorpusSet fromChild = CorpusMembers.of(goal2, project);

		assertThat(fromParent.members()).extracting(Member::ref).containsExactly(G1, G2, S10);
		assertThat(fromChild.members()).extracting(Member::ref).containsExactly(G1, G2, S10);
		assertThat(fromParent.links()).containsExactly(Set.of(G1, S10));
		assertThat(fromParent.fingerprints()).containsOnlyKeys(G1, G2, S10);
		assertThat(fromParent.glossaryTokens()).containsKey("Clerk");
	}

	@Test
	void aUseCaseSetTakesItsScenariosStepsAndActor() {
		CorpusSet set = CorpusMembers.of(useCase40, project);

		assertThat(set.members()).extracting(Member::ref).containsExactly(A20, U40, SC50, ST60,
				ST61, SC51, ST62);
		assertThat(set.links()).hasSize(10).contains(Set.of(U40, A20), Set.of(SC51, ST62))
				.doesNotContain(Set.of(T30, U40));
	}

	@Test
	void aProjectRootIsTheWholeProject() {
		assertThat(CorpusMembers.of(project, project)).isEqualTo(CorpusMembers.project(project));
	}

	@Test
	void onlyAProjectGoalOrUseCaseRootsASet() {
		assertThat(SetKind.of(project)).isEqualTo(SetKind.PROJECT);
		assertThat(SetKind.of(goal1)).isEqualTo(SetKind.GOAL);
		assertThat(SetKind.of(useCase40)).isEqualTo(SetKind.USE_CASE);
		Step step = step(1, "x");
		assertThat(SetKind.of(step)).isNull();

		assertThatThrownBy(() -> CorpusMembers.of(step, project))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Step");
		assertThatThrownBy(() -> CorpusMembers.of(null, project))
				.isInstanceOf(IllegalArgumentException.class).hasMessageEndingWith("not null");
	}

	@Test
	void aReferenceNeedsAnEntityWithAnId() {
		assertThat(CorpusMembers.ref(goal1)).isEqualTo(G1);
		assertThat(CorpusMembers.ref(CorpusFixture.entity(Goal.class, null, "n", "t"))).isNull();
		assertThat(CorpusMembers.ref("not an entity")).isNull();
		assertThat(CorpusMembers.ref(null)).isNull();
	}

	@Test
	void anEmptyProjectHasAnEmptySet() {
		CorpusSet set = CorpusMembers.project(new CorpusFixture().project(8));

		assertThat(set.members()).isEmpty();
		assertThat(set.links()).isEmpty();
		assertThat(set.scope()).isEqualTo(List.of());
	}
}
