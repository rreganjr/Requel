/*
 * $Id$
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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.impl.AbstractAnnotation;
import edu.harvard.fas.rregan.requel.annotation.impl.AddWordToDictionaryPosition;
import edu.harvard.fas.rregan.requel.annotation.impl.ChangeSpellingPosition;
import edu.harvard.fas.rregan.requel.annotation.impl.IssueImpl;
import edu.harvard.fas.rregan.requel.annotation.impl.LexicalIssue;
import edu.harvard.fas.rregan.requel.annotation.impl.NoteImpl;
import edu.harvard.fas.rregan.requel.annotation.impl.PositionImpl;
import edu.harvard.fas.rregan.requel.project.DomainAdminUserRole;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
import edu.harvard.fas.rregan.requel.project.command.ExportProjectCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.AddActorPosition;
import edu.harvard.fas.rregan.requel.project.impl.AddGlossaryTermPosition;
import edu.harvard.fas.rregan.requel.project.impl.ProjectImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.SystemAdminUserRole;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.impl.OrganizationImpl;
import edu.harvard.fas.rregan.requel.user.impl.UserImpl;

/**
 * @author ron
 */
@Controller("exportProjectCommand")
@Scope("prototype")
public class ExportProjectCommandImpl extends AbstractProjectCommand implements
		ExportProjectCommand {

	/**
	 * An array of classes to pass to JAXBContext.newInstance() that includes
	 * all the classes that are used in XmlElementRef annotations
	 */
	public static final Class<?>[] CLASSES_FOR_JAXB;
	static {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add(ProjectImpl.class); // project entites are found by
		// reachability
		classes.add(AbstractAnnotation.class);
		classes.add(NoteImpl.class);
		classes.add(IssueImpl.class);
		classes.add(LexicalIssue.class);
		classes.add(UserImpl.class);
		classes.add(OrganizationImpl.class);
		classes.add(SystemAdminUserRole.class);
		classes.add(ProjectUserRole.class);
		classes.add(DomainAdminUserRole.class);
		classes.add(PositionImpl.class);
		classes.add(ChangeSpellingPosition.class);
		classes.add(AddWordToDictionaryPosition.class);
		classes.add(AddGlossaryTermPosition.class);
		classes.add(AddActorPosition.class);
		CLASSES_FOR_JAXB = classes.toArray(new Class<?>[classes.size()]);
	}

	private Project project;
	private OutputStream outputStream;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public ExportProjectCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.command.ExportProjectCommand#setProject(edu.harvard.fas.rregan.requel.project.Project)
	 */
	@Override
	public void setProject(Project project) {
		this.project = project;
	}

	@Override
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	protected OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() {
		try {
			// NOTE: the classes referenced by Xml
			JAXBContext context = JAXBContext.newInstance(CLASSES_FOR_JAXB);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.people.fas.harvard.edu/~rregan/requel http://requel.sourceforge.net/integration/1.0/project.xsd");
			marshaller.marshal(project, getOutputStream());
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
