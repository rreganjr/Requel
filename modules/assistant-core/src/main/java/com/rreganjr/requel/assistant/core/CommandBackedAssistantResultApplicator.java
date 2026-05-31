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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Argument;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.NoSuchAnnotationException;
import com.rreganjr.requel.annotation.NoSuchPositionException;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditArgumentCommand;
import com.rreganjr.requel.annotation.command.EditChangeSpellingPositionCommand;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
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
	public AppliedAssistantResult apply(AssistantContext context, AssistantResult result) {
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(result, "result");

		User editedBy = resolveUser(context);
		String source = "ASSISTANT:" + result.assistantId();
		List<Long> annotationIds = new ArrayList<Long>();
		// Annotations created earlier in this same result, keyed by action key,
		// so a position can attach to an issue with no persisted id yet.
		Map<String, Object> createdByActionKey = new HashMap<String, Object>();
		int newFindings = 0;

		for (AnnotationAction action : result.annotationActions()) {
			try {
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
		return new AppliedAssistantResult(annotationIds.size(), annotationIds);
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
			case RESOLVE_ISSUE:
			case DELETE_NOTE:
			case DELETE_ISSUE:
			case DELETE_POSITION:
			case DELETE_ARGUMENT:
			case REMOVE_ANNOTATION_FROM_ANNOTATABLE:
			default:
				// Implemented with the finding state machine in Step 6.
				log.info("Skipping unsupported assistant action type {} (key {})",
						action.actionType(), action.actionKey());
				return null;
		}
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
		Note existing = tryFindNote(grouping, annotatable, text);
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

		Issue issue;
		if ("LEXICAL".equalsIgnoreCase(kind)) {
			EditLexicalIssueCommand command = annotationCommandFactory.newEditLexicalIssueCommand();
			String word = stringMeta(action, "word");
			String propertyName = stringMeta(action, "annotatableEntityPropertyName");
			boolean hasWord = word != null && !word.isBlank();
			// Word-oriented lexical issues (spelling, vague, glossary) dedupe by word;
			// word-less property-level lexical issues (e.g. complexity) dedupe by text.
			Issue existing = hasWord ? tryFindLexicalIssue(grouping, annotatable, word, propertyName)
					: tryFindIssue(grouping, annotatable, text);
			if (existing != null) {
				command.setIssue(existing);
			}
			if (hasWord) {
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
			Issue existing = tryFindIssue(grouping, annotatable, text);
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

	private Note tryFindNote(Object grouping, Annotatable annotatable, String text) {
		try {
			return annotationRepository.findNote(grouping, annotatable, text);
		} catch (NoSuchAnnotationException e) {
			return null;
		}
	}

	private Issue tryFindIssue(Object grouping, Annotatable annotatable, String text) {
		try {
			return annotationRepository.findIssue(grouping, annotatable, text);
		} catch (NoSuchAnnotationException e) {
			return null;
		}
	}

	private Issue tryFindLexicalIssue(Object grouping, Annotatable annotatable, String word,
			String propertyName) {
		if (word == null) {
			return null;
		}
		try {
			if (propertyName != null) {
				return annotationRepository.findLexicalIssue(grouping, annotatable, word,
						propertyName);
			}
			return annotationRepository.findLexicalIssue(grouping, annotatable, word);
		} catch (NoSuchAnnotationException e) {
			return null;
		}
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
