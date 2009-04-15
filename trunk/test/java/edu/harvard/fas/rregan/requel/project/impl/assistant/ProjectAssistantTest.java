/*
 * $Id: ProjectAssistantTest.java,v 1.11 2009/03/27 07:16:07 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.impl.assistant;

import java.io.InputStream;

import edu.harvard.fas.rregan.AbstractIntegrationTestCase;
import edu.harvard.fas.rregan.requel.project.command.ImportProjectCommand;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
public class ProjectAssistantTest extends AbstractIntegrationTestCase {

	public static final String testProjectXmlFile = "resources/xml/testProject.xml";

	/**
	 * TODO: This is a duplicate of ImportProjectCommandTest.
	 * 
	 * @throws Exception
	 */
	public void testProjectAssistant() throws Exception {
		long uniqueifier = System.currentTimeMillis();
		String projectName = "Test Project " + uniqueifier;
		InputStream projectXmlInputStream = getClass().getClassLoader().getResourceAsStream(
				testProjectXmlFile);
		ImportProjectCommand command = getProjectCommandFactory().newImportProjectCommand();
		User creator = getUserRepository().findUserByUsername("project");
		User assistantUser = getUserRepository().findUserByUsername("assistant");
		command.setAnalysisEnabled(false);
		command.setEditedBy(creator);
		command.setName(projectName);
		command.setInputStream(projectXmlInputStream);
		command = getCommandHandler().execute(command);

		LexicalAssistant lexicalAssistant = new LexicalAssistant(getCommandHandler(),
				getProjectCommandFactory(), getAnnotationCommandFactory(),
				getAnnotationRepository(), getProjectRepository(), getDictionaryRepository(),
				getNlpProcessorFactory());

		ProjectAssistant projectAssistant = new ProjectAssistant(lexicalAssistant, assistantUser);
		projectAssistant.analyze(command.getProject());
	}
}
