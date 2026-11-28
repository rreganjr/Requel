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
package com.rreganjr.requel.service.command;

import static com.rreganjr.requel.service.api.dto.BulkIssueOutcomeDto.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.impl.IssueImpl;
import com.rreganjr.requel.annotation.impl.PositionTypes;
import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.service.api.dto.BulkIssueOutcomeDto;
import com.rreganjr.requel.service.api.dto.BulkResolveIssuesInput;
import com.rreganjr.requel.service.api.dto.BulkResolveIssuesResultDto;
import com.rreganjr.requel.service.api.dto.EditIssueInput;
import com.rreganjr.requel.service.api.dto.ResolveIssueInput;

import jakarta.persistence.EntityManager;

/**
 * #507: runs one action over many issues. Every id is checked before anything runs; then each
 * issue's own command (ResolveIssue or EditIssue) goes through the full handler chain in a
 * transaction of its own ({@code REQUIRES_NEW}), so one refusal or failure undoes nothing else.
 */
@Component
public class BulkIssueResolver {

	private static final Logger log = LoggerFactory.getLogger(BulkIssueResolver.class);

	/** Position types that resolve without changing the model: never a "suggested fix". */
	private static final Set<String> NOT_A_FIX = Set.of("Position", "IgnorePosition");

	private final EntityManager entityManager;
	private final AnnotatableTypeRegistry annotatableTypeRegistry;
	// Lazily: the command factory is built from the registrars, one of which registers this.
	private final ObjectProvider<ApiCommandFactory> apiCommandFactory;
	private final ObjectProvider<CommandHandler> commandHandler;
	private final TransactionTemplate perIssue;

	public BulkIssueResolver(EntityManager entityManager, AnnotatableTypeRegistry annotatableTypeRegistry,
			ObjectProvider<ApiCommandFactory> apiCommandFactory, ObjectProvider<CommandHandler> commandHandler,
			PlatformTransactionManager transactionManager) {
		this.entityManager = entityManager;
		this.annotatableTypeRegistry = annotatableTypeRegistry;
		this.apiCommandFactory = apiCommandFactory;
		this.commandHandler = commandHandler;
		this.perIssue = new TransactionTemplate(transactionManager);
		this.perIssue.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	/**
	 * Checks the input and every id against the project; any problem fails the whole request
	 * before an issue is touched.
	 */
	List<Long> checkedIds(Project project, BulkResolveIssuesInput input) {
		boolean setSeverity = BulkResolveIssuesInput.SET_SEVERITY.equals(input.action());
		if (setSeverity && (input.severity() == null || input.severity().isBlank())) {
			throw new IllegalArgumentException("SET_SEVERITY needs a severity: LOW, MEDIUM or HIGH.");
		}
		if (!setSeverity && input.severity() != null) {
			throw new IllegalArgumentException("severity is only for SET_SEVERITY.");
		}
		List<Long> ids = new ArrayList<>(new LinkedHashSet<>(input.issueIds()));
		for (Long id : ids) {
			IssueImpl issue = entityManager.find(IssueImpl.class, id);
			if (issue == null) {
				throw new NotFoundInputException("Issue not found: " + id);
			}
			AnnotationCommandRegistrar.requireSameScope(issue.getGroupingObject(), project, "Issue", id);
		}
		return ids;
	}

	BulkResolveIssuesResultDto resolve(Project project, BulkResolveIssuesInput input) {
		List<Long> ids = checkedIds(project, input);
		// The checks loaded the issues into this (outer) persistence context; each issue's command
		// runs in its own, so drop these copies rather than hold them stale.
		entityManager.clear();
		List<BulkIssueOutcomeDto> outcomes = new ArrayList<>();
		for (Long id : ids) {
			outcomes.add(resolveOne(project, id, input));
		}
		int done = count(outcomes, DONE);
		int skipped = count(outcomes, SKIPPED_NOT_APPLICABLE) + count(outcomes, SKIPPED_AMBIGUOUS);
		int refused = count(outcomes, REFUSED);
		int failed = count(outcomes, FAILED);
		return new BulkResolveIssuesResultDto(input.action(), done, skipped, refused, failed, outcomes);
	}

	private BulkIssueOutcomeDto resolveOne(Project project, Long issueId, BulkResolveIssuesInput input) {
		try {
			return perIssue.execute(status -> {
				try {
					return apply(project, issueId, input);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new CheckedFailure(e);
				}
			});
		} catch (CheckedFailure e) {
			return failure(issueId, e.getCause());
		} catch (RuntimeException e) {
			return failure(issueId, e);
		}
	}

	private BulkIssueOutcomeDto apply(Project project, Long issueId, BulkResolveIssuesInput input) throws Exception {
		IssueImpl issue = entityManager.find(IssueImpl.class, issueId);
		if (issue == null) {
			return new BulkIssueOutcomeDto(issueId, FAILED, "The issue was deleted.", null);
		}
		String action = input.action();
		if ((BulkResolveIssuesInput.IGNORE.equals(action) || BulkResolveIssuesInput.APPLY_FIX.equals(action))
				&& issue.getResolvedByPosition() != null) {
			return new BulkIssueOutcomeDto(issueId, SKIPPED_NOT_APPLICABLE, "Already resolved.", null);
		}
		return switch (action) {
			case BulkResolveIssuesInput.IGNORE -> resolveWith(project, issue,
					positions(issue, "IgnorePosition"::equals), "No Ignore position.");
			case BulkResolveIssuesInput.APPLY_FIX -> resolveWith(project, issue,
					positions(issue, type -> !NOT_A_FIX.contains(type)), "No suggested fix.");
			case BulkResolveIssuesInput.SET_SEVERITY -> editIssue(project, issue, null, input.severity());
			case BulkResolveIssuesInput.MARK_ADVISORY -> editIssue(project, issue, false, null);
			default -> throw new IllegalArgumentException("Unknown action: " + action);
		};
	}

	private BulkIssueOutcomeDto resolveWith(Project project, IssueImpl issue, List<Position> candidates,
			String noneReason) throws Exception {
		if (candidates.isEmpty()) {
			return new BulkIssueOutcomeDto(issue.getId(), SKIPPED_NOT_APPLICABLE, noneReason, null);
		}
		if (candidates.size() > 1) {
			return new BulkIssueOutcomeDto(issue.getId(), SKIPPED_AMBIGUOUS,
					candidates.size() + " suggested fixes; choose one on the issue.", null);
		}
		Long positionId = candidates.get(0).getId();
		run("ResolveIssue", new ResolveIssueInput(project.getName(), issue.getId(), positionId));
		return new BulkIssueOutcomeDto(issue.getId(), DONE, null, positionId);
	}

	private BulkIssueOutcomeDto editIssue(Project project, IssueImpl issue, Boolean mustBeResolved,
			String severity) throws Exception {
		// EditIssue names an entity the issue is on; the one with the lowest id, as the search does.
		Object on = null;
		Long onId = null;
		for (Annotatable annotatable : issue.getAnnotatables()) {
			Object real = Hibernate.unproxy(annotatable);
			Object id = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(real);
			if (id instanceof Long l && (onId == null || l < onId)) {
				on = real;
				onId = l;
			}
		}
		Optional<String> type = on == null ? Optional.empty()
				: annotatableTypeRegistry.resolveDiscriminator(on.getClass());
		if (type.isEmpty()) {
			return new BulkIssueOutcomeDto(issue.getId(), FAILED, "The issue is on nothing that can be edited.", null);
		}
		run("EditIssue", new EditIssueInput(project.getName(), type.get(), onId, issue.getId(),
				issue.getText(), mustBeResolved, severity));
		return new BulkIssueOutcomeDto(issue.getId(), DONE, null, null);
	}

	private void run(String commandType, Object input) throws Exception {
		Command command = apiCommandFactory.getObject().newCommand(commandType, input);
		commandHandler.getObject().execute(command);
	}

	private static List<Position> positions(IssueImpl issue, java.util.function.Predicate<String> type) {
		return issue.getPositions().stream()
				.filter(p -> type.test(PositionTypes.typeNameOf(p)))
				.sorted(Comparator.comparing(Position::getId, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();
	}

	private static BulkIssueOutcomeDto failure(Long issueId, Throwable e) {
		Throwable cause = e;
		while (cause != null && !(cause instanceof AuthorizationException) && cause.getCause() != null
				&& cause.getCause() != cause) {
			cause = cause.getCause();
		}
		if (cause instanceof AuthorizationException) {
			return new BulkIssueOutcomeDto(issueId, REFUSED, cause.getMessage(), null);
		}
		log.debug("Bulk action failed on issue {}", issueId, e);
		String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
		return new BulkIssueOutcomeDto(issueId, FAILED, reason, null);
	}

	private static int count(List<BulkIssueOutcomeDto> outcomes, String outcome) {
		return (int) outcomes.stream().filter(o -> outcome.equals(o.outcome())).count();
	}

	/** Carries a checked exception out of the transaction callback. */
	private static final class CheckedFailure extends RuntimeException {
		CheckedFailure(Exception cause) {
			super(cause);
		}
	}
}
