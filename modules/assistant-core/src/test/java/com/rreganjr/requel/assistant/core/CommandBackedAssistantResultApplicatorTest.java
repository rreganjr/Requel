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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.IssueSeverity;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.IgnoredFindingStore;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.command.AddGlossaryTermRefererCommand;
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

	/**
	 * A run whose assistant identity does not resolve still has to apply its findings, so the
	 * applicator falls back to the triggering user rather than failing the whole pass (#302).
	 */
	@Test
	void fallsBackToTheTriggeringUserWhenTheAssistantDoesNotResolve() throws Exception {
		String key = "legacy-lexical:Goal:1:note";
		AssistantFindingEntity finding = new AssistantFindingEntity(UUID.randomUUID(), key,
				"legacy-lexical", "Goal", 1L, "note", AssistantFindingState.ACTIVE.name(),
				UUID.randomUUID(), Instant.parse("2026-05-29T00:00:00Z"));
		finding.setAppliedAnnotationId(55L);
		when(findingRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(finding));
		when(annotationRepository.findById(Note.class, 55L)).thenReturn(mock(Note.class));
		DeleteNoteCommand deleteCommand = mock(DeleteNoteCommand.class);
		when(annotationCommandFactory.newDeleteNoteCommand()).thenReturn(deleteCommand);
		when(commandHandler.execute(deleteCommand)).thenReturn(deleteCommand);

		com.rreganjr.requel.user.User triggeringUser = mock(com.rreganjr.requel.user.User.class);
		when(userRepository.findUserByUsername("assistant"))
				.thenThrow(new IllegalStateException("no assistant user on this deployment"));
		when(userRepository.findUserByUsername("ron")).thenReturn(triggeringUser);

		AnnotationAction delete = new AnnotationAction(key, AnnotationAction.ActionType.DELETE_NOTE,
				EntityRef.of("Goal", 1L), null, null, null, null, List.of(), Map.of());
		AssistantResult result = AssistantResult.builder().assistantId("legacy-lexical")
				.annotationAction(delete).build();

		newApplicator().apply(context(), result, CleanupPolicy.MARK_SUPERSEDED,
				EntityRef.of("Goal", 1L));

		verify(deleteCommand).setEditedBy(triggeringUser);
	}

	/** #268: an assistant that didn't finish must not auto-resolve on the strength of it. */
	@Test
	void anIncompleteResultDoesNotReconcileStaleFindings() {
		AssistantFindingEntity stale = new AssistantFindingEntity(UUID.randomUUID(),
				"legacy-lexical:Goal:1:old", "legacy-lexical", "Goal", 1L, "unknown-word",
				AssistantFindingState.ACTIVE.name(), UUID.randomUUID(),
				Instant.parse("2026-05-20T00:00:00Z"));
		when(findingRepository.findByAssistantIdAndTargetTypeAndTargetIdAndState("legacy-lexical",
				"Goal", 1L, AssistantFindingState.ACTIVE.name())).thenReturn(List.of(stale));
		AssistantResult result = AssistantResult.builder().assistantId("legacy-lexical")
				.metadata(Map.of("incomplete", Boolean.TRUE, "failedProperties", List.of("Text")))
				.build();

		newApplicator().apply(context(), result, CleanupPolicy.MARK_SUPERSEDED,
				EntityRef.of("Goal", 1L));

		assertThat(stale.getState()).isEqualTo(AssistantFindingState.ACTIVE.name());
		verify(findingRepository, never()).save(any());
	}

	/** #268: an unresolved issue the old path left, with no ASSISTANT: source, is detached. */
	@Test
	void aLegacyRemovalDetachesAnUnresolvedUnownedIssue() throws Exception {
		Issue legacy = mock(Issue.class);
		when(legacy.isResolved()).thenReturn(false);
		when(legacy.getSource()).thenReturn(null);
		RemoveAnnotationFromAnnotatableCommand command = applyLegacyRemoval(legacy, false);

		verify(command).setAnnotation(legacy);
		verify(commandHandler).execute(command);
		// No finding is involved: the old path never recorded one.
		verify(findingRepository, never()).save(any());
	}

	@Test
	void aLegacyRemovalLeavesAResolvedOrOwnedIssueAlone() throws Exception {
		Issue resolved = mock(Issue.class);
		when(resolved.isResolved()).thenReturn(true);
		applyLegacyRemoval(resolved, false);
		Issue owned = mock(Issue.class);
		when(owned.getSource()).thenReturn("ASSISTANT:legacy-lexical");
		applyLegacyRemoval(owned, false);

		verify(commandHandler, never()).execute(any());
	}

	@Test
	void anIncompleteResultMakesNoLegacyRemovals() throws Exception {
		Issue legacy = mock(Issue.class);
		applyLegacyRemoval(legacy, true);

		verify(commandHandler, never()).execute(any());
	}

	private RemoveAnnotationFromAnnotatableCommand applyLegacyRemoval(Issue issue,
			boolean incomplete) throws Exception {
		EntityRef goalRef = EntityRef.of("Goal", 1L);
		ProjectOrDomainEntity goal = mock(ProjectOrDomainEntity.class);
		AssistantTargetLoader loader = mock(AssistantTargetLoader.class);
		when(loader.supports(goalRef)).thenReturn(true);
		when(loader.loadTarget(goalRef)).thenReturn(Optional.of(goal));
		when(annotationRepository.findAnnotationById(77L)).thenReturn(issue);
		RemoveAnnotationFromAnnotatableCommand command = mock(
				RemoveAnnotationFromAnnotatableCommand.class);
		when(annotationCommandFactory.newRemoveAnnotationFromAnnotatableCommand())
				.thenReturn(command);
		when(commandHandler.execute(command)).thenReturn(command);
		CommandBackedAssistantResultApplicator applicator = new CommandBackedAssistantResultApplicator(
				commandHandler, annotationCommandFactory, projectCommandFactory, annotationRepository,
				userRepository, findingRepository, runRepository, List.of(loader), fixedClock);
		AnnotationAction removal = new AnnotationAction("legacy-lexical:Goal:1:legacy-issue:77",
				AnnotationAction.ActionType.REMOVE_ANNOTATION_FROM_ANNOTATABLE, goalRef, null, null,
				null, null, List.of(), Map.of(
						CommandBackedAssistantResultApplicator.LEGACY_ANNOTATION_ID, 77L));
		AssistantResult.Builder result = AssistantResult.builder().assistantId("legacy-lexical")
				.annotationAction(removal);
		if (incomplete) {
			result.metadata(Map.of("incomplete", Boolean.TRUE));
		}
		applicator.apply(context(), result.build(), CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED,
				goalRef);
		return command;
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
	void glossaryTermRefererActionAddsRefererAsTheAssistant() throws Exception {
		EntityRef refererRef = EntityRef.of("Goal", 1L);
		EntityRef termRef = EntityRef.of("GlossaryTerm", 9L);
		ProjectOrDomainEntity referer = mock(ProjectOrDomainEntity.class);
		GlossaryTerm term = mock(GlossaryTerm.class);
		AssistantTargetLoader loader = mock(AssistantTargetLoader.class);
		when(loader.supports(refererRef)).thenReturn(true);
		when(loader.loadTarget(refererRef)).thenReturn(Optional.of(referer));
		when(loader.supports(termRef)).thenReturn(true);
		when(loader.loadTarget(termRef)).thenReturn(Optional.of(term));
		AddGlossaryTermRefererCommand command = mock(AddGlossaryTermRefererCommand.class);
		when(projectCommandFactory.newAddGlossaryTermRefererCommand()).thenReturn(command);
		when(commandHandler.execute(command)).thenReturn(command);
		// The write is made as the assistant, not as the user whose edit triggered the run
		// (issue #302).
		com.rreganjr.requel.user.User assistantUser = mock(com.rreganjr.requel.user.User.class);
		when(userRepository.findUserByUsername("assistant")).thenReturn(assistantUser);

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
		verify(command).setReferer(referer);
		verify(command).setEditedBy(assistantUser);
		verify(commandHandler).execute(command);
		// A glossary-term referer produces no finding.
		verifyNoInteractions(findingRepository);
	}

	// ---- #271: issue severity ------------------------------------------------

	@Test
	void issueActionSeverityIsWrittenToTheIssue() throws Exception {
		EditIssueCommand command = stubIssueCommand();

		applyIssueAction("ai-review:Goal:1:ambiguous", "HIGH", Map.of("mustResolve", true));

		verify(command).setSeverity(IssueSeverity.HIGH);
		verify(commandHandler).execute(command);
	}

	@Test
	void lowerCaseSeverityIsAccepted() throws Exception {
		EditIssueCommand command = stubIssueCommand();

		applyIssueAction("ai-review:Goal:1:ambiguous", "medium", Map.of());

		verify(command).setSeverity(IssueSeverity.MEDIUM);
	}

	@Test
	void lexicalIssueWithoutSeverityGetsTheKindDefault() throws Exception {
		// The legacy NLP assistants send no severity; null lets the command pick the kind's
		// default (LOW for a lexical issue) on create and leave it unchanged on update.
		EditLexicalIssueCommand command = mock(EditLexicalIssueCommand.class);
		when(annotationCommandFactory.newEditLexicalIssueCommand()).thenReturn(command);
		when(commandHandler.execute(command)).thenReturn(command);
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(42L);
		when(command.getIssue()).thenReturn(issue);

		applyIssueAction("legacy-lexical:Goal:1:spelling:Name:zorblat", null,
				Map.of("kind", "LEXICAL", "word", "zorblat"));

		verify(command).setSeverity(null);
		verify(commandHandler).execute(command);
	}

	@Test
	void unknownSeverityFallsBackToTheDefaultAndKeepsTheRawValueOnTheFinding() throws Exception {
		// Lenient on the assistant path: a bad model reply must not drop a real finding.
		EditIssueCommand command = stubIssueCommand();

		applyIssueAction("ai-review:Goal:1:ambiguous", "urgent", Map.of());

		verify(command).setSeverity(null);
		verify(commandHandler).execute(command);
		verify(findingRepository, atLeastOnce())
				.save(argThat((AssistantFindingEntity f) -> "urgent".equals(f.getSeverity())));
	}

	// ---- #268: one glossary candidate per project ----------------------------

	private static final String GLOSSARY = "legacy-lexical-glossary-term";
	private static final Map<String, Object> SHARED = Map.of("kind", "LEXICAL",
			"word", "admin console", "findingType", "glossary-term", "scope", "PROJECT",
			"shareKey", "glossary-term:admin console");

	@Test
	void aProjectScopedFindingJoinsTheOpenIssueAnotherEntityAlreadyHas() throws Exception {
		EditLexicalIssueCommand command = stubLexicalIssueCommand();
		Issue shared = sharedIssue(77L, false);

		applyIssueAction(GLOSSARY + ":Goal:1:glossary-term:admin console", null, SHARED);

		verify(command).setIssue(shared);
		verify(commandHandler).execute(command);
	}

	@Test
	void aResolvedSharedIssueIsNotJoined() throws Exception {
		EditLexicalIssueCommand command = stubLexicalIssueCommand();
		sharedIssue(77L, true);

		applyIssueAction(GLOSSARY + ":Goal:1:glossary-term:admin console", null, SHARED);

		verify(command, never()).setIssue(any());
		verify(commandHandler).execute(command);
	}

	@Test
	void aFindingWithoutProjectScopeNeverJoinsAnotherEntitysIssue() throws Exception {
		EditLexicalIssueCommand command = stubLexicalIssueCommand();
		sharedIssue(77L, false);

		applyIssueAction(GLOSSARY + ":Goal:1:glossary-term:admin console", null,
				Map.of("kind", "LEXICAL", "word", "admin console", "shareKey",
						"glossary-term:admin console"));

		verify(command, never()).setIssue(any());
	}

	@Test
	void ignoringAGlossaryCandidateOnOneEntityIgnoresItOnEveryEntity() throws Exception {
		stubLexicalIssueCommand();
		IgnoredFindingStore store = mock(IgnoredFindingStore.class);
		when(store.ignoredKeys(7L))
				.thenReturn(Set.of(GLOSSARY + ":story:2:glossary-term:admin console"));

		applyIssueAction(GLOSSARY + ":Goal:1:glossary-term:Admin Console", null, SHARED, store);

		verify(commandHandler, never()).execute(any());
	}

	@Test
	void anotherAssistantsIgnoreWithTheSameSuffixDoesNotApply() throws Exception {
		EditLexicalIssueCommand command = stubLexicalIssueCommand();
		IgnoredFindingStore store = mock(IgnoredFindingStore.class);
		when(store.ignoredKeys(7L))
				.thenReturn(Set.of("legacy-lexical:story:2:glossary-term:admin console"));

		applyIssueAction(GLOSSARY + ":Goal:1:glossary-term:admin console", null, SHARED, store);

		verify(commandHandler).execute(command);
	}

	private EditLexicalIssueCommand stubLexicalIssueCommand() throws Exception {
		EditLexicalIssueCommand command = mock(EditLexicalIssueCommand.class);
		when(annotationCommandFactory.newEditLexicalIssueCommand()).thenReturn(command);
		when(commandHandler.execute(command)).thenReturn(command);
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(42L);
		when(command.getIssue()).thenReturn(issue);
		return command;
	}

	/** An issue another entity's (Story 2) ACTIVE glossary finding applied. */
	private Issue sharedIssue(Long id, boolean resolved) {
		AssistantFindingEntity finding = new AssistantFindingEntity(UUID.randomUUID(),
				GLOSSARY + ":Story:2:glossary-term:Admin Console", GLOSSARY, "Story", 2L,
				"glossary-term", AssistantFindingState.ACTIVE.name(), UUID.randomUUID(),
				Instant.parse("2026-05-29T00:00:00Z"));
		finding.setAppliedAnnotationId(id);
		when(findingRepository.findByAssistantIdAndProjectIdAndState(GLOSSARY, 7L,
				AssistantFindingState.ACTIVE.name())).thenReturn(List.of(finding));
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(id);
		when(issue.isResolved()).thenReturn(resolved);
		when(annotationRepository.findById(Issue.class, id)).thenReturn(issue);
		return issue;
	}

	private EditIssueCommand stubIssueCommand() throws Exception {
		EditIssueCommand command = mock(EditIssueCommand.class);
		when(annotationCommandFactory.newEditIssueCommand()).thenReturn(command);
		when(commandHandler.execute(command)).thenReturn(command);
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(42L);
		when(command.getIssue()).thenReturn(issue);
		return command;
	}

	private void applyIssueAction(String key, String severity, Map<String, Object> metadata) {
		applyIssueAction(key, severity, metadata, null);
	}

	private void applyIssueAction(String key, String severity, Map<String, Object> metadata,
			IgnoredFindingStore ignoredFindingStore) {
		EntityRef target = EntityRef.of("Goal", 1L);
		ProjectOrDomainEntity goal = mock(ProjectOrDomainEntity.class);
		AssistantTargetLoader loader = mock(AssistantTargetLoader.class);
		when(loader.supports(target)).thenReturn(true);
		when(loader.loadTarget(target)).thenReturn(Optional.of(goal));
		when(findingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		CommandBackedAssistantResultApplicator applicator = new CommandBackedAssistantResultApplicator(
				commandHandler, annotationCommandFactory, projectCommandFactory, annotationRepository,
				userRepository, findingRepository, runRepository, List.of(loader), fixedClock);
		if (ignoredFindingStore != null) {
			applicator.setIgnoredFindingStore(ignoredFindingStore);
		}

		AnnotationAction action = new AnnotationAction(key,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, target, null,
				"'fast' is not measurable", severity, 0.7, List.of(), metadata);
		AssistantResult result = AssistantResult.builder().assistantId(key.split(":")[0])
				.annotationAction(action).build();

		applicator.apply(context(), result, CleanupPolicy.MANUAL, target);
	}

	private static AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "ron"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", 7L), Locale.US,
				Clock.systemUTC(), Map.of());
	}
}
