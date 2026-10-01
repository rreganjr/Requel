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
package com.rreganjr.requel.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;
import com.rreganjr.requel.project.command.DeleteProjectCommand;
import com.rreganjr.requel.project.command.EditProjectAssistantSettingCommand;

/**
 * Issue #268: a project can switch each lexical assistant off; the others keep running. Driven
 * through the real SPI path, like {@link LexicalFindingScopeTest}.
 */
public class ProjectAssistantSettingsIT extends AbstractLexicalAssistantTest {

	@Autowired
	private ProjectAssistantSettingsStore settingsStore;

	@Autowired
	private SwitchableAssistantCatalog catalog;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	public void theFourLexicalAssistantsAreSwitchable() {
		assertEquals(Set.of("legacy-lexical", "legacy-lexical-vague-word",
				"legacy-lexical-glossary-term", "legacy-lexical-complexity"),
				Set.copyOf(catalog.switchableAssistants().stream()
						.map(SwitchableAssistantCatalog.SwitchableAssistant::assistantId).toList()));
		assertTrue(catalog.switchableAssistants().stream()
				.noneMatch(a -> a.displayName().equals(a.assistantId())),
				"each has a display name");
	}

	@Test
	public void switchingSpellingOffStopsSpellingFindingsAndLeavesTheOthersRunning()
			throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Settings");
		setEnabled(project, user, SPELLING, false);

		Goal goal = newGoal(project, user, "groal intake " + stamp(),
				"The clerk should record it.");

		assertTrue(spellingIssues(goal.getId(), "groal").isEmpty(),
				"spelling is off: no 'groal' issue");
		assertFalse(vagueIssues(goal.getId(), "should").isEmpty(),
				"the vague-word check still runs");

		setEnabled(project, user, SPELLING, true);
		editGoal(goal, user, freshGoalName(goal.getId()), "The clerk should record it twice.");

		assertEquals(1, spellingIssues(goal.getId(), "groal").size(),
				"switched back on, spelling raises 'groal'");
	}

	@Test
	public void switchingAnAssistantOffLeavesItsExistingIssues() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("SettingsKeep");
		Goal goal = newGoal(project, user, "groal intake " + stamp(), "The clerk records it.");
		assertEquals(1, spellingIssues(goal.getId(), "groal").size());

		setEnabled(project, user, SPELLING, false);
		editGoal(goal, user, freshGoalName(goal.getId()), "The clerk records it twice.");

		assertEquals(1, spellingIssues(goal.getId(), "groal").size(),
				"a switched-off assistant doesn't clear what it raised");
	}

	/**
	 * Issue #260: each lexical assistant writes as its own {@code assistant-<id>} user, not the
	 * legacy {@code assistant}, and that user is still machine-authored. The AI side is in
	 * {@code AssistantDefinitionsIT}.
	 */
	@Test
	public void aLexicalFindingIsWrittenByItsAssistantsOwnIdentity() throws Exception {
		ensureDictionaryLoaded();
		Goal goal = newGoal(newProject("SettingsAuthor"), projectUser(),
				"groal intake " + stamp(), "The clerk records it.");
		Issue issue = spellingIssue(goal.getId(), "groal");
		assertNotNull(issue, "spelling raises 'groal'");

		String author = jdbcTemplate.queryForObject("SELECT u.username FROM annotations a"
				+ " JOIN users u ON a.created_by_id = u.id WHERE a.id = ?", String.class,
				issue.getId());
		assertEquals("assistant-" + SPELLING, author);
		assertTrue(User.isAssistant(getUserRepository().findUserByUsername(author)));
	}

	@Test
	public void onlyASwitchableAssistantCanBeSet() throws Exception {
		User user = projectUser();
		Project project = newProject("SettingsUnknown");

		assertThrows(Exception.class, () -> setEnabled(project, user, "ai-requirements-review",
				false));
		assertThrows(Exception.class, () -> setEnabled(project, user, "no-such-assistant",
				false));
		assertTrue(settingsStore.disabledAssistants(project.getId()).isEmpty());
	}

	@Test
	public void deletingTheProjectDeletesItsSettings() throws Exception {
		User user = projectUser();
		Project project = newProject("SettingsDelete");
		Project other = newProject("SettingsOther");
		setEnabled(project, user, SPELLING, false);
		setEnabled(other, user, SPELLING, false);

		DeleteProjectCommand delete = getProjectCommandFactory().newDeleteProjectCommand();
		delete.setEditedBy(user);
		delete.setProject(getProjectRepository().get(project));
		getCommandHandler().execute(delete);

		assertTrue(settingsStore.disabledAssistants(project.getId()).isEmpty());
		assertEquals(Set.of(SPELLING), settingsStore.disabledAssistants(other.getId()));
	}

	// ---- fixtures -------------------------------------------------------------------------

	private void setEnabled(Project project, User user, String assistantId, boolean enabled)
			throws Exception {
		EditProjectAssistantSettingCommand command = getProjectCommandFactory()
				.newEditProjectAssistantSettingCommand();
		command.setEditedBy(user);
		command.setProject(project);
		command.setAssistantId(assistantId);
		command.setEnabled(enabled);
		getCommandHandler().execute(command);
	}

	private List<com.rreganjr.requel.annotation.impl.LexicalIssue> vagueIssues(Long goalId,
			String word) {
		return lexicalIssues(goalId).stream()
				.filter(issue -> word.equalsIgnoreCase(issue.getWord()) && issue.getText() != null
						&& issue.getText().contains("is vague"))
				.toList();
	}
}
