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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
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
import com.rreganjr.requel.user.UserRepository;

/**
 * Unit coverage for the parts of {@link CommandBackedAssistantResultApplicator}
 * that do not need the full command stack: empty results and the cleanup action
 * types (delete / resolve / remove) — both the no-op case (no matching finding)
 * and a delete that executes its command and transitions the finding to DROPPED.
 * The create-or-update paths are exercised end-to-end by the Step 5 integration
 * test against the real command handler.
 */
class CommandBackedAssistantResultApplicatorTest {

	private final CommandHandler commandHandler = mock(CommandHandler.class);
	private final AnnotationCommandFactory annotationCommandFactory = mock(
			AnnotationCommandFactory.class);
	private final ProjectCommandFactory projectCommandFactory = mock(ProjectCommandFactory.class);
	private final AnnotationRepository annotationRepository = mock(AnnotationRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AssistantFindingRepository findingRepository = mock(
			AssistantFindingRepository.class);
	private final AssistantRunRepository runRepository = mock(AssistantRunRepository.class);
	private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"),
			ZoneOffset.UTC);

	private CommandBackedAssistantResultApplicator newApplicator() {
		return new CommandBackedAssistantResultApplicator(commandHandler, annotationCommandFactory,
				projectCommandFactory, annotationRepository, userRepository, findingRepository,
				runRepository, List.of(), fixedClock);
	}

	@Test
	void emptyResultAppliesNothing() {
		AssistantResult result = AssistantResult.builder().assistantId("legacy-lexical")
				.summary("nothing to do").build();

		AppliedAssistantResult applied = newApplicator().apply(context(), result,
				CleanupPolicy.MARK_SUPERSEDED, EntityRef.of("Goal", 1L));

		assertThat(applied.appliedActionCount()).isZero();
		assertThat(applied.annotationIds()).isEmpty();
		verifyNoInteractions(commandHandler);
		verifyNoInteractions(annotationCommandFactory);
		// Under MARK_SUPERSEDED the dispatch target is reconciled even for an empty result,
		// so findings are queried; with none present nothing is written.
		verify(findingRepository, never()).save(any());
	}

	@Test
	void cleanupActionWithNoExistingAnnotationIsNoOp() {
		// A delete action whose key matches no finding resolves no annotation, so no
		// delete command is issued and no finding is written.
		AnnotationAction delete = new AnnotationAction("legacy-lexical:Goal:1:stale",
				AnnotationAction.ActionType.DELETE_NOTE, EntityRef.of("Goal", 1L), null,
				"obsolete", null, null, List.of(), Map.of());
		AssistantResult result = AssistantResult.builder().assistantId("legacy-lexical")
				.summary("one stale finding").annotationAction(delete).build();

		AppliedAssistantResult applied = newApplicator().apply(context(), result,
				CleanupPolicy.MARK_SUPERSEDED, EntityRef.of("Goal", 1L));

		assertThat(applied.appliedActionCount()).isZero();
		verifyNoInteractions(annotationCommandFactory);
		verify(findingRepository, never()).save(any());
	}

	@Test
	void deleteNoteActionExecutesDeleteAndDropsFinding() throws Exception {
		String key = "legacy-lexical:Goal:1:note";
		AssistantFindingEntity finding = new AssistantFindingEntity(UUID.randomUUID(), key,
				"legacy-lexical", "Goal", 1L, "note", AssistantFindingState.ACTIVE.name(),
				UUID.randomUUID(), Instant.parse("2026-05-29T00:00:00Z"));
		finding.setAppliedAnnotationId(55L);
		when(findingRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(finding));
		Note note = mock(Note.class);
		when(annotationRepository.findById(Note.class, 55L)).thenReturn(note);
		DeleteNoteCommand deleteCommand = mock(DeleteNoteCommand.class);
		when(annotationCommandFactory.newDeleteNoteCommand()).thenReturn(deleteCommand);
		when(commandHandler.execute(deleteCommand)).thenReturn(deleteCommand);

		AnnotationAction delete = new AnnotationAction(key, AnnotationAction.ActionType.DELETE_NOTE,
				EntityRef.of("Goal", 1L), null, null, null, null, List.of(), Map.of());
		AssistantResult result = AssistantResult.builder().assistantId("legacy-lexical")
				.annotationAction(delete).build();

		newApplicator().apply(context(), result, CleanupPolicy.MARK_SUPERSEDED,
				EntityRef.of("Goal", 1L));

		verify(deleteCommand).setNote(note);
		verify(commandHandler).execute(deleteCommand);
		assertThat(finding.getState()).isEqualTo(AssistantFindingState.DROPPED.name());
		assertThat(finding.getClosedAt()).isEqualTo(Instant.parse("2026-05-29T00:00:00Z"));
		verify(findingRepository).save(finding);
	}

	@Test
	void staleFindingIsSupersededUnderMarkSupersededPolicy() {
		// A prior ACTIVE finding on the analyzed goal that this (empty) run does not
		// re-report is marked SUPERSEDED, with its annotation left in place.
		AssistantFindingEntity stale = new AssistantFindingEntity(UUID.randomUUID(),
				"legacy-lexical:Goal:1:old", "legacy-lexical", "Goal", 1L, "unknown-word",
				AssistantFindingState.ACTIVE.name(), UUID.randomUUID(),
				Instant.parse("2026-05-20T00:00:00Z"));
		stale.setAppliedAnnotationId(99L);
		when(findingRepository.findByAssistantIdAndTargetTypeAndTargetIdAndState("legacy-lexical",
				"Goal", 1L, AssistantFindingState.ACTIVE.name())).thenReturn(List.of(stale));

		AssistantResult result = AssistantResult.builder().assistantId("legacy-lexical").build();

		newApplicator().apply(context(), result, CleanupPolicy.MARK_SUPERSEDED,
				EntityRef.of("Goal", 1L));

		assertThat(stale.getState()).isEqualTo(AssistantFindingState.SUPERSEDED.name());
		assertThat(stale.getSupersededByRunId()).isNotNull();
		assertThat(stale.getClosedAt()).isEqualTo(Instant.parse("2026-05-29T00:00:00Z"));
		assertThat(stale.getAppliedAnnotationId()).isEqualTo(99L); // annotation left intact
		verify(findingRepository).save(stale);
		// SUPERSEDED never touches annotations.
		verifyNoInteractions(annotationCommandFactory);
	}

	@Test
	void manualPolicyLeavesStaleFindingsUntouched() {
		// Under MANUAL the applicator does not query or transition prior findings.
		AssistantResult result = AssistantResult.builder().assistantId("legacy-lexical").build();

		newApplicator().apply(context(), result, CleanupPolicy.MANUAL, EntityRef.of("Goal", 1L));

		verifyNoInteractions(findingRepository);
		verifyNoInteractions(annotationCommandFactory);
	}

	@Test
	void glossaryTermRefererActionAddsRefererViaEditCommand() throws Exception {
		EntityRef refererRef = EntityRef.of("Goal", 1L);
		EntityRef termRef = EntityRef.of("GlossaryTerm", 9L);
		ProjectOrDomainEntity referer = mock(ProjectOrDomainEntity.class);
		GlossaryTerm term = mock(GlossaryTerm.class);
		AssistantTargetLoader loader = mock(AssistantTargetLoader.class);
		when(loader.supports(refererRef)).thenReturn(true);
		when(loader.loadTarget(refererRef)).thenReturn(Optional.of(referer));
		when(loader.supports(termRef)).thenReturn(true);
		when(loader.loadTarget(termRef)).thenReturn(Optional.of(term));
		EditGlossaryTermCommand command = mock(EditGlossaryTermCommand.class);
		when(projectCommandFactory.newEditGlossaryTermCommand()).thenReturn(command);
		when(commandHandler.execute(command)).thenReturn(command);

		CommandBackedAssistantResultApplicator applicator = new CommandBackedAssistantResultApplicator(
				commandHandler, annotationCommandFactory, projectCommandFactory, annotationRepository,
				userRepository, findingRepository, runRepository, List.of(loader), fixedClock);

		AnnotationAction action = new AnnotationAction("legacy-lexical:Goal:1:glossary-referer:9",
				AnnotationAction.ActionType.ADD_GLOSSARY_TERM_REFERER, refererRef, null, null, null,
				null, List.of(),
				Map.of("glossaryTermType", "GlossaryTerm", "glossaryTermId", 9L));
		AssistantResult result = AssistantResult.builder().assistantId("legacy-lexical")
				.annotationAction(action).build();

		applicator.apply(context(), result, CleanupPolicy.MANUAL, refererRef);

		verify(command).setGlossaryTerm(term);
		verify(command).setAddReferers(argThat(set -> set.size() == 1 && set.contains(referer)));
		verify(command).setEditedBy(any());
		verify(commandHandler).execute(command);
		// A glossary-term referer is a project edit, not a finding.
		verifyNoInteractions(findingRepository);
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "ron"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", 7L), Locale.US,
				Clock.systemUTC(), Map.of());
	}
}
