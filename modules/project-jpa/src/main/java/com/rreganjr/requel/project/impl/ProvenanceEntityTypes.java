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
package com.rreganjr.requel.project.impl;

import java.util.Map;
import java.util.Optional;

import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;

/**
 * Issue #272: the entity types that can carry a source link, by the name the link records —
 * {@code getProjectOrDomainEntityInterface().getSimpleName()}, which is also what the entity
 * delete path passes to {@code ProvenanceStore.deleteForTarget}. User stakeholders and report
 * generators are not ingested, so they have no entry.
 */
public final class ProvenanceEntityTypes {

	public static final Map<String, Class<? extends ProjectOrDomainEntity>> BY_NAME = Map.of(
			"Goal", Goal.class,
			"Story", Story.class,
			"Actor", Actor.class,
			"UseCase", UseCase.class,
			"Scenario", Scenario.class,
			"Step", Step.class,
			"GlossaryTerm", GlossaryTerm.class,
			"NonUserStakeholder", NonUserStakeholder.class);

	private ProvenanceEntityTypes() {
	}

	/**
	 * @return the type named {@code name}.
	 * @throws IllegalArgumentException for a type that cannot carry a source link
	 */
	public static Class<? extends ProjectOrDomainEntity> require(String name) {
		Class<? extends ProjectOrDomainEntity> type = name == null ? null : BY_NAME.get(name);
		if (type == null) {
			throw new IllegalArgumentException("entityType '" + name
					+ "' cannot carry a source; expected one of " + String.join(", ",
							new java.util.TreeSet<>(BY_NAME.keySet())));
		}
		return type;
	}

	/** The link's name for an entity: its entity interface's simple name. */
	public static String nameOf(ProjectOrDomainEntity entity) {
		return entity.getProjectOrDomainEntityInterface().getSimpleName();
	}

	/**
	 * The class the stakeholder permission is checked against: an edit to a step is a scenario
	 * edit and a non-user stakeholder is a stakeholder, as their own edit commands check.
	 */
	public static Class<?> permissionType(Class<?> entityType) {
		if (Step.class.equals(entityType)) {
			return Scenario.class;
		}
		if (NonUserStakeholder.class.equals(entityType)) {
			return Stakeholder.class;
		}
		return entityType;
	}

	/**
	 * Load an entity of {@code type} by id, only if it belongs to {@code project}.
	 */
	public static Optional<ProjectOrDomainEntity> load(ProjectRepository repository,
			Project project, Class<?> type, Long id) {
		if (type == null || id == null) {
			return Optional.empty();
		}
		try {
			Object entity = repository.findById(type, id);
			if (entity instanceof ProjectOrDomainEntity found && project != null
					&& found.getProjectOrDomain() != null
					&& project.getId().equals(found.getProjectOrDomain().getId())) {
				return Optional.of(found);
			}
			return Optional.empty();
		} catch (NoSuchEntityException e) {
			return Optional.empty();
		}
	}
}
