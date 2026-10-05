/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl.repository.init;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.bootstrap.AbstractSystemInitializer;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.AssistantDefinition;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.StakeholderPermissionRules;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;

/**
 * @author ron
 */
@Component("stakeholderPermissionsInitializer")
@Scope("prototype")
public class StakeholderPermissionsInitializer extends AbstractSystemInitializer {

	private final ProjectRepository projectRepository;

	/**
	 * @param projectRepository
	 */
	@Autowired
	public StakeholderPermissionsInitializer(ProjectRepository projectRepository) {
		super(10);
		this.projectRepository = projectRepository;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void initialize() {
		log.debug("update stakeholder permissions...");
		for (StakeholderPermission permission : getPermissionTypes()) {
			try {
				permission = projectRepository.findStakeholderPermission(
						permission.getEntityType(), permission.getPermissionType());
				log.debug(permission + " is already persistent.");
			} catch (EntityException e) {
				log.debug("creating: " + permission);
				permission = projectRepository.persist(permission);
			}
		}

		backfillProjectDeletePermission();
		backfillAssistantDefinitionPermission();
		closeImpliedPermissions();
	}

	/**
	 * Issue #75: grant every existing {@link UserStakeholder} what its permissions imply
	 * ({@link StakeholderPermissionRules#IMPLIED}), as {@code EditUserStakeholder} now does on
	 * every save - a UseCase[Edit] holder gets Scenario[Edit] and Actor[Edit], without which it
	 * can't create a use case. Idempotent.
	 */
	private void closeImpliedPermissions() {
		java.util.Map<String, StakeholderPermission> byKey = new java.util.HashMap<>();
		for (StakeholderPermission permission : projectRepository
				.findAvailableStakeholderPermissions()) {
			byKey.put(permission.getPermissionKey(), permission);
		}
		for (StakeholderPermissionRules.Implied rule : StakeholderPermissionRules.IMPLIED) {
			StakeholderPermission granted = byKey.get(rule.granted());
			StakeholderPermission implied = byKey.get(rule.implied());
			if (granted == null || implied == null) {
				continue;
			}
			for (UserStakeholder stakeholder : projectRepository
					.findUserStakeholdersWithPermission(granted)) {
				if (!stakeholder.getStakeholderPermissions().contains(implied)) {
					log.debug("granting implied " + rule.implied() + " to " + stakeholder);
					stakeholder.grantStakeholderPermission(implied);
					projectRepository.merge(stakeholder);
				}
			}
		}
	}

	/**
	 * Grant {@code AssistantDefinition[Edit]} to every existing {@link UserStakeholder} that holds
	 * {@code Project[Edit]} (issue #264), as {@link #backfillProjectDeletePermission} does for
	 * {@code Project[Delete]}: project owners keep managing their project's assistants after the
	 * upgrade. New project creators get every available permission without this. Idempotent.
	 */
	private void backfillAssistantDefinitionPermission() {
		StakeholderPermission projectEdit = projectRepository.findStakeholderPermission(
				Project.class, StakeholderPermissionType.Edit);
		StakeholderPermission definitionEdit = projectRepository.findStakeholderPermission(
				AssistantDefinition.class, StakeholderPermissionType.Edit);
		for (UserStakeholder stakeholder : projectRepository
				.findUserStakeholdersWithPermission(projectEdit)) {
			if (!stakeholder.getStakeholderPermissions().contains(definitionEdit)) {
				log.debug("backfilling AssistantDefinition[Edit] to " + stakeholder);
				stakeholder.grantStakeholderPermission(definitionEdit);
				projectRepository.merge(stakeholder);
			}
		}
	}

	/**
	 * Grant {@code Project[Delete]} to every existing {@link UserStakeholder} that
	 * already holds {@code Project[Edit]} (issue #240). New project creators and
	 * {@code SystemAdmin} are covered without this; the backfill lets owners of
	 * projects created before the permission existed delete their own projects.
	 * Idempotent - holders that already have it are skipped.
	 */
	private void backfillProjectDeletePermission() {
		StakeholderPermission projectEdit = projectRepository.findStakeholderPermission(
				Project.class, StakeholderPermissionType.Edit);
		StakeholderPermission projectDelete = projectRepository.findStakeholderPermission(
				Project.class, StakeholderPermissionType.Delete);
		for (UserStakeholder stakeholder : projectRepository
				.findUserStakeholdersWithPermission(projectEdit)) {
			if (!stakeholder.getStakeholderPermissions().contains(projectDelete)) {
				log.debug("backfilling Project[Delete] to " + stakeholder);
				stakeholder.grantStakeholderPermission(projectDelete);
				projectRepository.merge(stakeholder);
			}
		}
	}

	private Collection<StakeholderPermission> getPermissionTypes() {
		Collection<StakeholderPermission> entityTypes = new ArrayList<StakeholderPermission>();
		entityTypes
				.add(new StakeholderPermissionImpl(Project.class, StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(Project.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(Project.class,
				StakeholderPermissionType.Delete));

		// #264: authoring the project's assistant definitions; Edit only (read and write)
		entityTypes.add(new StakeholderPermissionImpl(AssistantDefinition.class,
				StakeholderPermissionType.Edit));

		entityTypes.add(new StakeholderPermissionImpl(Annotation.class,
				StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(Annotation.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(Annotation.class,
				StakeholderPermissionType.Delete));
		entityTypes.add(new StakeholderPermissionImpl(Goal.class, StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(Goal.class, StakeholderPermissionType.Grant));
		entityTypes
				.add(new StakeholderPermissionImpl(Goal.class, StakeholderPermissionType.Delete));
		entityTypes.add(new StakeholderPermissionImpl(Actor.class, StakeholderPermissionType.Edit));
		entityTypes
				.add(new StakeholderPermissionImpl(Actor.class, StakeholderPermissionType.Grant));
		entityTypes
				.add(new StakeholderPermissionImpl(Actor.class, StakeholderPermissionType.Delete));
		entityTypes.add(new StakeholderPermissionImpl(Stakeholder.class,
				StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(Stakeholder.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(Stakeholder.class,
				StakeholderPermissionType.Delete));
		entityTypes.add(new StakeholderPermissionImpl(GlossaryTerm.class,
				StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(GlossaryTerm.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(GlossaryTerm.class,
				StakeholderPermissionType.Delete));
		entityTypes.add(new StakeholderPermissionImpl(Story.class, StakeholderPermissionType.Edit));
		entityTypes
				.add(new StakeholderPermissionImpl(Story.class, StakeholderPermissionType.Grant));
		entityTypes
				.add(new StakeholderPermissionImpl(Story.class, StakeholderPermissionType.Delete));

		entityTypes
				.add(new StakeholderPermissionImpl(UseCase.class, StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(UseCase.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(UseCase.class,
				StakeholderPermissionType.Delete));

		entityTypes.add(new StakeholderPermissionImpl(Scenario.class,
				StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(Scenario.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(Scenario.class,
				StakeholderPermissionType.Delete));

		entityTypes.add(new StakeholderPermissionImpl(ReportGenerator.class,
				StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(ReportGenerator.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(ReportGenerator.class,
				StakeholderPermissionType.Delete));

		return entityTypes;
	}
}
