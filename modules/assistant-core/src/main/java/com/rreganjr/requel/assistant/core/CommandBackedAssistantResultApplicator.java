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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Argument;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.NoSuchPositionException;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteArgumentCommand;
import com.rreganjr.requel.annotation.command.DeleteIssueCommand;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.annotation.command.DeletePositionCommand;
import com.rreganjr.requel.annotation.command.EditArgumentCommand;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.annotation.command.EditChangeSpellingPositionCommand;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingState;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.command.EditAddActorToProjectPositionCommand;
import com.rreganjr.requel.project.command.EditAddWordToGlossaryPositionCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.user.UserRepository;

/**
 * Command-backed applicator: turns each {@link AnnotationAction} into a call
 * through the existing command + {@link CommandHandler} chain so authorization,
 * validation, audit, optimistic locking, and SSE behave exactly as they do for
 * UI-driven edits. Commands are executed as the triggering user (resolved by
 * username), which is what {@code AuthorizingCommandHandler} checks via
 * {@code getEditedBy()}.
 *
 * <p>
 * Most actions map to {@link AnnotationCommandFactory} (notes, issues, lexical
 * issues, and the change-spelling / add-word-to-dictionary / plain positions).
 * The project-scoped positions an assistant can raise &mdash; "add word to
 * glossary" and "add actor to project", selected via {@code metadata.kind} on a
 * {@code CREATE_OR_UPDATE_POSITION} action &mdash; are resolve-positions that
 * mutate the project when accepted, so they are created through
 * {@link ProjectCommandFactory} and carry the owning {@code ProjectOrDomain}
 * (taken from the parent issue's grouping object). Both factories are reached
 * through the same {@link CommandHandler}, so authorization and audit are
 * identical regardless of which one produced the command.
 *
 * <p>
 * Idempotency: each primary action (note / issue) is keyed by its
 * {@code actionKey} in the {@code assistant_findings} table. A re-run with the
 * same key "touches" the existing finding (updates last-seen) rather than
 * duplicating it; content-level dedupe through the annotation repository's
 * find-by-text lookups reuses the same annotation. The richer state machine
 * (SUPERSEDED / AUTO_RESOLVED / MANUALLY_RESOLVED) and the
 * RESOLVE/DELETE/REMOVE action types are implemented in a later phase
 * (doc/43-phase-4.5-plan.md, Step 6); this applicator skips those action types
 * for now rather than failing the run.
 */
@Component
@Primary
public class CommandBackedAssistantResultApplicator implements AssistantResultApplicator {

	private static final Logger log = LoggerFactory
			.getLogger(CommandBackedAssistantResultApplicator.class);

	private static final int MAX_TEXT_LENGTH = 4000;
	private static final int MAX_SUMMARY_LENGTH = 500;

	private final CommandHandler commandHandler;
	private final AnnotationCommandFactory annotationCommandFactory;
	private final ProjectCommandFactory projectCommandFactory;
	private final AnnotationRepository annotationRepository;
	private final UserRepository userRepository;
	private final AssistantFindingRepository findingRepository;
	private final AssistantRunRepository runRepository;
	private final List<AssistantTargetLoader> targetLoaders;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Clock clock;

	@Autowired
	public CommandBackedAssistantResultApplicator(@Lazy CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			ProjectCommandFactory projectCommandFactory,
			AnnotationRepository annotationRepository, UserRepository userRepository,
			AssistantFindingRepository findingRepository, AssistantRunRepository runRepository,
			List<AssistantTargetLoader> targetLoaders) {
		this(commandHandler, annotationCommandFactory, projectCommandFactory, annotationRepository,
				userRepository, findingRepository, runRepository, targetLoaders, Clock.systemUTC());
	}

	CommandBackedAssistantResultApplicator(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			ProjectCommandFactory projectCommandFactory,
			AnnotationRepository annotationRepository, UserRepository userRepository,
			AssistantFindingRepository findingRepository, AssistantRunRepository runRepository,
			List<AssistantTargetLoader> targetLoaders, Clock clock) {
		this.commandHandler = Objects.requireNonNull(commandHandler, "commandHandler");
		this.annotationCommandFactory = Objects.requireNonNull(annotationCommandFactory,
				"annotationCommandFactory");
		this.projectCommandFactory = Objects.requireNonNull(projectCommandFactory,
				"projectCommandFactory");
		this.annotationRepository = Objects.requireNonNull(annotationRepository,
				"annotationRepository");
		this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
		this.findingRepository = Objects.requireNonNull(findingRepository, "findingRepository");
		this.runRepository = Objects.requireNonNull(runRepository, "runRepository");
		this.targetLoaders = List.copyOf(targetLoaders);
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	@Override
	public AppliedAssistantResult apply(AssistantContext context, AssistantResult result,
			CleanupPolicy cleanupPolicy, EntityRef dispatchTarget) {
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(result, "result");

		User editedBy = resolveUser(context);
		String source = "ASSISTANT:" + result.assistantId();
		List<Long> annotationIds = new ArrayList<Long>();
		// Annotations created earlier in this same result, keyed by action key,
		// so a position can attach to an issue with no persisted id yet.
		Map<String, Object> createdByActionKey = new HashMap<String, Object>();
		// Finding keys this run produced, grouped by the entity they target, so
		// stale findings on each target can be reconciled afterward.
		Map<EntityRef, Set<String>> producedKeysByTarget = new HashMap<EntityRef, Set<String>>();
		int newFindings = 0;

		for (AnnotationAction action : result.annotationActions()) {
			try {
				if (isCleanupAction(action.actionType())) {
					// Cleanup actions (resolve / delete / remove) reference an annotation a
					// prior finding created, by the same actionKey; they execute the matching
					// command and transition the finding rather than producing a new one.
					applyCleanupAction(action, editedBy, createdByActionKey);
					continue;
				}
				AppliedAction applied = applyAction(context, result, action, editedBy,
						createdByActionKey);
				if (applied == null) {
					continue;
				}
				// Stamp provenance (source label + idempotency key) on the created /
				// updated annotation for reverse lookup and source labeling. The entity
				// is managed in this transaction, so the change flushes on commit.
				stampProvenance(createdByActionKey.get(action.actionKey()), source,
						action.actionKey());
				if (applied.annotationId() != null) {
					annotationIds.add(applied.annotationId());
				}
				if (action.targetRef() != null) {
					producedKeysByTarget
							.computeIfAbsent(action.targetRef(), key -> new HashSet<String>())
							.add(action.actionKey());
					if (upsertFinding(context, result, action, applied.annotationId())) {
						newFindings++;
					}
				}
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalStateException("Failed to apply assistant action "
						+ action.actionKey(), e);
			}
		}

		bumpFindingsCount(context.runId(), newFindings);
		reconcileStaleFindings(result.assistantId(), cleanupPolicy, dispatchTarget,
				producedKeysByTarget, editedBy, context.runId());
		return new AppliedAssistantResult(annotationIds.size(), annotationIds);
	}

	private static boolean isCleanupAction(AnnotationAction.ActionType type) {
		switch (type) {
			case RESOLVE_ISSUE:
			case DELETE_NOTE:
			case DELETE_ISSUE:
			case DELETE_POSITION:
			case DELETE_ARGUMENT:
			case REMOVE_ANNOTATION_FROM_ANNOTATABLE:
				return true;
			default:
				return false;
		}
	}

	private AppliedAction applyAction(AssistantContext context, AssistantResult result,
			AnnotationAction action, User editedBy, Map<String, Object> createdByActionKey)
			throws Exception {
		switch (action.actionType()) {
			case CREATE_OR_UPDATE_NOTE:
				return applyNote(action, editedBy, createdByActionKey);
			case CREATE_OR_UPDATE_ISSUE:
				return applyIssue(action, editedBy, createdByActionKey);
			case CREATE_OR_UPDATE_POSITION:
				return applyPosition(action, editedBy, createdByActionKey);
			case CREATE_OR_UPDATE_ARGUMENT:
				return applyArgument(action, editedBy, createdByActionKey);
			default:
				log.info("Skipping unhandled create action type {} (key {})", action.actionType(),
						action.actionKey());
				return null;
		}
	}

	// ---- cleanup actions (resolve / delete / remove) --------------------------

	/**
	 * Apply a cleanup action against an annotation a prior finding created. The
	 * annotation is resolved by the action's {@code actionKey} (the same key used
	 * when it was created), via the finding's {@code applied_annotation_id}. When
	 * the annotation can no longer be resolved (already gone) the action is a no-op.
	 * On success the linked finding is transitioned: {@code DROPPED} for delete /
	 * remove, {@code AUTO_RESOLVED} for an assistant-initiated resolve.
	 */
	private void applyCleanupAction(AnnotationAction action, User editedBy,
			Map<String, Object> createdByActionKey) throws Exception {
		switch (action.actionType()) {
			case DELETE_NOTE:
				deleteNote(action, editedBy);
				break;
			case DELETE_ISSUE:
				deleteIssue(action, editedBy);
				break;
			case DELETE_POSITION:
				deletePosition(action, editedBy);
				break;
			case DELETE_ARGUMENT:
				deleteArgument(action, editedBy);
				break;
			case REMOVE_ANNOTATION_FROM_ANNOTATABLE:
				removeAnnotationFromAnnotatable(action, editedBy);
				break;
			case RESOLVE_ISSUE:
				resolveIssue(action, editedBy, createdByActionKey);
				break;
			default:
				break;
		}
	}

	private void deleteNote(AnnotationAction action, User editedBy) throws Exception {
		Note note = loadExistingAnnotation(action.actionKey(), Note.class);
		if (note == null) {
			logCleanupSkip(action, "note");
			return;
		}
		DeleteNoteCommand command = annotationCommandFactory.newDeleteNoteCommand();
		command.setNote(note);
		command.setEditedBy(editedBy);
		commandHandler.execute(command);
		transitionFinding(action.actionKey(), AssistantFindingState.DROPPED);
	}

	private void deleteIssue(AnnotationAction action, User editedBy) throws Exception {
		Issue issue = loadExistingAnnotation(action.actionKey(), Issue.class);
		if (issue == null) {
			logCleanupSkip(action, "issue");
			return;
		}
		DeleteIssueCommand command = annotationCommandFactory.newDeleteIssueCommand();
		command.setIssue(issue);
		command.setEditedBy(editedBy);
		commandHandler.execute(command);
		transitionFinding(action.actionKey(), AssistantFindingState.DROPPED);
	}

	private void deletePosition(AnnotationAction action, User editedBy) throws Exception {
		Position position = loadExistingAnnotation(action.actionKey(), Position.class);
		if (position == null) {
			logCleanupSkip(action, "position");
			return;
		}
		DeletePositionCommand command = annotationCommandFactory.newDeletePositionCommand();
		command.setPosition(position);
		command.setEditedBy(editedBy);
		commandHandler.execute(command);
		transitionFinding(action.actionKey(), AssistantFindingState.DROPPED);
	}

	private void deleteArgument(AnnotationAction action, User editedBy) throws Exception {
		Argument argument = loadExistingAnnotation(action.actionKey(), Argument.class);
		if (argument == null) {
			logCleanupSkip(action, "argument");
			return;
		}
		DeleteArgumentCommand command = annotationCommandFactory.newDeleteArgumentCommand();
		command.setArgument(argument);
		command.setEditedBy(editedBy);
		commandHandler.execute(command);
		transitionFinding(action.actionKey(), AssistantFindingState.DROPPED);
	}

	private void removeAnnotationFromAnnotatable(AnnotationAction action, User editedBy)
			throws Exception {
		Annotation annotation = loadExistingAnnotationById(action.actionKey());
		Annotatable annotatable = resolveAnnotatable(action.targetRef());
		if (annotation == null || annotatable == null) {
			logCleanupSkip(action, "annotation/annotatable");
			return;
		}
		RemoveAnnotationFromAnnotatableCommand command = annotationCommandFactory
				.newRemoveAnnotationFromAnnotatableCommand();
		command.setAnnotation(annotation);
		command.setAnnotatable(annotatable);
		command.setEditedBy(editedBy);
		commandHandler.execute(command);
		transitionFinding(action.actionKey(), AssistantFindingState.DROPPED);
	}

	private void resolveIssue(AnnotationAction action, User editedBy,
			Map<String, Object> createdByActionKey) throws Exception {
		Issue issue = loadExistingAnnotation(action.actionKey(), Issue.class);
		Position resolvingPosition = parentOfType(action, createdByActionKey, Position.class);
		Annotatable annotatable = resolveAnnotatable(action.targetRef());
		if (issue == null || resolvingPosition == null || annotatable == null) {
			log.info("Skipping resolve-issue action {} — issue, resolving position (parent key {}),"
					+ " or annotatable {} did not resolve", action.actionKey(),
					action.parentActionKey(), action.targetRef());
			return;
		}
		ResolveIssueCommand command = annotationCommandFactory
				.newResolveIssueCommand(resolvingPosition);
		command.setIssue(issue);
		command.setPosition(resolvingPosition);
		command.setAnnotatable(annotatable);
		command.setEditedBy(editedBy);
		commandHandler.execute(command);
		transitionFinding(action.actionKey(), AssistantFindingState.AUTO_RESOLVED);
	}

	private void logCleanupSkip(AnnotationAction action, String what) {
		log.info("Skipping {} action {} — no existing {} resolved for key", action.actionType(),
				action.actionKey(), what);
	}

	/**
	 * Move the finding identified by {@code actionKey} to {@code state} with a
	 * close timestamp. No-op when no finding matches the key.
	 */
	private void transitionFinding(String actionKey, AssistantFindingState state) {
		findingRepository.findByIdempotencyKey(actionKey).ifPresent(finding -> {
			finding.setState(state.name());
			finding.setClosedAt(clock.instant());
			findingRepository.save(finding);
		});
	}

	/** Load the annotation a finding created (by action key), concrete type unknown. */
	private Annotation loadExistingAnnotationById(String actionKey) {
		return findingRepository.findByIdempotencyKey(actionKey)
				.map(AssistantFindingEntity::getAppliedAnnotationId)
				.map(annotationRepository::findAnnotationById).orElse(null);
	}

	private AppliedAction applyNote(AnnotationAction action, User editedBy,
			Map<String, Object> createdByActionKey) throws Exception {
		Annotatable annotatable = resolveAnnotatable(action.targetRef());
		if (annotatable == null) {
			log.info("Skipping note action {} — target {} did not resolve", action.actionKey(),
					action.targetRef());
			return null;
		}
		Object grouping = grouping(annotatable);
		String text = truncate(action.text(), MAX_TEXT_LENGTH);
		EditNoteCommand command = annotationCommandFactory.newEditNoteCommand();
		Note existing = loadExistingAnnotation(action.actionKey(), Note.class);
		if (existing != null) {
			command.setNote(existing);
		}
		command.setGroupingObject(grouping);
		command.setText(text);
		command.setAnnotatable(annotatable);
		command.setEditedBy(editedBy);
		command = commandHandler.execute(command);
		Note note = command.getNote();
		createdByActionKey.put(action.actionKey(), note);
		return new AppliedAction(note.getId());
	}

	private AppliedAction applyIssue(AnnotationAction action, User editedBy,
			Map<String, Object> createdByActionKey) throws Exception {
		Annotatable annotatable = resolveAnnotatable(action.targetRef());
		if (annotatable == null) {
			log.info("Skipping issue action {} — target {} did not resolve", action.actionKey(),
					action.targetRef());
			return null;
		}
		Object grouping = grouping(annotatable);
		String text = truncate(action.text(), MAX_TEXT_LENGTH);
		boolean mustResolve = booleanMeta(action, "mustResolve", true);
		String kind = stringMeta(action, "kind");

		// Idempotency is keyed on the action key via the AssistantFinding's applied
		// annotation id, not on content. This lets the same word raise distinct issues
		// from different assistants/finding-types without one overwriting another, and
		// avoids hijacking human-authored annotations with matching text.
		Issue existing = loadExistingAnnotation(action.actionKey(), Issue.class);

		Issue issue;
		if ("LEXICAL".equalsIgnoreCase(kind)) {
			EditLexicalIssueCommand command = annotationCommandFactory.newEditLexicalIssueCommand();
			String word = stringMeta(action, "word");
			String propertyName = stringMeta(action, "annotatableEntityPropertyName");
			if (existing != null) {
				command.setIssue(existing);
			}
			if (word != null && !word.isBlank()) {
				command.setWord(word);
			}
			if (propertyName != null) {
				command.setAnnotatableEntityPropertyName(propertyName);
			}
			command.setGroupingObject(grouping);
			command.setText(text);
			command.setMustBeResolved(mustResolve);
			command.setAnnotatable(annotatable);
			command.setEditedBy(editedBy);
			command = commandHandler.execute(command);
			issue = command.getIssue();
		} else {
			EditIssueCommand command = annotationCommandFactory.newEditIssueCommand();
			if (existing != null) {
				command.setIssue(existing);
			}
			command.setGroupingObject(grouping);
			command.setText(text);
			command.setMustBeResolved(mustResolve);
			command.setAnnotatable(annotatable);
			command.setEditedBy(editedBy);
			command = commandHandler.execute(command);
			issue = command.getIssue();
		}
		createdByActionKey.put(action.actionKey(), issue);
		return new AppliedAction(issue.getId());
	}

	private AppliedAction applyPosition(AnnotationAction action, User editedBy,
			Map<String, Object> createdByActionKey) throws Exception {
		Issue issue = parentOfType(action, createdByActionKey, Issue.class);
		if (issue == null) {
			log.info("Skipping position action {} — parent issue (key {}) not resolved in result",
					action.actionKey(), action.parentActionKey());
			return null;
		}
		Object grouping = issue.getGroupingObject();
		String text = truncate(action.text(), MAX_TEXT_LENGTH);
		String kind = stringMeta(action, "kind");

		// Project-specific positions go through ProjectCommandFactory and carry the
		// owning ProjectOrDomain. The legacy assistant created these without a
		// content dedupe, so we do not look up an existing position for them.
		if ("ADD_WORD_TO_GLOSSARY".equalsIgnoreCase(kind)
				|| "ADD_ACTOR_TO_PROJECT".equalsIgnoreCase(kind)) {
			return applyProjectPosition(kind, issue, grouping, text, editedBy, action,
					createdByActionKey);
		}

		EditPositionCommand command;
		if ("CHANGE_SPELLING".equalsIgnoreCase(kind)) {
			EditChangeSpellingPositionCommand changeSpelling = annotationCommandFactory
					.newEditChangeSpellingPositionCommand();
			String proposedWord = stringMeta(action, "proposedWord");
			if (proposedWord != null) {
				changeSpelling.setProposedWord(proposedWord);
			}
			command = changeSpelling;
		} else if ("ADD_WORD_TO_DICTIONARY".equalsIgnoreCase(kind)) {
			command = annotationCommandFactory.newEditAddWordToDictionaryPositionCommand();
		} else {
			command = annotationCommandFactory.newEditPositionCommand();
		}

		Position existing = tryFindPosition(grouping, text);
		if (existing != null) {
			command.setPosition(existing);
		}
		command.setIssue(issue);
		command.setText(text);
		command.setEditedBy(editedBy);
		command = commandHandler.execute(command);
		Position position = command.getPosition();
		createdByActionKey.put(action.actionKey(), position);
		return new AppliedAction(position.getId());
	}

	/**
	 * Apply an "add word to glossary" / "add actor to project" position. These
	 * are resolve-positions tied to project commands (they mutate the project when
	 * a user accepts them), so they are created through {@link ProjectCommandFactory}
	 * and carry the owning {@link ProjectOrDomain}.
	 */
	private AppliedAction applyProjectPosition(String kind, Issue issue, Object grouping,
			String text, User editedBy, AnnotationAction action,
			Map<String, Object> createdByActionKey) throws Exception {
		if (!(grouping instanceof ProjectOrDomain projectOrDomain)) {
			log.info("Skipping project position action {} — issue grouping is not a ProjectOrDomain",
					action.actionKey());
			return null;
		}
		EditPositionCommand command;
		if ("ADD_WORD_TO_GLOSSARY".equalsIgnoreCase(kind)) {
			EditAddWordToGlossaryPositionCommand glossary = projectCommandFactory
					.newEditAddWordToGlossaryPositionCommand();
			glossary.setProjectOrDomain(projectOrDomain);
			command = glossary;
		} else {
			EditAddActorToProjectPositionCommand actor = projectCommandFactory
					.newEditAddActorToProjectPositionCommand();
			actor.setProjectOrDomain(projectOrDomain);
			command = actor;
		}
		command.setIssue(issue);
		command.setText(text);
		command.setEditedBy(editedBy);
		command = commandHandler.execute(command);
		Position position = command.getPosition();
		createdByActionKey.put(action.actionKey(), position);
		return new AppliedAction(position.getId());
	}

	private AppliedAction applyArgument(AnnotationAction action, User editedBy,
			Map<String, Object> createdByActionKey) throws Exception {
		Position position = parentOfType(action, createdByActionKey, Position.class);
		if (position == null) {
			log.info("Skipping argument action {} — parent position (key {}) not resolved",
					action.actionKey(), action.parentActionKey());
			return null;
		}
		String text = truncate(action.text(), MAX_TEXT_LENGTH);
		EditArgumentCommand command = annotationCommandFactory.newEditArgumentCommand();
		command.setPosition(position);
		command.setText(text);
		String supportLevel = stringMeta(action, "supportLevel");
		if (supportLevel != null) {
			command.setSupportLevelName(supportLevel);
		}
		command.setEditedBy(editedBy);
		command = commandHandler.execute(command);
		Argument argument = command.getArgument();
		createdByActionKey.put(action.actionKey(), argument);
		return new AppliedAction(argument.getId());
	}

	/**
	 * Stamp the assistant provenance ({@code source} = {@code ASSISTANT:<id>} and
	 * the idempotency key) on a created/updated annotation. The annotation is
	 * managed in the current transaction, so the change flushes on commit; this
	 * gives the UI a source label and provides a fast finding -> annotation reverse
	 * lookup without joining the assistant tables.
	 */
	private static void stampProvenance(Object annotation, String source, String idempotencyKey) {
		if (annotation instanceof AbstractAnnotation persistentAnnotation) {
			persistentAnnotation.setSource(source);
			persistentAnnotation.setAssistantIdempotencyKey(idempotencyKey);
		}
	}

	/**
	 * Resolve the annotation a finding previously created, for key-based
	 * (not content-based) update. Looks up the {@link AssistantFindingEntity} by the
	 * action key and loads the annotation it points at; returns {@code null} for a
	 * first-time finding or when the linked annotation no longer exists.
	 */
	private <T> T loadExistingAnnotation(String actionKey, Class<T> annotationType) {
		return findingRepository.findByIdempotencyKey(actionKey)
				.map(AssistantFindingEntity::getAppliedAnnotationId)
				.map(annotationId -> annotationRepository.findById(annotationType, annotationId))
				.orElse(null);
	}

	// ---- finding upsert -------------------------------------------------------

	/**
	 * @return true if a new finding row was created (vs touching an existing one).
	 */
	private boolean upsertFinding(AssistantContext context, AssistantResult result,
			AnnotationAction action, Long annotationId) {
		Instant now = clock.instant();
		Optional<AssistantFindingEntity> existing = findingRepository
				.findByIdempotencyKey(action.actionKey());
		if (existing.isPresent()) {
			AssistantFindingEntity finding = existing.get();
			finding.setLastSeenRunId(context.runId().toString());
			finding.setLastSeenAt(now);
			if (annotationId != null) {
				finding.setAppliedAnnotationId(annotationId);
			}
			findingRepository.save(finding);
			return false;
		}

		EntityRef targetRef = action.targetRef();
		AssistantFindingEntity finding = new AssistantFindingEntity(UUID.randomUUID(),
				action.actionKey(), result.assistantId(), targetRef.entityType(),
				targetRef.entityId(), findingType(action), AssistantFindingState.ACTIVE.name(),
				context.runId(), now);
		if (context.projectRef() != null) {
			finding.setProjectId(context.projectRef().entityId());
		}
		finding.setSeverity(action.severity());
		if (action.confidence() != null) {
			finding.setConfidence(BigDecimal.valueOf(action.confidence())
					.setScale(3, RoundingMode.HALF_UP));
		}
		finding.setSummary(truncate(action.text(), MAX_SUMMARY_LENGTH));
		finding.setEvidenceJson(evidenceJson(action));
		finding.setAppliedAnnotationId(annotationId);
		findingRepository.save(finding);
		return true;
	}

	private void bumpFindingsCount(UUID runId, int newFindings) {
		if (newFindings <= 0) {
			return;
		}
		runRepository.findById(runId.toString()).ifPresent(run -> {
			run.setFindingsCount(run.getFindingsCount() + newFindings);
			runRepository.save(run);
		});
	}

	// ---- stale-finding reconciliation -----------------------------------------

	/**
	 * Reconcile previously-recorded {@code ACTIVE} findings for this assistant
	 * against what the current run produced. Behaviour depends on the assistant's
	 * {@link CleanupPolicy}:
	 * <ul>
	 * <li>{@link CleanupPolicy#AUTO_RESOLVE_IF_UNTOUCHED} — remove the annotation and
	 * mark the finding {@code AUTO_RESOLVED}, but only if it is still assistant-owned
	 * and untouched by a human (see {@link #autoResolveIfUntouched}).</li>
	 * <li>{@link CleanupPolicy#MARK_SUPERSEDED} (the default) — mark the finding
	 * {@code SUPERSEDED} (stamped with {@code superseded_by_run_id} = this run) and
	 * leave the annotation in place; the finding is kept for history.</li>
	 * <li>{@link CleanupPolicy#MANUAL} — never auto-transition; operator-managed.</li>
	 * </ul>
	 *
	 * <p>
	 * Reconciliation is per target entity. The set of targets to check is the
	 * union of every entity this run raised an action against and the original
	 * dispatch target (so a re-run that produces <em>no</em> actions still reconciles
	 * the prior findings on the entity that was analyzed). For each prior
	 * {@code ACTIVE} finding on a target whose idempotency key the current run did
	 * not re-emit, the policy-specific transition is applied.
	 */
	private void reconcileStaleFindings(String assistantId, CleanupPolicy cleanupPolicy,
			EntityRef dispatchTarget, Map<EntityRef, Set<String>> producedKeysByTarget,
			User editedBy, UUID runId) {
		if (cleanupPolicy != CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED
				&& cleanupPolicy != CleanupPolicy.MARK_SUPERSEDED) {
			// MANUAL (or any future operator-managed policy): leave findings as-is.
			return;
		}
		Set<EntityRef> targets = new HashSet<>(producedKeysByTarget.keySet());
		if (dispatchTarget != null) {
			targets.add(dispatchTarget);
		}
		for (EntityRef target : targets) {
			Set<String> producedKeys = producedKeysByTarget.getOrDefault(target,
					java.util.Collections.<String>emptySet());
			List<AssistantFindingEntity> priorActive = findingRepository
					.findByAssistantIdAndTargetTypeAndTargetIdAndState(assistantId,
							target.entityType(), target.entityId(),
							AssistantFindingState.ACTIVE.name());
			for (AssistantFindingEntity finding : priorActive) {
				if (producedKeys.contains(finding.getIdempotencyKey())) {
					continue;
				}
				if (cleanupPolicy == CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED) {
					autoResolveIfUntouched(finding, target, editedBy);
				} else {
					markSuperseded(finding, runId);
				}
			}
		}
	}

	/**
	 * Mark a stale finding {@code SUPERSEDED}: record the run that superseded it and
	 * close it, leaving its annotation untouched. Used by the default
	 * {@link CleanupPolicy#MARK_SUPERSEDED} when a re-run no longer reports the finding.
	 */
	private void markSuperseded(AssistantFindingEntity finding, UUID runId) {
		finding.setState(AssistantFindingState.SUPERSEDED.name());
		if (runId != null) {
			finding.setSupersededByRunId(runId.toString());
		}
		finding.setClosedAt(clock.instant());
		findingRepository.save(finding);
	}

	/**
	 * Auto-resolve one stale finding: if its applied annotation is still present
	 * and {@link #isUntouched untouched} by a human, remove the annotation from
	 * its target and mark the finding {@code AUTO_RESOLVED}. If the annotation was
	 * edited or resolved by a human (or already gone) the finding is left
	 * {@code ACTIVE} so the human's work is preserved.
	 */
	private void autoResolveIfUntouched(AssistantFindingEntity finding, EntityRef target,
			User editedBy) {
		Long annotationId = finding.getAppliedAnnotationId();
		if (annotationId == null) {
			// Nothing was applied; the finding is purely advisory. Close it.
			markAutoResolved(finding);
			return;
		}
		Annotation annotation = annotationRepository.findAnnotationById(annotationId);
		if (annotation == null) {
			// Annotation already removed elsewhere; the finding is moot.
			markAutoResolved(finding);
			return;
		}
		if (!isUntouched(annotation)) {
			// A human edited or resolved it — leave it (and the finding) alone.
			return;
		}
		Annotatable annotatable = resolveAnnotatable(target);
		if (annotatable == null) {
			log.info("Cannot auto-resolve finding {} — target {} did not resolve",
					finding.getIdempotencyKey(), target);
			return;
		}
		try {
			RemoveAnnotationFromAnnotatableCommand command = annotationCommandFactory
					.newRemoveAnnotationFromAnnotatableCommand();
			command.setAnnotation(annotation);
			command.setAnnotatable(annotatable);
			command.setEditedBy(editedBy);
			commandHandler.execute(command);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to auto-resolve assistant finding "
					+ finding.getIdempotencyKey(), e);
		}
		markAutoResolved(finding);
	}

	private void markAutoResolved(AssistantFindingEntity finding) {
		finding.setState(AssistantFindingState.AUTO_RESOLVED.name());
		finding.setClosedAt(clock.instant());
		findingRepository.save(finding);
	}

	/**
	 * An annotation is "untouched" — safe to auto-remove — when it is still
	 * assistant-owned and unresolved: its {@code source} is an {@code ASSISTANT:}
	 * label and it is not resolved. A resolved annotation means a human acted on
	 * the finding, so it is preserved. This mirrors the legacy
	 * {@code removeUnneededLexicalIssues}, which removed a no-longer-relevant
	 * lexical issue only when it was unresolved.
	 *
	 * <p>
	 * Positions are not inspected: {@code PositionImpl} is not an
	 * {@link AbstractAnnotation}, so it carries no provenance {@code source}, and
	 * commands run as the triggering user, so a position's {@code createdBy} cannot
	 * distinguish an assistant-authored position from a human one. The resolved
	 * flag is the reliable human-engagement signal.
	 */
	private static boolean isUntouched(Annotation annotation) {
		if (!isAssistantSourced(annotation)) {
			return false;
		}
		return !annotation.isResolved();
	}

	private static boolean isAssistantSourced(Annotation annotation) {
		if (annotation instanceof AbstractAnnotation persistentAnnotation) {
			String source = persistentAnnotation.getSource();
			return source != null && source.startsWith("ASSISTANT:");
		}
		return false;
	}

	// ---- resolution helpers ---------------------------------------------------

	private Annotatable resolveAnnotatable(EntityRef ref) {
		if (ref == null) {
			return null;
		}
		for (AssistantTargetLoader loader : targetLoaders) {
			if (loader.supports(ref)) {
				Optional<Object> target = loader.loadTarget(ref);
				if (target.isPresent() && target.get() instanceof Annotatable annotatable) {
					return annotatable;
				}
			}
		}
		return null;
	}

	private static Object grouping(Annotatable annotatable) {
		if (annotatable instanceof ProjectOrDomainEntity entity) {
			return entity.getProjectOrDomain();
		}
		return annotatable;
	}

	private User resolveUser(AssistantContext context) {
		UserRef ref = context.triggeringUser();
		if (ref == null || ref.username() == null) {
			return null;
		}
		return userRepository.findUserByUsername(ref.username());
	}

	private <T> T parentOfType(AnnotationAction action, Map<String, Object> createdByActionKey,
			Class<T> type) {
		if (action.parentActionKey() == null) {
			return null;
		}
		Object parent = createdByActionKey.get(action.parentActionKey());
		return type.isInstance(parent) ? type.cast(parent) : null;
	}

	private Position tryFindPosition(Object grouping, String text) {
		try {
			return annotationRepository.findPosition(grouping, text);
		} catch (NoSuchPositionException e) {
			return null;
		}
	}

	// ---- small utilities ------------------------------------------------------

	private static String findingType(AnnotationAction action) {
		String fromMeta = stringMeta(action, "findingType");
		return fromMeta != null ? fromMeta : action.actionType().name();
	}

	private static String stringMeta(AnnotationAction action, String key) {
		Object value = action.metadata().get(key);
		return value == null ? null : value.toString();
	}

	private static boolean booleanMeta(AnnotationAction action, String key, boolean defaultValue) {
		Object value = action.metadata().get(key);
		if (value instanceof Boolean b) {
			return b;
		}
		if (value != null) {
			return Boolean.parseBoolean(value.toString());
		}
		return defaultValue;
	}

	private static String truncate(String text, int max) {
		if (text == null) {
			return null;
		}
		return text.length() <= max ? text : text.substring(0, max);
	}

	private String evidenceJson(AnnotationAction action) {
		if (action.evidence() == null || action.evidence().isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(action.evidence());
		} catch (JsonProcessingException e) {
			log.warn("Could not serialize evidence for action {}", action.actionKey(), e);
			return null;
		}
	}

	private record AppliedAction(Long annotationId) {
	}
}
