/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl.assistant;

import java.io.InputStream;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.platform.identity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

/**
 * @author ron
 */
@Disabled("Relies on legacy JAXB importer; superseded by streaming importer tests.")
public class ProjectAssistantTest extends AbstractIntegrationTestCase {

	public static final String testProjectXmlFile = "xml/testProject.xml";

	/**
	 * TODO: This is a duplicate of ImportProjectCommandTest.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProjectAssistant() throws Exception {
		long uniqueifier = System.currentTimeMillis();
		String projectName = "Test Project " + uniqueifier;
		ensureDictionaryLoaded();
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
