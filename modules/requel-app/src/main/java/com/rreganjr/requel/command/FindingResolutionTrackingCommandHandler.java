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
package com.rreganjr.requel.command;

import org.hibernate.Hibernate;

import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.impl.IgnorePosition;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.project.IgnoredFinding;
import com.rreganjr.requel.project.IgnoredFindingStore;
import com.rreganjr.requel.project.Project;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingRepository;
import com.rreganjr.requel.assistant.core.persistence.AssistantFindingState;

/**
 * A {@link CommandHandler} decorator that records the human side of the assistant
 * finding state machine: when a user resolves an assistant-raised issue, the
 * finding that produced that issue is moved {@code ACTIVE -> MANUALLY_RESOLVED}.
 *
 * <p>
 * This is the counterpart to the applicator's {@code AUTO_RESOLVE_IF_UNTOUCHED}
 * reconciliation (which closes findings a re-run no longer reports). A
 * {@link ResolveIssueCommand} is only ever issued by a human accepting a position
 * in the UI &mdash; the assistant auto-resolve path removes annotations through
 * {@code RemoveAnnotationFromAnnotatableCommand}, never through resolution &mdash;
 * so reacting here cannot collide with assistant-initiated cleanup. Once a finding
 * is {@code MANUALLY_RESOLVED} the applicator's "untouched" check (issue resolved)
 * already leaves it alone, so a later re-run will not auto-resolve or duplicate it.
 *
 * <p>
 * The bookkeeping is best-effort: a failure to update findings must never roll back
 * the user's resolution, so exceptions are logged and swallowed.
 */
public class FindingResolutionTrackingCommandHandler implements CommandHandler {

	private static final Logger log = LoggerFactory
			.getLogger(FindingResolutionTrackingCommandHandler.class);

	private final CommandHandler commandHandler;
	private final AssistantFindingRepository findingRepository;
	private IgnoredFindingStore ignoredFindingStore;
	private final Clock clock;

	public FindingResolutionTrackingCommandHandler(CommandHandler commandHandler,
			AssistantFindingRepository findingRepository) {
		this(commandHandler, findingRepository, Clock.systemUTC());
	}

	FindingResolutionTrackingCommandHandler(CommandHandler commandHandler,
			AssistantFindingRepository findingRepository, Clock clock) {
		this.commandHandler = commandHandler;
		this.findingRepository = findingRepository;
		this.clock = clock;
	}

	/** Issue #320: set from commandHandlerConfig.xml. */
	public void setIgnoredFindingStore(IgnoredFindingStore ignoredFindingStore) {
		this.ignoredFindingStore = ignoredFindingStore;
	}

	@Override
	public <T extends Command> T execute(T command) throws Exception {
		T executedCommand = commandHandler.execute(command);
		if (executedCommand instanceof ResolveIssueCommand resolve) {
			try {
				markFindingsManuallyResolved(resolve.getIssue());
			} catch (Exception e) {
				log.warn("Could not mark findings MANUALLY_RESOLVED for command {}: {}",
						command.getClass().getSimpleName(), e.getMessage(), e);
			}
			try {
				recordIgnoredFindings(resolve);
			} catch (Exception e) {
				log.warn("Could not record the ignored findings for command {}: {}",
						command.getClass().getSimpleName(), e.getMessage(), e);
			}
		}
		return executedCommand;
	}

	/**
	 * Issue #320: when the issue was resolved with an {@link IgnorePosition}, record an ignored
	 * finding for every assistant finding behind the issue, so the assistants skip it from now on
	 * even if the resolved issue is deleted. All findings on the issue, whatever their state: an
	 * issue shared by several entities (before the #320 scoping fix) was ignored on all of them.
	 */
	private void recordIgnoredFindings(ResolveIssueCommand resolve) {
		if (ignoredFindingStore == null) {
			return;
		}
		Position position = resolve.getResolvingPosition();
		Issue issue = resolve.getIssue();
		if (position == null || issue == null || issue.getId() == null
				|| !(Hibernate.unproxy(position) instanceof IgnorePosition)) {
			return;
		}
		Object unproxied = Hibernate.unproxy(issue);
		String propertyName = null;
		String word = null;
		if (unproxied instanceof LexicalIssue lexical) {
			propertyName = lexical.getAnnotatableEntityPropertyName();
			word = lexical.getWord();
		}
		User user = (resolve instanceof EditCommand edit) ? edit.getEditedBy() : null;
		for (AssistantFindingEntity finding : findingRepository
				.findByAppliedAnnotationId(issue.getId())) {
			String prefix = IgnoredFinding.keyPrefix(finding.getAssistantId(),
					finding.getTargetType(), finding.getTargetId());
			String key = finding.getIdempotencyKey();
			Long projectId = finding.getProjectId() != null ? finding.getProjectId()
					: projectIdOf(issue);
			if (key == null || !key.startsWith(prefix) || projectId == null) {
				log.warn("Not recording an ignore for finding {}: its key doesn't start with {}"
						+ " or it has no project", key, prefix);
				continue;
			}
			ignoredFindingStore.record(new IgnoredFindingStore.Spec(projectId,
					finding.getTargetType(), finding.getTargetId(), finding.getAssistantId(),
					finding.getFindingType(), propertyName, key.substring(prefix.length()),
					word != null ? word : finding.getSummary(), issue.getId()), user);
		}
	}

	private static Long projectIdOf(Issue issue) {
		Object grouping = issue.getGroupingObject();
		return (grouping instanceof Project project) ? project.getId() : null;
	}

	private void markFindingsManuallyResolved(Issue issue) {
		if (issue == null || issue.getId() == null) {
			return;
		}
		List<AssistantFindingEntity> findings = findingRepository
				.findByAppliedAnnotationId(issue.getId());
		Instant now = clock.instant();
		for (AssistantFindingEntity finding : findings) {
			if (AssistantFindingState.ACTIVE.name().equals(finding.getState())) {
				finding.setState(AssistantFindingState.MANUALLY_RESOLVED.name());
				finding.setClosedAt(now);
				findingRepository.save(finding);
			}
		}
	}
}
