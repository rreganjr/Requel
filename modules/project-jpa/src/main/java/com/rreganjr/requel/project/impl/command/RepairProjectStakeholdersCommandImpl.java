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
package com.rreganjr.requel.project.impl.command;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresSystemRole;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RepairProjectStakeholdersCommand;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Restore the creator's stakeholder row and permissions on projects that lost them
 * (issue #256). See {@link RepairProjectStakeholdersCommand} for why this exists and
 * what it deliberately leaves alone.
 *
 * <p>
 * The grant loop mirrors {@code EditProjectCommandImpl.createProject()} exactly - every
 * row from {@code findAvailableStakeholderPermissions()} - so a repaired project is
 * indistinguishable from a freshly created one. That is the point: this command must
 * never become a third definition of "what the creator gets", or it will drift from
 * creation the way creation and import drifted from each other.
 *
 * @author ron
 */
@Controller("repairProjectStakeholdersCommand")
@Scope("prototype")
public class RepairProjectStakeholdersCommandImpl extends AbstractEditProjectCommand
		implements RepairProjectStakeholdersCommand, AuthorizableCommand {

	private static final Logger log = LoggerFactory
			.getLogger(RepairProjectStakeholdersCommandImpl.class);

	private static final String ASSISTANT_USERNAME = "assistant";

	private String projectName;

	private int projectsScanned;

	private int stakeholdersCreated;

	private int permissionsGranted;

	private int permissionsRevoked;

	private int projectsSkipped;

	@Autowired
	public RepairProjectStakeholdersCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	protected String getProjectName() {
		return projectName;
	}

	@Override
	public int getProjectsScanned() {
		return projectsScanned;
	}

	@Override
	public int getStakeholdersCreated() {
		return stakeholdersCreated;
	}

	@Override
	public int getPermissionsGranted() {
		return permissionsGranted;
	}

	@Override
	public int getPermissionsRevoked() {
		return permissionsRevoked;
	}

	@Override
	public int getProjectsSkipped() {
		return projectsSkipped;
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		Set<StakeholderPermission> available = getProjectRepository()
				.findAvailableStakeholderPermissions();
		Set<StakeholderPermission> assistantPermissions = getProjectRepository()
				.findAssistantStakeholderPermissions();
		// Resolved once: a deployment without the assistant user should log that fact once,
		// not once per project, and should still repair every creator.
		com.rreganjr.requel.user.User assistant = resolveAssistant();

		for (Project project : resolveProjects()) {
			projectsScanned++;
			Project managed = getProjectRepository().get(project);
			repair(managed, available, editedBy);
			if (assistant != null) {
				repairAssistant(managed, assistant, assistantPermissions, editedBy);
			}
		}
		log.info("Repaired project stakeholders: scanned={}, created={}, granted={}, revoked={},"
				+ " skipped={}", projectsScanned, stakeholdersCreated, permissionsGranted,
				permissionsRevoked, projectsSkipped);
	}

	private Collection<? extends Project> resolveProjects() {
		if (getProjectName() != null && !getProjectName().isBlank()) {
			return List.of(getProjectRepository().findProjectByName(getProjectName().trim()));
		}
		return getProjectRepository().findAllProjects();
	}

	/**
	 * Ensure this project's creator holds a stakeholder row with every available
	 * permission. A project whose {@code createdBy} is null - or set to a user row that
	 * no longer exists - is counted as skipped rather than failing the whole run: a
	 * repair that aborts partway through 500 projects because one of them is malformed
	 * is worse than one that reports what it could not fix.
	 */
	private void repair(Project project, Set<StakeholderPermission> available, User editedBy) {
		com.rreganjr.requel.user.User creator = resolveCreator(project);
		if (creator == null) {
			projectsSkipped++;
			return;
		}

		UserStakeholder stakeholder = findUserStakeholder(project, creator);
		if (stakeholder == null) {
			stakeholder = getProjectRepository()
					.persist(new UserStakeholderImpl(project, editedBy, creator));
			stakeholdersCreated++;
		}

		for (StakeholderPermission permission : available) {
			if (!stakeholder.getStakeholderPermissions().contains(permission)) {
				stakeholder.grantStakeholderPermission(permission);
				permissionsGranted++;
			}
		}
		getProjectRepository().merge(stakeholder);
	}

	/**
	 * Bring this project's {@code assistant} stakeholder to exactly the assistant's permission
	 * set (issue #302), creating the row when the project predates it having one.
	 *
	 * <p>
	 * This is the one place a repair revokes. A project imported before #302 had the assistant
	 * granted every available permission by the import loop, and "the assistant holds the same
	 * set however the project was created" is not true while those extras remain. The narrowing
	 * is scoped to the assistant's own row: {@link #repair} handles humans and only ever grants.
	 *
	 */
	private void repairAssistant(Project project, com.rreganjr.requel.user.User assistant,
			Set<StakeholderPermission> assistantPermissions, User editedBy) {
		UserStakeholder stakeholder = findUserStakeholder(project, assistant);
		if (stakeholder == null) {
			stakeholder = getProjectRepository()
					.persist(new UserStakeholderImpl(project, editedBy, assistant));
			project.getStakeholders().add(stakeholder);
			stakeholdersCreated++;
		}

		for (StakeholderPermission permission : assistantPermissions) {
			if (!stakeholder.getStakeholderPermissions().contains(permission)) {
				stakeholder.grantStakeholderPermission(permission);
				permissionsGranted++;
			}
		}
		for (StakeholderPermission held : new LinkedHashSet<StakeholderPermission>(
				stakeholder.getStakeholderPermissions())) {
			if (!assistantPermissions.contains(held)) {
				stakeholder.revokeStakeholderPermission(held);
				permissionsRevoked++;
			}
		}
		getProjectRepository().merge(stakeholder);
	}

	/**
	 * @return the assistant user, or null on a deployment that has none - in which case there
	 *         is no assistant row to repair and the creator repair still runs.
	 */
	private com.rreganjr.requel.user.User resolveAssistant() {
		try {
			return getUserRepository().findUserByUsername(ASSISTANT_USERNAME);
		} catch (Exception e) {
			log.warn("The assistant user does not resolve; skipping assistant stakeholder"
					+ " repair: {}", e.getMessage());
			return null;
		}
	}

	private com.rreganjr.requel.user.User resolveCreator(Project project) {
		if (project.getCreatedBy() == null) {
			return null;
		}
		try {
			return getUserRepository()
					.findUserByUsername(project.getCreatedBy().getUsername());
		} catch (Exception e) {
			log.warn("Project '{}' createdBy does not resolve to a user; skipping: {}",
					project.getName(), e.getMessage());
			return null;
		}
	}

	private UserStakeholder findUserStakeholder(Project project,
			com.rreganjr.requel.user.User user) {
		for (Stakeholder s : project.getStakeholders()) {
			if (s.matchesUser(user) && s instanceof UserStakeholder us) {
				return us;
			}
		}
		return null;
	}

	/**
	 * Creating stakeholder rows and granting permissions is identity management, so this
	 * takes the system-administrator role rather than any per-project permission - there is no project
	 * to be a stakeholder of when the whole point is that the row is missing.
	 */
	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresSystemRole(SystemAdminUserRole.class);
	}
}
