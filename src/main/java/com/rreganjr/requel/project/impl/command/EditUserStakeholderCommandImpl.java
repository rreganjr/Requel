/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.requel.project.impl.command;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectTeam;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ProjectTeamImpl;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Create or edit a stakeholder for a system user.
 * 
 * @author ron
 */
@Controller("editUserStakeholderCommand")
@Scope("prototype")
public class EditUserStakeholderCommandImpl extends AbstractEditProjectOrDomainEntityCommand
		implements EditUserStakeholderCommand {

	private UserStakeholder stakeholder;
	private String username;
	private Set<String> permissionKeys;
	private String teamName;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditUserStakeholderCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	public UserStakeholder getStakeholder() {
		return stakeholder;
	}

	public void setStakeholder(UserStakeholder stakeholder) {
		this.stakeholder = stakeholder;
	}

	protected String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setStakeholderPermissions(Set<String> permissionKeys) {
		this.permissionKeys = permissionKeys;
	}

	protected Set<String> getStakeholderPermissions() {
		return permissionKeys;
	}

	protected String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	@Override
	public void execute() {
		ProjectOrDomain projectOrDomain = getProjectRepository().get(getProjectOrDomain());
		User editedBy = getProjectRepository().get(getEditedBy());
		User user = getUserRepository().findUserByUsername(getUsername());

		UserStakeholderImpl stakeholderImpl = (UserStakeholderImpl) getStakeholder();

		// check for uniqueness
		try {
			UserStakeholder existing = getProjectRepository()
					.findStakeholderByProjectOrDomainAndUser(projectOrDomain, user);
			if (stakeholderImpl == null) {
				throw EntityException.uniquenessConflict(Stakeholder.class, existing, FIELD_USER,
						EntityExceptionActionType.Creating);
			} else if (!existing.equals(stakeholderImpl)) {
				throw EntityException.uniquenessConflict(Stakeholder.class, existing, FIELD_USER,
						EntityExceptionActionType.Updating);
			}
		} catch (NoSuchEntityException e) {
		}

		ProjectTeam oldTeam = null;
		ProjectTeam newTeam = null;
		if (stakeholderImpl != null) {
			oldTeam = getRepository().get(stakeholderImpl.getTeam());
		}

		String teamName = getTeamName();
		if ((teamName != null) && (teamName.length() > 0)) {
			for (ProjectTeam aTeam : projectOrDomain.getTeams()) {
				if (teamName.equalsIgnoreCase(aTeam.getName())) {
					newTeam = aTeam;
					break;
				}
			}
			if (newTeam == null) {
				// TODO: add/use create team command?
				newTeam = getRepository().persist(
						new ProjectTeamImpl(projectOrDomain, editedBy, teamName));
			}
		}

		if (stakeholderImpl == null) {
			stakeholderImpl = getProjectRepository().persist(
					new UserStakeholderImpl(projectOrDomain, editedBy, user));
		} else {
			stakeholderImpl.setUser(user);
		}
		stakeholderImpl.setTeam(newTeam);
		stakeholderImpl = getProjectRepository().merge(stakeholderImpl);

		if (oldTeam != null) {
			oldTeam.getMembers().remove(stakeholderImpl);
		}
		if (newTeam != null) {
			newTeam.getMembers().add(stakeholderImpl);
		}

		for (StakeholderPermission permission : getProjectRepository()
				.findAvailableStakeholderPermissions()) {
			if (stakeholderImpl.hasPermission(permission)
					&& !getStakeholderPermissions().contains(permission.getPermissionKey())) {
				stakeholderImpl.revokeStakeholderPermission(permission);
			} else if (!stakeholderImpl.hasPermission(permission)
					&& getStakeholderPermissions().contains(permission.getPermissionKey())) {
				stakeholderImpl.grantStakeholderPermission(permission);
			}
		}

		if (user != null) {
			ProjectUserRole projectRole = user.getRoleForType(ProjectUserRole.class);
			projectRole.getActiveProjects().add((Project) projectOrDomain);
		}
		setStakeholder(stakeholderImpl);
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			// TODO: analyze stakeholder?
		}
	}
}
