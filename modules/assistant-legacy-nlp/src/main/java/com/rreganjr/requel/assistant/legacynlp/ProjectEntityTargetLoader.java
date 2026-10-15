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
package com.rreganjr.requel.assistant.legacynlp;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.AssistantTargetLoader;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;

/**
 * Resolves an assistant {@link EntityRef} to a persisted project entity by its
 * stable id, using {@link ProjectRepository#findById(Class, Long)}. The
 * dispatcher worker uses this to reload the analysis target in its own
 * transaction (so lazy collections stay attached during analysis), and the
 * result applicator uses it to resolve an action's target annotatable.
 *
 * <p>
 * The {@code entityType} string on the {@link EntityRef} is the domain
 * interface simple name (e.g. {@code "Goal"}, {@code "Story"}) — the same value
 * the legacy adapters stamp on the actions they emit.
 */
@Component
public class ProjectEntityTargetLoader implements AssistantTargetLoader {

	private static final Map<String, Class<?>> SUPPORTED_TYPES = Map.of(
			"Project", Project.class,
			"Goal", Goal.class,
			"Story", Story.class,
			"Actor", Actor.class,
			"UseCase", UseCase.class,
			"Scenario", Scenario.class,
			"Step", Step.class,
			"GlossaryTerm", GlossaryTerm.class);

	private final ProjectRepository projectRepository;

	@Autowired
	public ProjectEntityTargetLoader(ProjectRepository projectRepository) {
		this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
	}

	@Override
	public boolean supports(EntityRef targetRef) {
		return targetRef != null && targetRef.entityId() != null
				&& SUPPORTED_TYPES.containsKey(targetRef.entityType());
	}

	@Override
	public Optional<Object> loadTarget(EntityRef targetRef) {
		if (!supports(targetRef)) {
			return Optional.empty();
		}
		Class<?> entityType = SUPPORTED_TYPES.get(targetRef.entityType());
		try {
			return Optional.ofNullable(projectRepository.findById(entityType,
					targetRef.entityId()));
		} catch (NoSuchEntityException e) {
			return Optional.empty();
		}
	}
}
