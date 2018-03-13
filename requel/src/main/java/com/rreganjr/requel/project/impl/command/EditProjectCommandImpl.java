/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditReportGeneratorCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.exception.NoSuchOrganizationException;
import com.rreganjr.requel.user.impl.OrganizationImpl;

/**
 * @author ron
 */
@Controller("editProjectCommand")
@Scope("prototype")
public class EditProjectCommandImpl extends AbstractEditProjectCommand implements
		EditProjectCommand {

	public static final String BUILTIN_REPORT_GENERATOR_PATH = "xslt/project2html.xslt";

	private String name;
	private String description;
	private String organizationName;
	private Project project;
	private boolean analysisEnabled = true;

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

	protected String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}

	@Override
	public void execute() {
		Organization organization = null;
		try {
			organization = getUserRepository().findOrganizationByName(getOrganizationName());
		} catch (NoSuchOrganizationException e) {
			organization = getUserRepository().persist(new OrganizationImpl(getOrganizationName()));
		}
		User user = getUserRepository().get(getEditedBy());
		ProjectImpl projectImpl = (ProjectImpl) getProject();

		// check for uniqueness
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

		if (projectImpl == null) {
			projectImpl = createProject(organization, user);
		} else {
			projectImpl.setName(getName());
			projectImpl.setOrganization(organization);
		}
		projectImpl.setText(getText());
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

	private ProjectImpl createProject(Organization organization, User user) {
		ProjectImpl projectImpl = getProjectRepository().persist(
				new ProjectImpl(getName(), user, organization));

		// TODO: use a command to create the stakeholder
		// EditUserStakeholderCommand command =
		// getProjectCommandFactory().newEditUserStakeholderCommand();

		// create a stakeholder for the user that creates the project
		UserStakeholderImpl creatorStakeholder = getProjectRepository().persist(
				new UserStakeholderImpl(projectImpl, user, user));
		for (StakeholderPermission permission : getProjectRepository()
				.findAvailableStakeholderPermissions()) {
			creatorStakeholder.grantStakeholderPermission(permission);
		}

		// TODO: use a command to create the stakeholder
		// create a stakeholder for assistant
		User assistantUser = getUserRepository().findUserByUsername("assistant");
		getProjectRepository().persist(new UserStakeholderImpl(projectImpl, user, assistantUser));

		ProjectUserRole role = user.getRoleForType(ProjectUserRole.class);
		role.getActiveProjects().add(projectImpl);

		addBuiltinReportGenerator(projectImpl, user);

		return projectImpl;
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
