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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.command.AnalysisRequestDispatcher;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Manual trigger for the AI requirements review (issue #43, Phase 5). Dispatches a
 * {@code REQUIREMENTS_REVIEW} analysis run for one project entity on behalf of the current
 * (triggering) user — the "run manually first" path. The run executes the
 * {@code RequirementsReviewAssistant} (when {@code requel.ai.enabled}); ordinary post-edit
 * saves are unaffected.
 *
 * <p>
 * Authorization mirrors the read API: the triggering user must be a system admin or a
 * stakeholder on the target's project, else {@link AuthorizationException}.
 */
@Service
public class AiReviewService {

	public static final String TASK_TYPE = "REQUIREMENTS_REVIEW";

	/** Reviewable target types (the {@link com.rreganjr.requel.project.TextEntity}s). */
	private static final Map<String, Class<? extends ProjectOrDomainEntity>> REVIEWABLE_TYPES = Map.of(
			"Goal", Goal.class,
			"Story", Story.class,
			"Actor", Actor.class,
			"UseCase", UseCase.class,
			"Scenario", Scenario.class,
			"Step", Step.class);

	private final ProjectRepository projectRepository;
	private final CurrentUserResolver currentUserResolver;
	private final AnalysisRequestDispatcher analysisRequestDispatcher;

	@Autowired
	public AiReviewService(ProjectRepository projectRepository,
			CurrentUserResolver currentUserResolver,
			AnalysisRequestDispatcher analysisRequestDispatcher) {
		this.projectRepository = projectRepository;
		this.currentUserResolver = currentUserResolver;
		this.analysisRequestDispatcher = analysisRequestDispatcher;
	}

	/**
	 * Dispatch a {@code REQUIREMENTS_REVIEW} run for the given entity as the current user.
	 *
	 * @throws IllegalArgumentException if {@code entityType} is not reviewable
	 * @throws com.rreganjr.platform.exception.NoSuchEntityException if the entity does not exist
	 * @throws AuthorizationException if the current user cannot access the entity's project
	 * @throws IllegalStateException if there is no authenticated user
	 */
	public void requestReview(String entityType, Long entityId) {
		Class<? extends ProjectOrDomainEntity> type = REVIEWABLE_TYPES.get(entityType);
		if (type == null) {
			throw new IllegalArgumentException("Entity type is not reviewable: " + entityType);
		}
		User user = currentUserResolver.resolve();
		ProjectOrDomainEntity target = projectRepository.findById(type, entityId);
		requireProjectAccess(target, user);
		analysisRequestDispatcher.dispatch(target, user, TASK_TYPE);
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
