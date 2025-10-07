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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition;
import com.rreganjr.requel.annotation.impl.ChangeSpellingPosition;
import com.rreganjr.requel.annotation.impl.IssueImpl;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.annotation.impl.NoteImpl;
import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.requel.project.DomainAdminUserRole;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.command.ExportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.AddActorPosition;
import com.rreganjr.requel.project.impl.AddGlossaryTermPosition;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.SystemAdminUserRole;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.impl.UserImpl;

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
	 * @see com.rreganjr.requel.project.command.ExportProjectCommand#setProject(com.rreganjr.requel.project.Project)
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
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() {
		try {
			// NOTE: the classes referenced by Xml
			JAXBContext context = JAXBContext.newInstance(CLASSES_FOR_JAXB);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.rreganjr.com/requel http://requel.sourceforge.net/integration/1.0/project.xsd");
			marshaller.marshal(project, getOutputStream());
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
