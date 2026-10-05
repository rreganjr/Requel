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
package com.rreganjr.requel.project.impl.repository.init;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.AssistantDefinition;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;

/** Issue #264: the AssistantDefinition[Edit] permission is seeded and backfilled to owners. */
class StakeholderPermissionsInitializerTest {

	private final ProjectRepository repository = mock(ProjectRepository.class);
	private final StakeholderPermission projectEdit = new StakeholderPermissionImpl(Project.class,
			StakeholderPermissionType.Edit);
	private final StakeholderPermission projectDelete = new StakeholderPermissionImpl(
			Project.class, StakeholderPermissionType.Delete);
	private final StakeholderPermission definitionEdit = new StakeholderPermissionImpl(
			AssistantDefinition.class, StakeholderPermissionType.Edit);

	private UserStakeholder owner(StakeholderPermission... held) {
		UserStakeholder owner = mock(UserStakeholder.class);
		when(owner.getStakeholderPermissions()).thenReturn(new HashSet<>(Set.of(held)));
		return owner;
	}

	@Test
	void ownersWithProjectEditGetTheDefinitionPermissionOnce() {
		when(repository.findStakeholderPermission(any(), any())).thenAnswer(inv -> {
			Class<?> type = inv.getArgument(0);
			StakeholderPermissionType permission = inv.getArgument(1);
			if (type == AssistantDefinition.class) {
				return definitionEdit;
			}
			return permission == StakeholderPermissionType.Edit ? projectEdit : projectDelete;
		});
		UserStakeholder without = owner(projectEdit, projectDelete);
		UserStakeholder already = owner(projectEdit, projectDelete, definitionEdit);
		when(repository.findUserStakeholdersWithPermission(projectEdit))
				.thenReturn(new java.util.LinkedHashSet<>(List.of(without, already)));

		new StakeholderPermissionsInitializer(repository).initialize();

		// the permission row is looked up (and exists), so nothing is persisted for it
		verify(repository, org.mockito.Mockito.atLeastOnce()).findStakeholderPermission(
				AssistantDefinition.class,
				StakeholderPermissionType.Edit);
		verify(without).grantStakeholderPermission(definitionEdit);
		verify(already, never()).grantStakeholderPermission(eq(definitionEdit));
		verify(repository).merge(without);
		verify(repository, never()).merge(already);
		verify(repository, never()).persist(any());
	}

	/** #75: every UseCase[Edit] holder gets what it implies, once. */
	@Test
	void useCaseEditHoldersGetScenarioAndActorEditOnce() {
		StakeholderPermission useCaseEdit = new StakeholderPermissionImpl(
				com.rreganjr.requel.project.UseCase.class, StakeholderPermissionType.Edit);
		StakeholderPermission scenarioEdit = new StakeholderPermissionImpl(
				com.rreganjr.requel.project.Scenario.class, StakeholderPermissionType.Edit);
		StakeholderPermission actorEdit = new StakeholderPermissionImpl(
				com.rreganjr.requel.project.Actor.class, StakeholderPermissionType.Edit);
		when(repository.findStakeholderPermission(any(), any())).thenAnswer(inv -> {
			Class<?> type = inv.getArgument(0);
			StakeholderPermissionType permission = inv.getArgument(1);
			if (type == AssistantDefinition.class) {
				return definitionEdit;
			}
			return permission == StakeholderPermissionType.Edit ? projectEdit : projectDelete;
		});
		when(repository.findAvailableStakeholderPermissions())
				.thenReturn(new java.util.LinkedHashSet<>(List.of(useCaseEdit, scenarioEdit, actorEdit)));
		UserStakeholder writer = owner(useCaseEdit);
		UserStakeholder closed = owner(useCaseEdit, scenarioEdit, actorEdit);
		when(repository.findUserStakeholdersWithPermission(useCaseEdit))
				.thenReturn(new java.util.LinkedHashSet<>(List.of(writer, closed)));

		new StakeholderPermissionsInitializer(repository).initialize();

		verify(writer).grantStakeholderPermission(scenarioEdit);
		verify(writer).grantStakeholderPermission(actorEdit);
		verify(closed, never()).grantStakeholderPermission(any());
		verify(repository, never()).merge(closed);
	}
}
