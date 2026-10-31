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
package com.rreganjr.requel.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.core.SimpleAssistantRegistry;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService;
import com.rreganjr.requel.command.AnalysisRequestDispatcher;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.service.api.dto.AiReviewAvailabilityDto;
import com.rreganjr.requel.service.api.dto.AiReviewRequestDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Manual trigger for the AI requirements review (issue #43, Phase 5). Dispatches a
 * {@code REQUIREMENTS_REVIEW} analysis run for one project entity on behalf of the current
 * (triggering) user — the "run manually first" path. The run executes the matching assistant
 * definitions (#260; by default the bundled {@code ai-requirements-review}) when
 * {@code requel.ai.enabled}; ordinary post-edit saves are unaffected.
 *
 * <p>
 * Authorization mirrors the read API: the triggering user must be a system admin or a
 * stakeholder on the target's project, else {@link AuthorizationException}.
 * <p>
 * Issue #387: a request that cannot run is refused ({@link AiReviewUnavailableException}) rather
 * than queuing a run that does nothing, a request while the entity's review is still queued or
 * running returns that run rather than paying for another, and the ids of the runs are returned
 * so the caller can follow them. The {@code projectName} overloads are the gateway's: the entity
 * must be in that project.
 */
@Service
public class AiReviewService {

	public static final String TASK_TYPE = "REQUIREMENTS_REVIEW";
	/** #265: the composed policy pass. */
	public static final String POLICY_TASK_TYPE = AssistantDefinition.POLICY_REVIEW;

	/** Reviewable target types (the {@link com.rreganjr.requel.project.TextEntity}s). */
	private static final Map<String, Class<? extends ProjectOrDomainEntity>> REVIEWABLE_TYPES = Map.of(
			"Goal", Goal.class,
			"Story", Story.class,
			"Actor", Actor.class,
			"UseCase", UseCase.class,
			"Scenario", Scenario.class,
			"Step", Step.class,
			// #263: reviewable, with its own definition and the glossary-related provider
			"GlossaryTerm", GlossaryTerm.class);

	/** #387: how an unavailable reason names a type's entities. */
	private static final Map<String, String> TYPE_PLURALS = Map.of("Goal", "goals", "Story",
			"stories", "Actor", "actors", "UseCase", "use cases", "Scenario", "scenarios", "Step",
			"steps", "GlossaryTerm", "glossary terms");

	/** #387: run statuses that mean a review is still in flight. */
	private static final List<String> IN_FLIGHT = List.of("QUEUED", "RUNNING");

	static final String AI_OFF = "AI review is off on this server.";
	static final String NOT_ALLOWLISTED = "This project is not on the server's AI review allowlist.";

	private final ProjectRepository projectRepository;
	private final CurrentUserResolver currentUserResolver;
	private final AnalysisRequestDispatcher analysisRequestDispatcher;
	private final AssistantRunReadService runReadService;
	private SimpleAssistantRegistry registry;
	private AiProperties aiProperties;

	/** #387: whether AI is on and which projects may use it; without it, AI is off. */
	@Autowired(required = false)
	public void setAiProperties(AiProperties aiProperties) {
		this.aiProperties = aiProperties;
	}

	/** #265: decides whether a policy applies; without it, no policy pass is dispatched. */
	@Autowired(required = false)
	public void setRegistry(SimpleAssistantRegistry registry) {
		this.registry = registry;
	}

	@Autowired
	public AiReviewService(ProjectRepository projectRepository,
			CurrentUserResolver currentUserResolver,
			AnalysisRequestDispatcher analysisRequestDispatcher,
			AssistantRunReadService runReadService) {
		this.projectRepository = projectRepository;
		this.currentUserResolver = currentUserResolver;
		this.analysisRequestDispatcher = analysisRequestDispatcher;
		this.runReadService = runReadService;
	}

	/**
	 * Dispatch a {@code REQUIREMENTS_REVIEW} run for the given entity as the current user, and the
	 * policy pass when a policy applies (#265). A run already queued or running for the entity is
	 * returned instead of dispatching another (#387).
	 *
	 * @return the runs to follow
	 * @throws IllegalArgumentException if {@code entityType} is not reviewable
	 * @throws com.rreganjr.platform.exception.NoSuchEntityException if the entity does not exist
	 * @throws AuthorizationException if the current user cannot access the entity's project
	 * @throws AiReviewUnavailableException if a review cannot run for the entity (#387)
	 * @throws IllegalStateException if there is no authenticated user
	 */
	public AiReviewRequestDto requestReview(String entityType, Long entityId) {
		return requestReview(null, entityType, entityId);
	}

	/**
	 * Issue #387: {@link #requestReview(String, Long)} for an entity that must be in
	 * {@code projectName} (the gateway's form); null skips the project check.
	 *
	 * @throws com.rreganjr.requel.project.exception.NoSuchProjectException for an unknown project
	 * @throws IllegalArgumentException also when the entity is in another project
	 */
	public AiReviewRequestDto requestReview(String projectName, String entityType, Long entityId) {
		User user = currentUserResolver.resolve();
		ProjectOrDomainEntity target = accessibleTarget(projectName, entityType, entityId, user);
		String reason = unavailableReason(target);
		if (reason != null) {
			throw new AiReviewUnavailableException(reason);
		}
		String runId = inFlight(target, TASK_TYPE)
				.orElseGet(() -> analysisRequestDispatcher.dispatch(target, user, TASK_TYPE));
		String policyRunId = null;
		// #265: the policy pass, only when a policy applies (no empty runs).
		if (registry != null && !registry.policiesFor(target, projectId(target)).isEmpty()) {
			policyRunId = inFlight(target, POLICY_TASK_TYPE).orElseGet(
					() -> analysisRequestDispatcher.dispatch(target, user, POLICY_TASK_TYPE));
		}
		return new AiReviewRequestDto(runId, policyRunId);
	}

	/**
	 * Issue #387: whether a review of the entity would run, and if not why. Same type, entity and
	 * access checks as {@link #requestReview(String, Long)}.
	 */
	public AiReviewAvailabilityDto availability(String entityType, Long entityId) {
		User user = currentUserResolver.resolve();
		String reason = unavailableReason(accessibleTarget(null, entityType, entityId, user));
		return new AiReviewAvailabilityDto(reason == null, reason);
	}

	/**
	 * Null when a review of {@code target} would run; otherwise why not: AI off on the server,
	 * the project not on {@code requel.ai.projectAllowlist}, or no review definition left to run
	 * for its type after the project's switches.
	 */
	private String unavailableReason(ProjectOrDomainEntity target) {
		if (aiProperties == null || !aiProperties.isEnabled()) {
			return AI_OFF;
		}
		Long projectId = projectId(target);
		List<String> allowlist = aiProperties.getProjectAllowlist();
		if (allowlist != null && !allowlist.isEmpty()
				&& (projectId == null || !allowlist.contains(String.valueOf(projectId)))) {
			return NOT_ALLOWLISTED;
		}
		if (registry == null || registry.reviewsFor(target, projectId).isEmpty()) {
			String type = target.getProjectOrDomainEntityInterface().getSimpleName();
			return "AI review of " + TYPE_PLURALS.getOrDefault(type, type)
					+ " is switched off for this project.";
		}
		return null;
	}

	/** The id of the entity's latest run of {@code taskType} when it is still queued or running. */
	private Optional<String> inFlight(ProjectOrDomainEntity target, String taskType) {
		return runReadService.latestRun(target.getProjectOrDomainEntityInterface().getSimpleName(),
				target.getId(), taskType)
				.filter(run -> IN_FLIGHT.contains(run.status()))
				.map(AssistantRunReadService.RunView::runId);
	}

	/**
	 * The reviewable entity, after the type, existence, project (when named) and access checks.
	 */
	private ProjectOrDomainEntity accessibleTarget(String projectName, String entityType,
			Long entityId, User user) {
		Class<? extends ProjectOrDomainEntity> type = REVIEWABLE_TYPES.get(entityType);
		if (type == null) {
			throw new IllegalArgumentException("Entity type is not reviewable: " + entityType);
		}
		Project project = projectName == null ? null : projectRepository.findProjectByName(projectName);
		ProjectOrDomainEntity target = projectRepository.findById(type, entityId);
		if (project != null && !inProject(target, project)) {
			throw new IllegalArgumentException(entityType + " " + entityId
					+ " belongs to another project.");
		}
		requireProjectAccess(target, user);
		return target;
	}

	/** By id: the entity's project may be a Hibernate proxy. */
	private static boolean inProject(ProjectOrDomainEntity target, Project project) {
		ProjectOrDomain owner = target.getProjectOrDomain();
		return owner instanceof Project && owner.getId() != null
				&& owner.getId().equals(project.getId());
	}

	private static Long projectId(ProjectOrDomainEntity target) {
		return target.getProjectOrDomain() == null ? null : target.getProjectOrDomain().getId();
	}

	/**
	 * Issue #355: the entity's latest {@code REQUIREMENTS_REVIEW} run with the findings it reported,
	 * or empty when it has never been reviewed. Same type and access checks as
	 * {@link #requestReview}.
	 *
	 * @throws IllegalArgumentException if {@code entityType} is not reviewable
	 * @throws com.rreganjr.platform.exception.NoSuchEntityException if the entity does not exist
	 * @throws AuthorizationException if the current user cannot access the entity's project
	 */
	public Optional<AssistantRunReadService.RunView> latestReview(String entityType, Long entityId) {
		return latestReview(entityType, entityId, TASK_TYPE);
	}

	/**
	 * The entity's latest run of {@code taskType}: {@code REQUIREMENTS_REVIEW} or
	 * {@code POLICY_REVIEW} (#265).
	 */
	public Optional<AssistantRunReadService.RunView> latestReview(String entityType, Long entityId,
			String taskType) {
		return latestReview(null, entityType, entityId, taskType);
	}

	/**
	 * Issue #387: {@link #latestReview(String, Long, String)} for an entity that must be in
	 * {@code projectName} (the gateway's form); null skips the project check. A null task type is
	 * the requirements review.
	 */
	public Optional<AssistantRunReadService.RunView> latestReview(String projectName,
			String entityType, Long entityId, String taskType) {
		String task = taskType == null || taskType.isBlank() ? TASK_TYPE : taskType;
		if (!TASK_TYPE.equals(task) && !POLICY_TASK_TYPE.equals(task)) {
			throw new IllegalArgumentException("Unknown review task type: " + taskType + " (known: "
					+ TASK_TYPE + ", " + POLICY_TASK_TYPE + ")");
		}
		User user = currentUserResolver.resolve();
		ProjectOrDomainEntity target = accessibleTarget(projectName, entityType, entityId, user);
		// Runs are keyed by the entity's interface name, as the dispatcher writes them.
		return runReadService.latestRun(target.getProjectOrDomainEntityInterface().getSimpleName(),
				target.getId(), task);
	}

	private void requireProjectAccess(ProjectOrDomainEntity target, User user) {
		if (user.hasRole(SystemAdminUserRole.class)) {
			return;
		}
		ProjectOrDomain projectOrDomain = target.getProjectOrDomain();
		if (projectOrDomain != null) {
			for (Stakeholder stakeholder : projectOrDomain.getStakeholders()) {
				if (stakeholder.matchesUser(user) && stakeholder instanceof UserStakeholder) {
					return;
				}
			}
		}
		throw new AuthorizationException("You do not have access to this project.");
	}
}
