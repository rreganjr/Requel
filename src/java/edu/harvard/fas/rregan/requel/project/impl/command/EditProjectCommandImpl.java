/*
 * $Id: EditProjectCommandImpl.java,v 1.23 2009/03/30 11:54:29 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
import edu.harvard.fas.rregan.requel.project.StakeholderPermission;
import edu.harvard.fas.rregan.requel.project.command.EditProjectCommand;
import edu.harvard.fas.rregan.requel.project.command.EditReportGeneratorCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.ProjectImpl;
import edu.harvard.fas.rregan.requel.project.impl.StakeholderImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.Organization;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchOrganizationException;
import edu.harvard.fas.rregan.requel.user.impl.OrganizationImpl;

/**
 * @author ron
 */
@Controller("editProjectCommand")
@Scope("prototype")
public class EditProjectCommandImpl extends AbstractEditProjectCommand implements
		EditProjectCommand {

	public static final String BUILTIN_REPORT_GENERATOR_PATH = "resources/xslt/project2html.xslt";

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

		StakeholderImpl creatorStakeholder = getProjectRepository().persist(
				new StakeholderImpl(projectImpl, user, user));
		for (StakeholderPermission permission : getProjectRepository()
				.findAvailableStakeholderPermissions()) {
			creatorStakeholder.grantStakeholderPermission(permission);
		}

		User assistantUser = getUserRepository().findUserByUsername("assistant");
		getProjectRepository().persist(new StakeholderImpl(projectImpl, user, assistantUser));

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
