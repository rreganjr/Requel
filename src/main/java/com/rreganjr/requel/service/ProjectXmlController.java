/*
 * $Id$
 * 
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
package com.rreganjr.requel.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestHandler;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.ExportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Request Handler that delivers Requel Project XML files by project name.
 * 
 * In the web.xml this is identified as ProjectXMLServlet.
 * @author ron
 */
@RequestMapping("/projectxml")
@Controller("ProjectXmlController")
public class ProjectXmlController {
	private static final Logger log = Logger.getLogger(ProjectXmlController.class);

	private ProjectRepository projectRepository;
	private ProjectCommandFactory projectCommandFactory;
	private CommandHandler commandHandler;
	
	@Autowired
	public ProjectXmlController(ProjectRepository projectRepository,
								ProjectCommandFactory projectCommandFactory, CommandHandler commandHandler) {
		setProjectRepository(projectRepository);
		setProjectCommandFactory(projectCommandFactory);
		setCommandHandler(commandHandler);
	}
	
	@RequestMapping(method = RequestMethod.GET)
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
