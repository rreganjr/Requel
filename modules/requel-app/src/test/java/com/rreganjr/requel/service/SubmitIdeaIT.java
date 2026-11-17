/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.Idea;
import com.rreganjr.requel.project.IdeaStore;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.DeleteProjectCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.SubmitIdeaCommand;
import com.rreganjr.requel.user.User;

/**
 * The SubmitIdea command through the real command chain on H2: it saves an idea on an existing
 * project, combines typed and file text, refuses a too-short idea, and the project's ideas go
 * when the project is deleted.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SubmitIdeaIT extends AbstractIntegrationTestCase {

	@Autowired
	private IdeaStore ideaStore;

	private User admin;
	private Project project;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		admin = getUserRepository().findUserByUsername("admin");
		project = newProject("idea-test-" + System.nanoTime());
	}

	private Project newProject(String name) throws Exception {
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(name);
		cmd.setText("idea integration test project");
		cmd.setOrganizationName("IdeaTestOrg-" + name);
		return getCommandHandler().execute(cmd).getProject();
	}

	private SubmitIdeaCommand submit(Project target, String text, String fileText, String fileName)
			throws Exception {
		SubmitIdeaCommand cmd = getProjectCommandFactory().newSubmitIdeaCommand();
		cmd.setEditedBy(admin);
		cmd.setProject(target);
		cmd.setText(text);
		if (fileText != null) {
			cmd.setFileText(fileText, fileName);
		}
		return getCommandHandler().execute(cmd);
	}

	@Test
	public void typedIdeaIsSavedAsSubmitted() throws Exception {
		Project p = newProject("idea-typed-" + System.nanoTime());
		Idea idea = submit(p, "  Let users export a report as PDF  ", null, null).getIdea();

		assertTrue(idea.getId() != null);
		assertEquals("Let users export a report as PDF", idea.getText());
		assertEquals(Idea.Status.SUBMITTED, idea.getStatus());
		assertNull(idea.getSourceFileName());
		assertEquals(p.getId(), idea.getProjectId());
		assertEquals(1, ideaStore.list(p.getId()).size());
	}

	@Test
	public void fileTextFollowsTypedTextAndNameIsKept() throws Exception {
		Project p = newProject("idea-file-" + System.nanoTime());
		Idea idea = submit(p, "Typed part of the idea", "Text from the file\n", "notes.txt").getIdea();

		assertEquals("Typed part of the idea\n\nText from the file", idea.getText());
		assertEquals("notes.txt", idea.getSourceFileName());
	}

	@Test
	public void fileAloneIsEnough() throws Exception {
		Project p = newProject("idea-fileonly-" + System.nanoTime());
		Idea idea = submit(p, null, "Short", "a.txt").getIdea();

		assertEquals("Short", idea.getText());
	}

	@Test
	public void tooShortTypedIdeaIsRefusedAndNothingIsSaved() throws Exception {
		Project p = newProject("idea-short-" + System.nanoTime());

		assertThrows(IllegalArgumentException.class, () -> submit(p, "too short", null, null));
		assertThrows(IllegalArgumentException.class, () -> submit(p, "   ", null, null));
		assertTrue(ideaStore.list(p.getId()).isEmpty());
	}

	@Test
	public void ideasAreListedNewestFirstPerProject() throws Exception {
		Project p = newProject("idea-order-" + System.nanoTime());
		submit(p, "first idea in the list", null, null);
		submit(p, "second idea in the list", null, null);
		List<Idea> ideas = ideaStore.list(p.getId());

		assertEquals(2, ideas.size());
		assertEquals("second idea in the list", ideas.get(0).getText());
		assertTrue(ideaStore.list(project.getId()).isEmpty());
	}

	@Test
	public void deletingTheProjectDeletesItsIdeas() throws Exception {
		Project p = newProject("idea-delete-" + System.nanoTime());
		submit(p, "an idea that must not outlive its project", null, null);
		assertEquals(1, ideaStore.list(p.getId()).size());

		DeleteProjectCommand delete = getProjectCommandFactory().newDeleteProjectCommand();
		delete.setEditedBy(admin);
		delete.setProject(p);
		getCommandHandler().execute(delete);

		assertTrue(ideaStore.list(p.getId()).isEmpty());
	}
}
