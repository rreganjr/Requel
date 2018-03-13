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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.command.EditReportGeneratorCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ReportGeneratorImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

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
