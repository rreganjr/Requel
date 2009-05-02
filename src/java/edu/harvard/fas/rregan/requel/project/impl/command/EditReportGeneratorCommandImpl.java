/*
 * $Id: EditReportGeneratorCommandImpl.java,v 1.7 2009/03/30 11:54:30 rregan Exp $
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ReportGenerator;
import edu.harvard.fas.rregan.requel.project.command.EditReportGeneratorCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.ReportGeneratorImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editReportGeneratorCommand")
@Scope("prototype")
public class EditReportGeneratorCommandImpl extends AbstractEditProjectOrDomainEntityCommand
		implements EditReportGeneratorCommand {

	private ReportGenerator reportGenerator;
	private String text;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditReportGeneratorCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	public ReportGenerator getReportGenerator() {
		return reportGenerator;
	}

	public void setReportGenerator(ReportGenerator reportGenerator) {
		this.reportGenerator = reportGenerator;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	protected String getText() {
		return text;
	}

	@Override
	public void execute() {
		User editedBy = getProjectRepository().get(getEditedBy());
		ReportGeneratorImpl reportGeneratorImpl = (ReportGeneratorImpl) getReportGenerator();
		ProjectOrDomain projectOrDomain = getProjectRepository().get(getProjectOrDomain());

		// check for uniqueness
		try {
			ReportGenerator existing = getProjectRepository()
					.findReportGeneratorByProjectOrDomainAndName(projectOrDomain, getName());
			if (reportGeneratorImpl == null) {
				throw EntityException.uniquenessConflict(ReportGenerator.class, existing,
						FIELD_NAME, EntityExceptionActionType.Creating);
			} else if (!existing.equals(reportGeneratorImpl)) {
				throw EntityException.uniquenessConflict(ReportGenerator.class, existing,
						FIELD_NAME, EntityExceptionActionType.Updating);
			}
		} catch (NoSuchEntityException e) {
		}

		if (reportGeneratorImpl == null) {
			reportGeneratorImpl = getProjectRepository().persist(
					new ReportGeneratorImpl(projectOrDomain, editedBy, getName(), getText()));
		} else {
			reportGeneratorImpl.setName(getName());
			reportGeneratorImpl.setText(getText());
		}
		reportGeneratorImpl = getProjectRepository().merge(reportGeneratorImpl);
		setReportGenerator(reportGeneratorImpl);
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			// TODO: analyze report generator?
		}
	}
}
