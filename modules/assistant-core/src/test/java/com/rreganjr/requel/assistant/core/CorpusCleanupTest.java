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
package com.rreganjr.requel.assistant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingState;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.user.UserRepository;

/**
 * Issue #266: how the applicator cleans up after a corpus run. A relationship finding is the rows
 * sharing one annotation; it is retired, all rows together, only when every participant is in the
 * analyzed set and the run didn't report it again.
 */
class CorpusCleanupTest {

	private static final String FINDER = "corpus-finder";
	private static final String ACTIVE = AssistantFindingState.ACTIVE.name();
	private static final Instant NOW = Instant.parse("2026-10-03T00:00:00Z");

	private final CommandHandler commandHandler = mock(CommandHandler.class);
	private final AnnotationCommandFactory annotationCommandFactory = mock(
			AnnotationCommandFactory.class);
	private final AnnotationRepository annotationRepository = mock(AnnotationRepository.class);
	private final AssistantFindingRepository findingRepository = mock(
			AssistantFindingRepository.class);
	private final List<AssistantFindingEntity> active = new ArrayList<>();
	private CommandBackedAssistantResultApplicator applicator;

	@BeforeEach
	void setUp() {
		AssistantTargetLoader loader = mock(AssistantTargetLoader.class);
		when(loader.supports(any())).thenReturn(true);
		when(loader.loadTarget(any())).thenReturn(Optional.of(mock(ProjectOrDomainEntity.class)));
		when(findingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(findingRepository.findByAssistantIdAndProjectIdAndState(FINDER, 7L, ACTIVE))
				.thenReturn(active);
		applicator = new CommandBackedAssistantResultApplicator(commandHandler,
				annotationCommandFactory, mock(ProjectCommandFactory.class), annotationRepository,
				mock(UserRepository.class), findingRepository, mock(AssistantRunRepository.class),
				List.of(loader), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	/** An ACTIVE row of {@code key} on {@code target}, applied as {@code annotationId}. */
	private AssistantFindingEntity row(String key, EntityRef target, Long annotationId) {
		AssistantFindingEntity row = new AssistantFindingEntity(UUID.randomUUID(), key, FINDER,
				target.entityType(), target.entityId(), "POSSIBLE_OVERLAP", ACTIVE,
				UUID.randomUUID(), Instant.parse("2026-10-01T00:00:00Z"));
		row.setAppliedAnnotationId(annotationId);
		active.add(row);
		return row;
	}

	private static AssistantContext context(EntityRef project) {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "ron"),
				new UserRef(11L, "assistant"), project, "CORPUS_CANDIDATES", Locale.US,
				Clock.systemUTC(), Map.of());
	}

	private void apply(CleanupPolicy policy, Map<String, Object> metadata,
			AnnotationAction... actions) {
		apply(context(EntityRef.of("Project", 7L)), policy, metadata, actions);
	}

	private void apply(AssistantContext context, CleanupPolicy policy,
			Map<String, Object> metadata, AnnotationAction... actions) {
		AssistantResult.Builder result = AssistantResult.builder().assistantId(FINDER)
				.metadata(metadata);
		Arrays.stream(actions).forEach(result::annotationAction);
		applicator.apply(context, result.build(), policy, EntityRef.of("Project", 7L));
	}

	private static Map<String, Object> scope(String... refs) {
		return Map.of(CommandBackedAssistantResultApplicator.CORPUS_SCOPE, List.of(refs));
	}

	/** Issue actions apply to issue {@code issueId}. */
	private void stubIssueCommand(long issueId) throws Exception {
		EditIssueCommand command = mock(EditIssueCommand.class);
		when(annotationCommandFactory.newEditIssueCommand()).thenReturn(command);
		when(commandHandler.execute(command)).thenReturn(command);
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(issueId);
		when(command.getIssue()).thenReturn(issue);
	}

	private static AnnotationAction issue(String key, EntityRef target,
			Map<String, Object> metadata) {
		return new AnnotationAction(key, AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE,
				target, null, "Possible overlap", "LOW", null, List.of(), metadata);
	}

	@Test
	void aFindingNoLongerReportedInsideTheSetIsSupersededOnEveryParticipant() {
		AssistantFindingEntity one = row("k1", EntityRef.of("Goal", 1L), 99L);
		AssistantFindingEntity two = row("k2", EntityRef.of("Goal", 2L), 99L);

		apply(CleanupPolicy.MARK_SUPERSEDED, scope("Goal:1", "Goal:2", "Goal:3"));

		assertThat(one.getState()).isEqualTo(AssistantFindingState.SUPERSEDED.name());
		assertThat(two.getState()).isEqualTo(AssistantFindingState.SUPERSEDED.name());
	}

	@Test
	void aFindingReachingOutsideTheSetIsLeftAlone() {
		AssistantFindingEntity inside = row("k1", EntityRef.of("Goal", 1L), 99L);
		AssistantFindingEntity outside = row("k2", EntityRef.of("Goal", 5L), 99L);

		apply(CleanupPolicy.MARK_SUPERSEDED, scope("Goal:1", "Goal:2"));

		assertThat(inside.getState()).isEqualTo(ACTIVE);
		assertThat(outside.getState()).isEqualTo(ACTIVE);
	}

	@Test
	void aFindingReportedOnAnyParticipantKeepsAllItsRows() throws Exception {
		// the re-reported row stays on the finding's shared issue
		stubIssueCommand(99L);
		AssistantFindingEntity one = row("k1", EntityRef.of("Goal", 1L), 99L);
		AssistantFindingEntity two = row("k2", EntityRef.of("Goal", 2L), 99L);
		when(findingRepository.findByIdempotencyKey("k1")).thenReturn(Optional.of(one));

		apply(CleanupPolicy.MARK_SUPERSEDED, scope("Goal:1", "Goal:2"),
				issue("k1", EntityRef.of("Goal", 1L), Map.of()));

		assertThat(one.getState()).isEqualTo(ACTIVE);
		assertThat(two.getState()).isEqualTo(ACTIVE);
	}

	@Test
	void underAutoResolveAnUnappliedOrRemovedFindingIsClosed() {
		// no annotation applied: the rows are their own findings, keyed by idempotency key
		AssistantFindingEntity advisory = row("k1", EntityRef.of("Goal", 1L), null);
		AssistantFindingEntity removed = row("k2", EntityRef.of("Goal", 2L), 98L);
		when(annotationRepository.findAnnotationById(98L)).thenReturn(null);

		apply(CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED, scope("Goal:1", "Goal:2"));

		assertThat(advisory.getState()).isEqualTo(AssistantFindingState.AUTO_RESOLVED.name());
		assertThat(removed.getState()).isEqualTo(AssistantFindingState.AUTO_RESOLVED.name());
		assertThat(removed.getClosedAt()).isEqualTo(NOW);
	}

	@Test
	void manualCleanupOrARunWithoutAProjectRetiresNothing() {
		AssistantFindingEntity one = row("k1", EntityRef.of("Goal", 1L), null);

		apply(CleanupPolicy.MANUAL, scope("Goal:1"));
		apply(context(null), CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED, scope("Goal:1"));

		assertThat(one.getState()).isEqualTo(ACTIVE);
		verify(findingRepository, never()).findByAssistantIdAndProjectIdAndState(any(), any(),
				any());
	}

	@Test
	void anAnalysisRetiresTheFinderFindingsItNames() {
		AssistantFindingEntity judged = row("judged", EntityRef.of("Goal", 1L), null);
		AssistantFindingEntity closed = row("closed", EntityRef.of("Goal", 2L), null);
		closed.setState(AssistantFindingState.SUPERSEDED.name());
		when(findingRepository.findByIdempotencyKey("judged")).thenReturn(Optional.of(judged));
		when(findingRepository.findByIdempotencyKey("closed")).thenReturn(Optional.of(closed));
		when(findingRepository.findByIdempotencyKey("missing")).thenReturn(Optional.empty());
		active.clear();

		apply(CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED, Map.of(
				CommandBackedAssistantResultApplicator.CORPUS_SCOPE, List.of("Goal:1", "Goal:2"),
				CommandBackedAssistantResultApplicator.RETIRES_FINDINGS,
				Arrays.asList("judged", null, "closed", "missing")));

		assertThat(judged.getState()).isEqualTo(AssistantFindingState.AUTO_RESOLVED.name());
		assertThat(closed.getState()).isEqualTo(AssistantFindingState.SUPERSEDED.name());
	}

	@Test
	void retiresFindingsMustBeAList() {
		AssistantFindingEntity judged = row("judged", EntityRef.of("Goal", 1L), null);
		active.clear();

		apply(CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED, Map.of(
				CommandBackedAssistantResultApplicator.CORPUS_SCOPE, List.of("Goal:1"),
				CommandBackedAssistantResultApplicator.RETIRES_FINDINGS, "judged"));

		assertThat(judged.getState()).isEqualTo(ACTIVE);
	}

	@Test
	void aParticipantKeepsTheFingerprintItWasAnalyzedWithAndStalesTogether() throws Exception {
		stubIssueCommand(42L);

		apply(CleanupPolicy.MANUAL, scope("Goal:1", "Goal:2"),
				issue("k1", EntityRef.of("Goal", 1L), Map.of(
						CommandBackedAssistantResultApplicator.TARGET_FINGERPRINT, "fp-goal-1",
						CommandBackedAssistantResultApplicator.STALE_TOGETHER, true)),
				issue("k2", EntityRef.of("Goal", 2L), Map.of()));

		verify(findingRepository, atLeastOnce()).save(argThat((AssistantFindingEntity row) -> row != null
				&& "k1".equals(row.getIdempotencyKey()) && "fp-goal-1".equals(row.getTargetFingerprint())
				&& row.isStaleTogether()));
		verify(findingRepository, atLeastOnce()).save(argThat((AssistantFindingEntity row) -> row != null
				&& "k2".equals(row.getIdempotencyKey()) && !row.isStaleTogether()));
	}
}
