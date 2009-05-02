/*
 * $Id: EditStakeholderCommandImpl.java,v 1.25 2009/03/30 11:54:31 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.project.impl.command;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ProjectTeam;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermission;
import edu.harvard.fas.rregan.requel.project.command.EditStakeholderCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.ProjectTeamImpl;
import edu.harvard.fas.rregan.requel.project.impl.StakeholderImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editStakeholderCommand")
@Scope("prototype")
public class EditStakeholderCommandImpl extends AbstractEditProjectOrDomainEntityCommand implements
		EditStakeholderCommand {

	private Stakeholder stakeholder;
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
	public EditStakeholderCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	public Stakeholder getStakeholder() {
		return stakeholder;
	}

	public void setStakeholder(Stakeholder stakeholder) {
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
		User user = null;
		String username = getUsername();
		if ((username != null) && (username.length() > 0)) {
			user = getUserRepository().findUserByUsername(username);
		}

		StakeholderImpl stakeholderImpl = (StakeholderImpl) getStakeholder();

		// check for uniqueness
		try {
			Stakeholder existing;
			if (user != null) {
				existing = getProjectRepository().findStakeholderByProjectOrDomainAndUser(
						projectOrDomain, user);
			} else {
				existing = getProjectRepository().findStakeholderByProjectOrDomainAndName(
						projectOrDomain, getName());
			}
			if (stakeholderImpl == null) {
				throw EntityException.uniquenessConflict(Stakeholder.class, existing,
						(user == null ? FIELD_NAME : FIELD_USER),
						EntityExceptionActionType.Creating);
			} else if (!existing.equals(stakeholderImpl)) {
				throw EntityException.uniquenessConflict(Stakeholder.class, existing,
						(user == null ? FIELD_NAME : FIELD_USER),
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
			stakeholderImpl = createStakeholder(projectOrDomain, user, editedBy, getName());
		} else {
			stakeholderImpl.setName(getName());
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

	private StakeholderImpl createStakeholder(ProjectOrDomain projectOrDomain,
			User stakeholderUser, User createdBy, String name) {
		StakeholderImpl stakeholderImpl = null;
		if (stakeholderUser != null) {
			stakeholderImpl = new StakeholderImpl(projectOrDomain, createdBy, stakeholderUser);
		} else {
			stakeholderImpl = new StakeholderImpl(projectOrDomain, createdBy, name);
		}
		return getProjectRepository().persist(stakeholderImpl);
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			// TODO: analyze stakeholder?
		}
	}
}
