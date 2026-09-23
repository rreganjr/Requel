/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.EntityLockException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditReportGeneratorCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.exception.NoSuchOrganizationException;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import com.rreganjr.requel.user.impl.OrganizationImpl;

/**
 * @author ron
 */
@Controller("editProjectCommand")
@Scope("prototype")
public class EditProjectCommandImpl extends AbstractEditProjectCommand implements
		EditProjectCommand, AuthorizableCommand, ProjectScopedCommand {

	private static final Logger log = LoggerFactory.getLogger(EditProjectCommandImpl.class);

	public static final String BUILTIN_REPORT_GENERATOR_PATH = "xslt/project2html.xslt";

	private String name;
	private String description;
	private Long organizationId;
	private String organizationName;
	private Project project;
	private boolean analysisEnabled = true;
	private Integer expectedVersion;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditProjectCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	protected String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected String getText() {
		return description;
	}

	public void setText(String description) {
		this.description = description;
	}

	protected Long getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(Long organizationId) {
		this.organizationId = organizationId;
	}

	protected String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}

	@Override
	public void setExpectedVersion(Integer expectedVersion) {
		this.expectedVersion = expectedVersion;
	}

	protected Integer getExpectedVersion() {
		return expectedVersion;
	}

	@Override
	public void execute() {
		Organization organization = resolveOrganization();
		User user = getUserRepository().get(getEditedBy());
		ProjectImpl projectImpl = (ProjectImpl) getProject();

		// Check for uniqueness. Skipped when no name was supplied: a null name means "leave it
		// as it is", and the finder trims the name, so it would NPE (issue #316).
		if (getName() != null) {
			try {
				Project existing = getProjectRepository().findProjectByName(getName());
				if (projectImpl == null) {
					throw EntityException.uniquenessConflict(Project.class, existing, FIELD_NAME,
							EntityExceptionActionType.Creating);
				} else if (!existing.equals(projectImpl)) {
					throw EntityException.uniquenessConflict(Project.class, existing, FIELD_NAME,
							EntityExceptionActionType.Updating);
				}
			} catch (NoSuchEntityException e) {
			}
		}

		if (projectImpl == null) {
			projectImpl = createProject(organization, user);
			projectImpl.setText(getText());
		} else {
			// Enforce the caller-supplied optimistic-lock version on update (issue #108).
			projectImpl = getRepository().get(projectImpl);
			if (getExpectedVersion() != null
					&& getExpectedVersion().intValue() != projectImpl.getVersion()) {
				throw EntityLockException.staleEntity(Project.class, projectImpl,
						EntityExceptionActionType.Updating);
			}
			// Null leaves a property as it is; "" clears the description, and
			// organizationName "" clears the organization (issue #316).
			if (getName() != null) {
				projectImpl.setName(getName());
			}
			if (isOrganizationSupplied()) {
				projectImpl.setOrganization(organization);
			}
			if (getText() != null) {
				projectImpl.setText(getText());
			}
		}
		projectImpl = getRepository().merge(projectImpl);
		setProject(projectImpl);
	}

	@Override
	public void setAnalysisEnabled(boolean analysisEnabled) {
		this.analysisEnabled = analysisEnabled;
	}

	protected boolean isAnalysisEnabled() {
		return analysisEnabled;
	}

	@Override
	public void invokeAnalysis() {
		// TODO: does the project need to be analyzed?
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		// project == null means new project creation — defer to role-permission check in execute()
		if (project == null) return null;
		return new RequiresStakeholderPermission(Project.class, "Edit");
	}

	private ProjectImpl createProject(Organization organization, User user) {
		ProjectImpl projectImpl = getProjectRepository().persist(
				new ProjectImpl(getName(), user, organization));

		// TODO: use a command to create the stakeholder
		// EditUserStakeholderCommand command =
		// getProjectCommandFactory().newEditUserStakeholderCommand();

		// create a stakeholder for the user that creates the project
		UserStakeholderImpl creatorStakeholder = getProjectRepository().persist(
				new UserStakeholderImpl(projectImpl, user, (com.rreganjr.requel.user.User)user));
		for (StakeholderPermission permission : getProjectRepository()
				.findAvailableStakeholderPermissions()) {
			creatorStakeholder.grantStakeholderPermission(permission);
		}

		// TODO: use a command to create the stakeholder
		// Create a stakeholder for the assistant, holding the set defined in one place for
		// creation, import and repair alike (issue #302). Before that it was created with no
		// permissions at all, so every assistant annotation on a UI-created project was
		// refused by AuthorizingCommandHandler while the same project imported from XML
		// worked. A missing assistant user is a warning rather than a failure, matching the
		// import path: a deployment without the assistant can still create projects.
		try {
			com.rreganjr.requel.user.User assistantUser = getUserRepository()
					.findUserByUsername("assistant");
			UserStakeholderImpl assistantStakeholder = getProjectRepository().persist(
					new UserStakeholderImpl(projectImpl, user, assistantUser));
			for (StakeholderPermission permission : getProjectRepository()
					.findAssistantStakeholderPermissions()) {
				assistantStakeholder.grantStakeholderPermission(permission);
			}
			// The persisted row has to join the project's collection for
			// ProjectImpl.getUserStakeholder() - and so the authorization check that calls it -
			// to see it without a reload, and the assistant needs project membership the same
			// way the import path grants it.
			projectImpl.getStakeholders().add(assistantStakeholder);
			try {
				assistantStakeholder.ensureProjectMembership();
			} catch (com.rreganjr.requel.user.exception.NoSuchRoleForUserException e) {
				log.warn("Assistant user missing ProjectUserRole; skipping membership"
						+ " enforcement on " + projectImpl.getName(), e);
			}
		} catch (NoSuchUserException e) {
			log.warn("The assistant user doesn't exist and could not be added as a stakeholder to "
					+ projectImpl.getName());
		}

		// Ensure the creator carries a ProjectUserRole so activeProjects can be tracked.
		com.rreganjr.requel.user.User requelUser = (com.rreganjr.requel.user.User) user;
		if (!requelUser.hasRole(ProjectUserRole.class)) {
			requelUser.grantRole(ProjectUserRole.class);
			getUserRepository().merge(requelUser);
		}
		ProjectUserRole role = requelUser.getRoleForType(ProjectUserRole.class);
		role.getActiveProjects().add(projectImpl);

		addBuiltinReportGenerator(projectImpl, user);

		return projectImpl;
	}

	/**
	 * Whether the caller said anything about the organization. On update, neither field set
	 * leaves the organization as it is (issue #316).
	 */
	private boolean isOrganizationSupplied() {
		return (getOrganizationId() != null) || (getOrganizationName() != null);
	}

	/**
	 * The organization the caller asked for, or null for none. A blank organizationName means
	 * "no organization" rather than an organization with an empty name (issue #316).
	 */
	private Organization resolveOrganization() {
		if (getOrganizationId() != null) {
			return getUserRepository().findOrganizationById(getOrganizationId());
		}
		if (getOrganizationName() != null && !getOrganizationName().trim().isEmpty()) {
			try {
				return getUserRepository().findOrganizationByName(getOrganizationName());
			} catch (NoSuchOrganizationException e) {
				return getUserRepository().persist(new OrganizationImpl(getOrganizationName()));
			}
		}
		return null;
	}

	private void addBuiltinReportGenerator(Project project, User user) {
		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
					BUILTIN_REPORT_GENERATOR_PATH);
			EditReportGeneratorCommand command = getProjectCommandFactory()
					.newEditReportGeneratorCommand();
			command.setEditedBy(user);
			command.setProjectOrDomain(project);
			command.setName("HTML Specification");
			command.setText(IOUtils.toString(inputStream));
			getCommandHandler().execute(command);
		} catch (Exception e) {
			log.error("The builtin report generator could not be added to " + project, e);
		}
	}
}
