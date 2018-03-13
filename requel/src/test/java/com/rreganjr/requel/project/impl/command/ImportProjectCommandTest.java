/*
 * $Id: ImportProjectCommandTest.java,v 1.4 2009/03/27 07:16:09 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package com.rreganjr.requel.project.impl.command;

import java.io.InputStream;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.impl.assistant.LexicalAssistant;
import com.rreganjr.requel.project.impl.assistant.ProjectAssistant;
import com.rreganjr.requel.user.User;

/**
 * @author ron
 */
public class ImportProjectCommandTest extends AbstractIntegrationTestCase {

	public static final String testProjectXmlFile = "xml/Requel.xml";

	/**
	 * TODO: This is a duplicate of ProjectAssistantTest. Import the test
	 * project and run the project assistant on it. This only tests that no
	 * unexpected exceptions occur.
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
		command.setAnalysisEnabled(false);
		command.setEditedBy(creator);
		command.setName(projectName);
		command.setInputStream(projectXmlInputStream);
		command = getCommandHandler().execute(command);

		// TODO: this all happens in one tranaction because the test is wrapped
		// in a transaction. That transaction should be terminated so this
		// behaves like the assistance in a running application.

		final User assistantUser = getUserRepository().findUserByUsername("assistant");
		final LexicalAssistant lexicalAssistant = new LexicalAssistant(getCommandHandler(),
				getProjectCommandFactory(), getAnnotationCommandFactory(),
				getAnnotationRepository(), getProjectRepository(), getDictionaryRepository(),
				getNlpProcessorFactory());
		final ProjectAssistant projectAssistant = new ProjectAssistant(lexicalAssistant,
				assistantUser);
		projectAssistant.analyze(command.getProject());

	}
}
