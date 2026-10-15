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
package com.rreganjr.requel.assistant.core.freshness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.spi.AnnotationFreshness.StaleAnnotations;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.AssistantTargetLoader;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingState;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.TargetFingerprint;

/** Issue #270: the stale rule over the assistant findings. */
class FindingFreshnessTest {

	private final AssistantFindingRepository repository = mock(AssistantFindingRepository.class);
	private final FindingFreshness freshness = new FindingFreshness(repository);
	private final List<AssistantFindingEntity> findings = new ArrayList<>();

	FindingFreshnessTest() {
		when(repository.findByAppliedAnnotationIdInAndStateIn(anyCollection(), anyCollection()))
				.thenReturn(findings);
	}

	@Test
	void anIssueRaisedAgainstTextThatHasSinceChangedIsStale() {
		Issue issue = issue(99L);
		Goal goal = goal(1L, "Login", "Users log in fast.", issue);
		finding("Goal", 1L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Login", "Users log in."));

		assertThat(freshness.staleAnnotations(List.of(goal)).isStale(goal, issue)).isTrue();
	}

	@Test
	void anIssueWhoseTextStillMatchesIsNotStale() {
		Issue issue = issue(99L);
		Goal goal = goal(1L, "Login", "Users log in.", issue);
		finding("Goal", 1L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Login", "Users log in."));

		assertThat(freshness.staleAnnotations(List.of(goal)).isStale(goal, issue)).isFalse();
	}

	@Test
	void aFindingRecordedBeforeFingerprintsIsNotStale() {
		Issue issue = issue(99L);
		Goal goal = goal(1L, "Login", "anything", issue);
		finding("Goal", 1L, 99L, AssistantFindingState.ACTIVE, null);

		assertThat(freshness.staleAnnotations(List.of(goal)).isStale(goal, issue)).isFalse();
	}

	@Test
	void aSupersededFindingIsStaleEvenWhenTheTextMatches() {
		Issue issue = issue(99L);
		Goal goal = goal(1L, "Login", "Users log in.", issue);
		finding("Goal", 1L, 99L, AssistantFindingState.SUPERSEDED,
				TargetFingerprint.of("Login", "Users log in."));

		assertThat(freshness.staleAnnotations(List.of(goal)).isStale(goal, issue)).isTrue();
	}

	@Test
	void oneFreshFindingKeepsTheAnnotationFresh() {
		Issue issue = issue(99L);
		Goal goal = goal(1L, "Login", "Users log in.", issue);
		finding("Goal", 1L, 99L, AssistantFindingState.SUPERSEDED, null);
		finding("Goal", 1L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Login", "Users log in."));

		assertThat(freshness.staleAnnotations(List.of(goal)).isStale(goal, issue)).isFalse();
	}

	@Test
	void anAnnotationWithNoFindingIsNeverStale() {
		Issue human = issue(5L);
		Goal goal = goal(1L, "Login", "Users log in.", human);

		assertThat(freshness.staleAnnotations(List.of(goal)).isStale(goal, human)).isFalse();
	}

	@Test
	void staleIsPerEntity() {
		// One glossary-candidate issue on two goals: stale on the one whose text changed.
		Issue shared = issue(99L);
		Goal changed = goal(1L, "Login", "Users sign in.", shared);
		Goal unchanged = goal(2L, "Logout", "Users log out.", shared);
		finding("Goal", 1L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Login", "Users log in."));
		finding("Goal", 2L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Logout", "Users log out."));

		StaleAnnotations stale = freshness.staleAnnotations(List.of(changed, unchanged));

		assertThat(stale.isStale(changed, shared)).isTrue();
		assertThat(stale.isStale(unchanged, shared)).isFalse();
	}

	@Test
	void aProjectWideReadIsOneQuery() {
		Goal a = goal(1L, "A", "a", issue(10L));
		Goal b = goal(2L, "B", "b", issue(11L));
		Goal c = goal(3L, "C", "c", issue(12L));

		freshness.staleAnnotations(List.of(a, b, c));

		verify(repository, times(1)).findByAppliedAnnotationIdInAndStateIn(anyCollection(),
				anyCollection());
	}

	@Test
	void anEntityOutsideTheLookupAnswersFalse() {
		Issue issue = issue(99L);
		Goal goal = goal(1L, "Login", "Users sign in.", issue);
		Goal other = goal(7L, "Other", "x", issue);
		finding("Goal", 1L, 99L, AssistantFindingState.ACTIVE, "stale-fingerprint");

		assertThat(freshness.staleAnnotations(List.of(goal)).isStale(other, issue)).isFalse();
	}

	@Test
	void aRelationshipIsStaleOnEveryParticipantWhenOneChanged() {
		// #266: one corpus issue on two goals; only the first changed, both read stale
		Issue shared = issue(99L);
		Goal changed = goal(1L, "Login", "Users sign in.", shared);
		Goal unchanged = goal(2L, "Logout", "Users log out.", shared);
		together(finding("Goal", 1L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Login", "Users log in.")));
		together(finding("Goal", 2L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Logout", "Users log out.")));

		StaleAnnotations stale = freshness.staleAnnotations(List.of(changed, unchanged));

		assertThat(stale.isStale(changed, shared)).isTrue();
		assertThat(stale.isStale(unchanged, shared)).isTrue();
	}

	@Test
	void aRelationshipLoadsAParticipantTheReadDidNotAskAbout() {
		// #266: reading only the unchanged goal still finds the other side changed
		Issue shared = issue(99L);
		Goal unchanged = goal(2L, "Logout", "Users log out.", shared);
		Goal changed = goal(1L, "Login", "Users sign in.", shared);
		AssistantTargetLoader loader = mock(AssistantTargetLoader.class);
		when(loader.supports(EntityRef.of("Goal", 1L))).thenReturn(true);
		when(loader.loadTarget(EntityRef.of("Goal", 1L))).thenReturn(Optional.of(changed));
		freshness.setTargetLoaders(List.of(loader));
		together(finding("Goal", 1L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Login", "Users log in.")));
		together(finding("Goal", 2L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Logout", "Users log out.")));

		assertThat(freshness.staleAnnotations(List.of(unchanged)).isStale(unchanged, shared))
				.isTrue();
	}

	@Test
	void aRelationshipWithEveryParticipantUnchangedIsFresh() {
		Issue shared = issue(99L);
		Goal a = goal(1L, "Login", "Users log in.", shared);
		Goal b = goal(2L, "Logout", "Users log out.", shared);
		together(finding("Goal", 1L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Login", "Users log in.")));
		together(finding("Goal", 2L, 99L, AssistantFindingState.ACTIVE,
				TargetFingerprint.of("Logout", "Users log out.")));

		StaleAnnotations stale = freshness.staleAnnotations(List.of(a, b));

		assertThat(stale.isStale(a, shared)).isFalse();
		assertThat(stale.isStale(b, shared)).isFalse();
	}

	private static void together(AssistantFindingEntity finding) {
		finding.setStaleTogether(true);
	}

	private AssistantFindingEntity finding(String type, Long targetId, Long annotationId,
			AssistantFindingState state, String fingerprint) {
		AssistantFindingEntity finding = new AssistantFindingEntity(UUID.randomUUID(),
				"k:" + UUID.randomUUID(), "ai-review", type, targetId, "AMBIGUOUS", state.name(),
				UUID.randomUUID(), Instant.parse("2026-09-01T00:00:00Z"));
		finding.setAppliedAnnotationId(annotationId);
		finding.setTargetFingerprint(fingerprint);
		findings.add(finding);
		return finding;
	}

	private static Issue issue(Long id) {
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(id);
		return issue;
	}

	private static Goal goal(Long id, String name, String text, Annotation... annotations) {
		Goal goal = mock(Goal.class);
		when(goal.getId()).thenReturn(id);
		when(goal.getName()).thenReturn(name);
		when(goal.getText()).thenReturn(text);
		doReturn(Goal.class).when(goal).getProjectOrDomainEntityInterface();
		when(goal.getAnnotations()).thenReturn(new LinkedHashSet<>(Set.of(annotations)));
		return goal;
	}
}
