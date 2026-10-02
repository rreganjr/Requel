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
import com.rreganjr.requel.annotation.IssueSeverity;
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
import com.rreganjr.requel.assistant.core.freshness.MachineAnnotations;
import com.rreganjr.requel.project.TargetFingerprint;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingState;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.command.AddGlossaryTermRefererCommand;
import com.rreganjr.requel.project.command.EditAddActorToProjectPositionCommand;
import com.rreganjr.requel.project.command.EditAddWordToGlossaryPositionCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.user.UserRepository;

/**
 * Command-backed applicator: turns each {@link AnnotationAction} into a call
 * through the existing command + {@link CommandHandler} chain so authorization,
 * validation, audit, optimistic locking, and SSE behave exactly as they do for
 * UI-driven edits. Commands are executed as the assistant user (issue #302,
 * {@link #resolveAssistantUser}), which is what {@code AuthorizingCommandHandler} checks via
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
 * (doc/work/2.0/43-phase-4.5-plan.md, Step 6); this applicator skips those action types
 * for now rather than failing the run.
 */
@Component
@Primary
public class CommandBackedAssistantResultApplicator implements AssistantResultApplicator {

	private static final Logger log = LoggerFactory
			.getLogger(CommandBackedAssistantResultApplicator.class);

	/**
	 * Issue #268: metadata naming an issue the old lexical path left, on a
	 * {@code REMOVE_ANNOTATION_FROM_ANNOTATABLE} action.
	 */
	public static final String LEGACY_ANNOTATION_ID = "legacyAnnotationId";

	/**
	 * Issue #270: the {@link AssistantContext#attributes()} entry carrying the
	 * {@link TargetFingerprint} of the dispatch target as the run analyzed it. Written by
	 * {@code AssistantRunWorker} in the analyze transaction.
	 */
	public static final String TARGET_FINGERPRINT = "targetFingerprint";

	private static final int MAX_TEXT_LENGTH = 4000;
	private static final int MAX_SUMMARY_LENGTH = 500;

	private final CommandHandler commandHandler;
	private final AnnotationCommandFactory annotationCommandFactory;
	private final ProjectCommandFactory projectCommandFactory;
	private final AnnotationRepository annotationRepository;
	private final UserRepository userRepository;
	/** Issue #260: per-assistant identities; null keeps the single run assistant user. */
	private com.rreganjr.requel.project.AssistantIdentities assistantIdentities;
	private final AssistantFindingRepository findingRepository;
	private final AssistantRunRepository runRepository;
	private final List<AssistantTargetLoader> targetLoaders;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private com.rreganjr.requel.project.IgnoredFindingStore ignoredFindingStore;
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

	/**
	 * Issue #260: setter-injected like the ignore store. Without it every result is written as the
	 * run's single assistant user, as before.
	 */
	@org.springframework.beans.factory.annotation.Autowired(required = false)
	public void setAssistantIdentities(com.rreganjr.requel.project.AssistantIdentities identities) {
		this.assistantIdentities = identities;
	}

	/**
	 * Issue #320: setter-injected so the constructors (and the tests that call them) don't change.
	 * Without a store nothing is treated as ignored.
	 */
	@org.springframework.beans.factory.annotation.Autowired(required = false)
	public void setIgnoredFindingStore(com.rreganjr.requel.project.IgnoredFindingStore store) {
		this.ignoredFindingStore = store;
	}

	/** The run's project's ignored keys, lower-cased; read once per apply. */
	private Set<String> ignoredKeys(AssistantContext context) {
		if (ignoredFindingStore == null || context.projectRef() == null) {
			return java.util.Collections.emptySet();
		}
		return ignoredFindingStore.ignoredKeys(context.projectRef().entityId());
	}

	/**
	 * Issue #268: {@code assistant|suffix} for every ignored key, where the suffix is the key after
	 * {@code assistant:type:id:}. An action with {@code scope=PROJECT} is ignored when any entity's
	 * finding with the same {@code shareKey} was: ignoring a glossary candidate once ignores it for
	 * the whole project, and the rows stay per entity, so export, import and the ignore list are
	 * unchanged.
	 */
	private static Set<String> ignoredShareKeys(Set<String> ignoredKeys) {
		Set<String> shareKeys = new HashSet<String>();
		for (String key : ignoredKeys) {
			String[] parts = key.split(":", 4);
			if (parts.length == 4) {
				shareKeys.add(parts[0] + "|" + parts[3]);
			}
		}
		return shareKeys;
	}

	private static boolean isIgnoredProjectWide(String assistantId, AnnotationAction action,
			Set<String> ignoredShareKeys) {
		String shareKey = projectShareKey(action);
		return shareKey != null
				&& action.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE
				&& ignoredShareKeys.contains(assistantId.toLowerCase(java.util.Locale.ROOT) + "|"
						+ shareKey.toLowerCase(java.util.Locale.ROOT));
	}

	/** The action's {@code shareKey} when it is {@code scope=PROJECT}, else null (#268). */
	private static String projectShareKey(AnnotationAction action) {
		if (!"PROJECT".equals(action.metadata().get("scope"))) {
			return null;
		}
		Object shareKey = action.metadata().get("shareKey");
		return shareKey instanceof String key && !key.isBlank() ? key : null;
	}

	private static boolean isIgnored(AnnotationAction action, Set<String> ignoredKeys) {
		if (ignoredKeys.isEmpty() || action.actionKey() == null) {
			return false;
		}
		AnnotationAction.ActionType type = action.actionType();
		return (type == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE
				|| type == AnnotationAction.ActionType.CREATE_OR_UPDATE_NOTE)
				&& ignoredKeys.contains(action.actionKey().toLowerCase(java.util.Locale.ROOT));
	}

	@Override
	public AppliedAssistantResult apply(AssistantContext context, AssistantResult result,
			CleanupPolicy cleanupPolicy, EntityRef dispatchTarget) {
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(result, "result");

		User editedBy = resolveAssistantUser(context, result.assistantId());
		String source = "ASSISTANT:" + result.assistantId();
		List<Long> annotationIds = new ArrayList<Long>();
		// Annotations created earlier in this same result, keyed by action key,
		// so a position can attach to an issue with no persisted id yet.
		Map<String, Object> createdByActionKey = new HashMap<String, Object>();
		// Finding keys this run produced, grouped by the entity they target, so
		// stale findings on each target can be reconciled afterward.
		Map<EntityRef, Set<String>> producedKeysByTarget = new HashMap<EntityRef, Set<String>>();
		int newFindings = 0;
		Set<String> ignoredKeys = ignoredKeys(context);
		Set<String> ignoredShareKeys = ignoredShareKeys(ignoredKeys);
		// Issue #268: an assistant that failed on part of the entity says so. Its findings still
		// apply, but nothing is removed or auto-resolved on the strength of an analysis that
		// didn't finish.
		boolean incomplete = isIncomplete(result);

		for (AnnotationAction action : result.annotationActions()) {
			try {
				if (incomplete && isLegacyRemoval(action)) {
					log.debug("Skipping {} from an incomplete result", action.actionKey());
					continue;
				}
				if (isIgnored(action, ignoredKeys)
						|| isIgnoredProjectWide(result.assistantId(), action, ignoredShareKeys)) {
					// Issue #320: the user ignored this finding on this entity and property. Not
					// applying it also skips its positions (their parent isn't in
					// createdByActionKey), and leaving the key out of producedKeysByTarget lets an
					// ACTIVE, untouched finding for it auto-resolve below.
					log.debug("Skipping ignored finding {}", action.actionKey());
					continue;
				}
				if (action.actionType() == AnnotationAction.ActionType.ADD_GLOSSARY_TERM_REFERER) {
					// Link the target entity to an existing glossary term. Idempotent at the
					// domain level, so no finding is recorded.
					applyGlossaryTermReferer(action, editedBy);
					continue;
				}
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
					if (upsertFinding(context, result, action, applied.annotationId(),
							createdByActionKey.get(action.actionKey()), dispatchTarget)) {
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
		if (incomplete) {
			log.info("Result of {} for run {} is incomplete (failed: {}); not reconciling stale"
					+ " findings", result.assistantId(), context.runId(),
					result.metadata().get("failedProperties"));
		} else {
			reconcileStaleFindings(result.assistantId(), cleanupPolicy, dispatchTarget,
					producedKeysByTarget, editedBy, context.runId());
			// #263: a definition reviewing this target also retires what other definitions of
			// the same task left on it (the generic review's findings once a per-type one runs).
			if (dispatchTarget != null) {
				for (String other : retiredAssistants(result)) {
					reconcileStaleFindings(other, cleanupPolicy, dispatchTarget,
							java.util.Map.of(), editedBy, context.runId());
				}
			}
		}
		return new AppliedAssistantResult(annotationIds.size(), annotationIds);
	}

	/** Issue #263: the other assistant ids whose findings on the target this result retires. */
	static List<String> retiredAssistants(AssistantResult result) {
		Object value = result.metadata().get(AssistantRunWorker.RETIRES_ASSISTANTS);
		List<String> ids = new ArrayList<>();
		if (value instanceof java.util.Collection<?> collection) {
			for (Object id : collection) {
				if (id != null && !id.toString().isBlank()
						&& !id.toString().equals(result.assistantId())) {
					ids.add(id.toString());
				}
			}
		}
		return ids;
	}

	/** Issue #268: the result's {@code metadata.incomplete} flag. */
	static boolean isIncomplete(AssistantResult result) {
		return Boolean.TRUE.equals(result.metadata().get("incomplete"));
	}

	private static boolean isLegacyRemoval(AnnotationAction action) {
		return action.actionType() == AnnotationAction.ActionType.REMOVE_ANNOTATION_FROM_ANNOTATABLE
				&& action.metadata().get(LEGACY_ANNOTATION_ID) != null;
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
				return applyIssue(context, result, action, editedBy, createdByActionKey);
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
		if (isLegacyRemoval(action)) {
			removeLegacyAnnotation(action, editedBy);
			return;
		}
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

	/**
	 * Issue #268: detach an issue the old lexical path left on the target, named by
	 * {@code metadata.legacyAnnotationId}. It has no finding, so nothing transitions. Checked
	 * again here, in the apply transaction: only an unresolved issue with no {@code ASSISTANT:}
	 * source is removed, so a user's resolution, or an assistant taking the issue over, between
	 * analysis and apply wins. Issue #270: nor is one carrying human discussion
	 * ({@link MachineAnnotations#hasHumanDiscussion}).
	 */
	private void removeLegacyAnnotation(AnnotationAction action, User editedBy) throws Exception {
		// Non-null: isLegacyRemoval checked it.
		Long annotationId = longMeta(action, LEGACY_ANNOTATION_ID);
		Annotation annotation = annotationRepository.findAnnotationById(annotationId);
		Annotatable annotatable = resolveAnnotatable(action.targetRef());
		if (annotation == null || annotatable == null || !(annotation instanceof Issue)
				|| annotation.isResolved() || MachineAnnotations.isAssistantSourced(annotation)) {
			log.debug("Skipping legacy removal {} — the annotation is gone, resolved or owned",
					action.actionKey());
			return;
		}
		if (MachineAnnotations.hasHumanDiscussion(annotation)) {
			// Issue #270: removing the issue from its only entity deletes it, positions and
			// arguments included. A human's discussion is never deleted by an assistant.
			log.info("Keeping the old lexical issue {} on {}: it carries human discussion",
					annotationId, action.targetRef());
			return;
		}
		RemoveAnnotationFromAnnotatableCommand command = annotationCommandFactory
				.newRemoveAnnotationFromAnnotatableCommand();
		command.setAnnotation(annotation);
		command.setAnnotatable(annotatable);
		command.setEditedBy(editedBy);
		commandHandler.execute(command);
		log.info("Removed the old lexical issue {} from {}", annotationId, action.targetRef());
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

	// ---- glossary-term referer ------------------------------------------------

	/**
	 * Add the action's {@code targetRef} entity as a referer to an existing glossary term
	 * (identified by {@code metadata.glossaryTermType} / {@code glossaryTermId}), through
	 * {@link AddGlossaryTermRefererCommand}. It produces no finding and is idempotent at the
	 * domain level (the referer collection is a {@code Set}). A no-op when either entity can no
	 * longer be resolved.
	 *
	 * <p>
	 * It goes through that narrow command rather than {@code EditGlossaryTermCommand} so the
	 * assistant does not need {@code GlossaryTerm[Edit]} to record a back-reference - see #302.
	 */
	private void applyGlossaryTermReferer(AnnotationAction action, User editedBy) throws Exception {
		Object refererObject = resolveTargetEntity(action.targetRef());
		Long termId = longMeta(action, "glossaryTermId");
		String termType = stringMeta(action, "glossaryTermType");
		Object termObject = termId == null ? null
				: resolveTargetEntity(EntityRef.of(termType, termId));
		if (!(refererObject instanceof ProjectOrDomainEntity referer)
				|| !(termObject instanceof GlossaryTerm glossaryTerm)) {
			log.info("Skipping glossary-term-referer action {} — referer {} or term {} did not"
					+ " resolve", action.actionKey(), action.targetRef(), termId);
			return;
		}
		AddGlossaryTermRefererCommand command = projectCommandFactory
				.newAddGlossaryTermRefererCommand();
		command.setGlossaryTerm(glossaryTerm);
		command.setReferer(referer);
		command.setEditedBy(editedBy);
		commandHandler.execute(command);
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

	private AppliedAction applyIssue(AssistantContext context, AssistantResult result,
			AnnotationAction action, User editedBy, Map<String, Object> createdByActionKey)
			throws Exception {
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
		IssueSeverity severity = issueSeverity(action);

		// Idempotency is keyed on the action key via the AssistantFinding's applied
		// annotation id, not on content. This lets the same word raise distinct issues
		// from different assistants/finding-types without one overwriting another, and
		// avoids hijacking human-authored annotations with matching text.
		Issue existing = loadExistingAnnotation(action.actionKey(), Issue.class);
		if (existing == null) {
			// Issue #268: a project-scoped finding joins the open issue another entity's finding
			// with the same shareKey already applied, instead of raising a second one.
			existing = findSharedIssue(context, result.assistantId(), projectShareKey(action));
		}

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
			command.setSeverity(severity);
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
			command.setSeverity(severity);
			command.setAnnotatable(annotatable);
			command.setEditedBy(editedBy);
			command = commandHandler.execute(command);
			issue = command.getIssue();
		}
		createdByActionKey.put(action.actionKey(), issue);
		return new AppliedAction(issue.getId());
	}

	/**
	 * #271: the issue severity an action carries. Absent (the legacy NLP assistants send none) is
	 * null: the issue kind's default on create, unchanged on update. The applicator is lenient: an
	 * unrecognised value from a model is logged and treated as absent, so one bad reply doesn't
	 * drop a real finding. The finding row still records the raw value.
	 */
	private static IssueSeverity issueSeverity(AnnotationAction action) {
		String raw = action.severity();
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return IssueSeverity.parse(raw).orElseGet(() -> {
			log.warn("assistant action {} sent unknown severity '{}'; using the default",
					action.actionKey(), raw);
			return null;
		});
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

		if ("IGNORE".equalsIgnoreCase(kind)) {
			// Issue #320: the ignore marker. The command reuses the grouping's ignore position with
			// this text itself, so the plain-position lookup below is skipped (it can find a
			// plain position with the same text, which isn't an IgnorePosition).
			EditPositionCommand ignore = annotationCommandFactory.newEditIgnorePositionCommand();
			ignore.setIssue(issue);
			ignore.setText(text);
			ignore.setEditedBy(editedBy);
			ignore = commandHandler.execute(ignore);
			Position position = ignore.getPosition();
			createdByActionKey.put(action.actionKey(), position);
			return new AppliedAction(position.getId());
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
	/**
	 * Issue #268: the open issue behind an {@code ACTIVE} finding of this assistant, in this
	 * project, whose key suffix (after {@code assistant:type:id:}) is {@code shareKey}; the lowest
	 * issue id when there are several. Null when there is no share key or no such issue.
	 */
	private Issue findSharedIssue(AssistantContext context, String assistantId, String shareKey) {
		if (shareKey == null || context.projectRef() == null) {
			return null;
		}
		Issue shared = null;
		for (AssistantFindingEntity finding : findingRepository.findByAssistantIdAndProjectIdAndState(
				assistantId, context.projectRef().entityId(), AssistantFindingState.ACTIVE.name())) {
			String[] parts = finding.getIdempotencyKey().split(":", 4);
			if (parts.length != 4 || !parts[3].equalsIgnoreCase(shareKey)
					|| finding.getAppliedAnnotationId() == null) {
				continue;
			}
			Issue issue;
			try {
				issue = annotationRepository.findById(Issue.class, finding.getAppliedAnnotationId());
			} catch (RuntimeException e) {
				continue;
			}
			if (issue != null && !issue.isResolved()
					&& (shared == null || issue.getId() < shared.getId())) {
				shared = issue;
			}
		}
		return shared;
	}

	private <T> T loadExistingAnnotation(String actionKey, Class<T> annotationType) {
		return findingRepository.findByIdempotencyKey(actionKey)
				.map(AssistantFindingEntity::getAppliedAnnotationId)
				.map(annotationId -> annotationRepository.findById(annotationType, annotationId))
				.orElse(null);
	}

	// ---- finding upsert -------------------------------------------------------

	/**
	 * Record that this run reported the finding.
	 * <p>
	 * Issue #320: a finding this run reports again, whose annotation is open, is {@code ACTIVE}
	 * whatever state it was left in. Two cases re-raise onto a closed finding:
	 * <ul>
	 * <li>the resolved issue was deleted, so a {@code MANUALLY_RESOLVED} finding is repointed at
	 * a new open issue;</li>
	 * <li>the word left the text and came back, so an {@code AUTO_RESOLVED} (or
	 * {@code SUPERSEDED}) finding gets its annotation back.</li>
	 * </ul>
	 * Leaving the old state meant {@link #reconcileStaleFindings}, which only considers
	 * {@code ACTIVE} findings, never cleaned the open issue up again.
	 *
	 * <p>
	 * Issue #270: both paths record the fingerprint of the text the finding was derived from, so a
	 * later edit makes it read stale until a run reports it again.
	 *
	 * @param annotation
	 *            the annotation this run applied for the action, or null
	 * @param dispatchTarget
	 *            the entity the run analyzed, whose fingerprint the worker took
	 * @return true if a new finding row was created (vs touching an existing one).
	 */
	private boolean upsertFinding(AssistantContext context, AssistantResult result,
			AnnotationAction action, Long annotationId, Object annotation,
			EntityRef dispatchTarget) {
		Instant now = clock.instant();
		String fingerprint = targetFingerprint(context, action.targetRef(), dispatchTarget);
		Optional<AssistantFindingEntity> existing = findingRepository
				.findByIdempotencyKey(action.actionKey());
		if (existing.isPresent()) {
			AssistantFindingEntity finding = existing.get();
			finding.setLastSeenRunId(context.runId().toString());
			finding.setLastSeenAt(now);
			if (annotationId != null) {
				finding.setAppliedAnnotationId(annotationId);
			}
			if (fingerprint != null) {
				finding.setTargetFingerprint(fingerprint);
			}
			if (!AssistantFindingState.ACTIVE.name().equals(finding.getState())
					&& isOpen(annotation)) {
				finding.setState(AssistantFindingState.ACTIVE.name());
				finding.setClosedAt(null);
				finding.setSupersededByRunId(null);
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
		finding.setTargetFingerprint(fingerprint);
		findingRepository.save(finding);
		return true;
	}

	/**
	 * Issue #270: the fingerprint of the finding's target as the run analyzed it. For the dispatch
	 * target that is the one the worker took in the analyze transaction; an entity changed between
	 * analysis and apply must leave the finding stale. A finding on another entity, or a result
	 * applied without the worker (tests, callers with their own context), falls back to the entity
	 * as it is now.
	 */
	private String targetFingerprint(AssistantContext context, EntityRef targetRef,
			EntityRef dispatchTarget) {
		Object analyzed = context.attributes().get(TARGET_FINGERPRINT);
		if (analyzed instanceof String value && targetRef != null
				&& targetRef.equals(dispatchTarget)) {
			return value;
		}
		Object entity = resolveTargetEntity(targetRef);
		return entity == null ? null : TargetFingerprint.of(entity);
	}

	/** An unresolved issue, or a note: an annotation a human still has to act on or read. */
	private static boolean isOpen(Object annotation) {
		if (annotation instanceof Issue issue) {
			return !issue.isResolved();
		}
		return annotation instanceof Note;
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
	 * and untouched by a human (see {@link #autoResolveIfUntouched}); otherwise mark it
	 * {@code SUPERSEDED} and keep the annotation (issue #270).</li>
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
					autoResolveIfUntouched(finding, target, editedBy, runId);
				} else {
					markSuperseded(finding, runId);
				}
			}
		}
	}

	/**
	 * Mark a stale finding {@code SUPERSEDED}: record the run that superseded it and
	 * close it, leaving its annotation untouched. Used by the default
	 * {@link CleanupPolicy#MARK_SUPERSEDED} when a re-run no longer reports the finding, and
	 * (issue #270) by {@link CleanupPolicy#AUTO_RESOLVE_IF_UNTOUCHED} when human discussion keeps
	 * the annotation. A SUPERSEDED finding's annotation reads stale.
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
	 * its target and mark the finding {@code AUTO_RESOLVED}. If a human resolved or
	 * discussed the annotation it is kept and the finding marked {@code SUPERSEDED}
	 * (issue #270); if the annotation is already gone the finding is closed.
	 */
	private void autoResolveIfUntouched(AssistantFindingEntity finding, EntityRef target,
			User editedBy, UUID runId) {
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
			// A human resolved it or discussed it: keep the annotation. Issue #270: the run no
			// longer reports it, so it is SUPERSEDED (reads "may no longer apply") rather than
			// left ACTIVE with a fingerprint that still matches.
			markSuperseded(finding, runId);
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
	 * An annotation is "untouched" — safe to auto-remove — when it is still assistant-owned (an
	 * {@code ASSISTANT:} source) and carries no human discussion: it is unresolved and every
	 * position and argument on it was written by the assistant user (issue #270,
	 * {@link MachineAnnotations#hasHumanDiscussion}). Since #302 assistant runs write as the
	 * assistant user, so the positions an assistant suggests do not block the cleanup, and a
	 * position or argument a person added does. Removing the annotation from its only entity
	 * deletes it, positions and arguments included, so this check is what keeps human discussion.
	 */
	private static boolean isUntouched(Annotation annotation) {
		return MachineAnnotations.isAssistantSourced(annotation)
				&& !MachineAnnotations.hasHumanDiscussion(annotation);
	}

	// ---- resolution helpers ---------------------------------------------------

	/** Resolve an entity by ref through the target loaders, or {@code null}. */
	private Object resolveTargetEntity(EntityRef ref) {
		if (ref == null) {
			return null;
		}
		for (AssistantTargetLoader loader : targetLoaders) {
			if (loader.supports(ref)) {
				Optional<Object> target = loader.loadTarget(ref);
				if (target.isPresent()) {
					return target.get();
				}
			}
		}
		return null;
	}

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

	/**
	 * The identity an assistant run's writes are made with: the assistant, not the person whose
	 * edit set the run going (issue #302).
	 *
	 * <p>
	 * These annotations are the assistant's findings, so the assistant authors them and the
	 * assistant's permissions authorize them. Writing them as the triggering user made every
	 * finding look hand-written by whoever happened to save the entity, and quietly borrowed
	 * that person's permissions - which is why #302 never showed up on this path even though
	 * the assistant held none on a UI-created project. MCP and agent writes are a different
	 * thing and keep the logged-in user: those go through the command gateway, which stamps
	 * editedBy from the SecurityContext.
	 *
	 * <p>
	 * Falls back to the triggering user when the run carries no resolvable assistant identity,
	 * so an unexpected context cannot take down a whole analysis pass.
	 */
	/**
	 * Issue #260: the assistant's own identity when {@link #setAssistantIdentities} is wired,
	 * otherwise (or if it cannot be resolved) the run's assistant user.
	 */
	private User resolveAssistantUser(AssistantContext context, String assistantId) {
		if (assistantIdentities != null && assistantId != null) {
			try {
				Long projectId = context.projectRef() != null
						&& "Project".equals(context.projectRef().entityType())
								? context.projectRef().entityId()
								: null;
				return assistantIdentities.identityFor(assistantId, projectId);
			} catch (RuntimeException e) {
				log.warn("No identity for assistant {} in run {}; writing as the run's assistant"
						+ " user", assistantId, context.runId(), e);
			}
		}
		return resolveAssistantUser(context);
	}

	private User resolveAssistantUser(AssistantContext context) {
		// AssistantContext requires a non-null assistantUser, so only the username can be absent.
		UserRef ref = context.assistantUser();
		if (ref.username() != null) {
			try {
				return userRepository.findUserByUsername(ref.username());
			} catch (RuntimeException e) {
				log.warn("Assistant user '{}' did not resolve; falling back to the triggering"
						+ " user for run {}", ref.username(), context.runId(), e);
			}
		}
		return resolveUser(context);
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

	private static Long longMeta(AnnotationAction action, String key) {
		Object value = action.metadata().get(key);
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value != null) {
			return Long.valueOf(value.toString());
		}
		return null;
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
