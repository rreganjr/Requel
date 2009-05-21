/*
 * $Id: $
 * 
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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
package edu.harvard.fas.rregan.requel.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.ExportProjectCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;

/**
 * Request Handler that delivers Requel Project XML files by project name.
 * 
 * In the web.xml this is identified as ProjectXMLServlet.
 * @author ron
 */
@Component("ProjectXMLServlet")
@Scope("singleton")
public class ProjectXmlRequestHandler implements HttpRequestHandler {
	private static final Logger log = Logger.getLogger(ProjectXmlRequestHandler.class);

	private ProjectRepository projectRepository;
	private ProjectCommandFactory projectCommandFactory;
	private CommandHandler commandHandler;
	
	@Autowired
	public ProjectXmlRequestHandler(ProjectRepository projectRepository, 
			ProjectCommandFactory projectCommandFactory, CommandHandler commandHandler) {
		setProjectRepository(projectRepository);
		setProjectCommandFactory(projectCommandFactory);
		setCommandHandler(commandHandler);
	}
	
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String projectName = request.getParameter("project");
		try {
			Project project = getProjectRepository().findProjectByName(projectName);
			ExportProjectCommand command = getProjectCommandFactory().newExportProjectCommand();
			command.setProject(project);
			command.setOutputStream(response.getOutputStream());
			command = getCommandHandler().execute(command);
		} catch (Exception e) {
			log.error("could not export the project '" + projectName + "': " + e, e);
			throw new ServletException("Could not export project '" + projectName + "'", e);
		}
	}
	
	protected ProjectRepository getProjectRepository() {
		return projectRepository;
	}
	
	protected void setProjectRepository(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}
	
	protected ProjectCommandFactory getProjectCommandFactory() {
		return projectCommandFactory;
	}
	
	protected void setProjectCommandFactory(ProjectCommandFactory projectCommandFactory) {
		this.projectCommandFactory = projectCommandFactory;
	}
	
	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}
	
	protected void setCommandHandler(CommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}
}
