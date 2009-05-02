/*
 * $Id: DeleteReportGeneratorCommandImpl.java,v 1.3 2009/03/30 11:54:26 rregan Exp $
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ReportGenerator;
import edu.harvard.fas.rregan.requel.project.command.DeleteReportGeneratorCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * Delete a reportGenerator from a project, cleaning up references from other
 * project entities, reportGenerator relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteReportGeneratorCommand")
@Scope("prototype")
public class DeleteReportGeneratorCommandImpl extends AbstractEditProjectCommand implements
		DeleteReportGeneratorCommand {

	private ReportGenerator reportGenerator;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteReportGeneratorCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setReportGenerator(ReportGenerator reportGenerator) {
		this.reportGenerator = reportGenerator;
	}

	protected ReportGenerator getReportGenerator() {
		return reportGenerator;
	}

	@Override
	public void execute() throws Exception {
		ReportGenerator reportGenerator = getRepository().get(getReportGenerator());
		User editedBy = getRepository().get(getEditedBy());
		Set<Annotation> annotations = new HashSet<Annotation>(reportGenerator.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(reportGenerator);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		reportGenerator.getProjectOrDomain().getReportGenerators().remove(reportGenerator);
		getRepository().delete(reportGenerator);
	}

}
