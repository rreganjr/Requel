/*
 * $Id: ImportProjectCommandImpl.java,v 1.23 2009/03/30 11:54:27 rregan Exp $
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

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermission;
import edu.harvard.fas.rregan.requel.project.command.EditReportGeneratorCommand;
import edu.harvard.fas.rregan.requel.project.command.ImportProjectCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.ProjectImpl;
import edu.harvard.fas.rregan.requel.project.impl.StakeholderImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Controller("importProjectCommand")
@Scope("prototype")
public class ImportProjectCommandImpl extends AbstractEditProjectCommand implements
		ImportProjectCommand {

	private Project project;
	private InputStream inputStream;
	private String name;
	private boolean analysisEnabled = false;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public ImportProjectCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.command.ImportProjectCommand#getProject()
	 */
	@Override
	public Project getProject() {
		return project;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.command.ImportProjectCommand#setProject(edu.harvard.fas.rregan.requel.project.Project)
	 */
	@Override
	public void setProject(Project project) {
		this.project = project;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.command.ImportProjectCommand#setInputStream(java.io.InputStream)
	 */
	@Override
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	protected InputStream getInputStream() {
		return inputStream;
	}

	protected String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected boolean isAnalysisEnabled() {
		return analysisEnabled;
	}

	public void setAnalysisEnabled(boolean analysisEnabled) {
		this.analysisEnabled = analysisEnabled;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() {
		try {
			User createdBy = getUserRepository().get(getEditedBy());
			// NOTE: the annotation classes need to be explicitly supplied to
			// the newInstance or an IllegalAnnotationExceptions will occur for
			// AbstractProjectOrDomainEntity.getAnnotations()
			JAXBContext context = JAXBContext
					.newInstance(ExportProjectCommandImpl.CLASSES_FOR_JAXB);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setListener(new UnmarshallerListener(getProjectRepository(),
					getUserRepository(), createdBy, getName()));
			ProjectImpl project = (ProjectImpl) unmarshaller.unmarshal(getInputStream());
			if (project.getCreatedBy() == null) {
				project.setCreatedBy(createdBy);
			}
			// add the user that imported the project as a stakeholder
			addUserAsStakeholder(project, createdBy, createdBy);

			try {
				// add the assistant as a stakeholder
				addUserAsStakeholder(project, getUserRepository().findUserByUsername("assistant"),
						createdBy);
			} catch (NoSuchUserException e) {
				log.warn("The assistant user doesn't exist and could not "
						+ " be added as a stakeholder to " + project.getName());
			}
			setProject(getProjectRepository().persist(project));

			// TODO: use a command to edit each stakeholder?
			// add the project to all the stakeholder user ProjectUserRoles
			for (Stakeholder stakeholder : project.getStakeholders()) {
				if (stakeholder.isUserStakeholder()) {
					stakeholder.getUser().getRoleForType(ProjectUserRole.class).getActiveProjects()
							.add(project);
				}
			}
			if (project.getReportGenerators().isEmpty()) {
				addBuiltinReportGenerator(project, createdBy);
			}
		} catch (Exception e) {
			log.error(e, e);
		}
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			getAssistantManager().analyzeProject(getProject());
		}
	}

	private void addUserAsStakeholder(Project project, User user, User editedBy) {
		// TODO: should this use the EditStakeholderCommand?
		if (user.hasRole(ProjectUserRole.class)) {
			boolean alreadyAStakeholder = false;
			for (Stakeholder stakeholder : project.getStakeholders()) {
				if (user.equals(stakeholder.getUser())) {
					alreadyAStakeholder = true;
					break;
				}
			}
			if (!alreadyAStakeholder) {
				Stakeholder creatorStakeholder = new StakeholderImpl(project, editedBy, user);
				getProjectRepository().persist(creatorStakeholder);
				for (StakeholderPermission permission : getProjectRepository()
						.findAvailableStakeholderPermissions()) {
					creatorStakeholder.grantStakeholderPermission(permission);
				}
				project.getStakeholders().add(creatorStakeholder);
			}
		}
	}

	private void addBuiltinReportGenerator(Project project, User user) {
		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
					EditProjectCommandImpl.BUILTIN_REPORT_GENERATOR_PATH);
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