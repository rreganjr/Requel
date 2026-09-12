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

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A null name must never reach a caller as a NullPointerException (issue #251).
 * <p>
 * {@code EditScenarioInput.name} was the only Edit*Input without {@code @NotBlank}, so a null name
 * passed validation, reached {@code EditScenarioCommandImpl}, and NPE'd inside
 * {@code findScenarioByProjectOrDomainAndName}, which trims it. Two layers answer that now, and
 * this covers both:
 * <ul>
 * <li>the gateway rejects it as INVALID_INPUT, naming the field, like every other entity already
 * did;</li>
 * <li>the commands guard the lookup and the setter, so the direct callers that bypass the DTO —
 * {@code EditUseCaseCommandImpl} builds an {@code EditScenarioCommand} itself, and the XML importer
 * and NLP analysis build commands without one — keep the current name instead of throwing.</li>
 * </ul>
 *
 * @author ron
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EditNullNameIT extends AbstractIntegrationTestCase {

	@Autowired
	private CommandGateway gateway;

	private String projectName;
	private String editorUsername;
	private Project project;
	private Long scenarioId;
	private Long goalId;
	private Long storyId;
	private String scenarioName;
	private String goalName;
	private String storyName;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");

		long ts = System.currentTimeMillis();
		projectName = "nullname-test-" + ts;
		EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
		projectCmd.setEditedBy(admin);
		projectCmd.setName(projectName);
		projectCmd.setText("null-name integration test project");
		projectCmd.setOrganizationName("NullNameOrg-" + ts);
		projectCmd = getCommandHandler().execute(projectCmd);
		project = projectCmd.getProject();

		editorUsername = "nullname-editor-" + ts;
		createUser(editorUsername);
		Set<String> perms = new HashSet<>();
		perms.addAll(keys(StakeholderPermissionType.Edit, Project.class, Goal.class, Actor.class,
				Story.class, UseCase.class, Scenario.class, Annotation.class));
		addUserStakeholder(project, editorUsername, perms);

		scenarioName = "nullname-scenario-" + ts;
		EditScenarioCommand scenarioCmd = getProjectCommandFactory().newEditScenarioCommand();
		scenarioCmd.setEditedBy(admin);
		scenarioCmd.setProjectOrDomain(project);
		scenarioCmd.setName(scenarioName);
		scenarioCmd.setText("the scenario whose name must survive a nameless edit");
		scenarioCmd.setScenarioTypeName(ScenarioType.Primary.name());
		scenarioCmd = getCommandHandler().execute(scenarioCmd);
		scenarioId = scenarioCmd.getScenario().getId();

		goalName = "nullname-goal-" + ts;
		EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
		goalCmd.setEditedBy(admin);
		goalCmd.setGoalContainer(project);
		goalCmd.setName(goalName);
		goalCmd.setText("the goal whose name must survive a nameless edit");
		goalCmd = getCommandHandler().execute(goalCmd);
		goalId = goalCmd.getGoal().getId();

		storyName = "nullname-story-" + ts;
		EditStoryCommand storyCmd = getProjectCommandFactory().newEditStoryCommand();
		storyCmd.setEditedBy(admin);
		storyCmd.setStoryContainer(project);
		storyCmd.setName(storyName);
		storyCmd.setText("the story whose name must survive a nameless edit");
		storyCmd.setStoryTypeName(StoryType.Success.name());
		storyCmd = getCommandHandler().execute(storyCmd);
		storyId = storyCmd.getStory().getId();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	// ---- the gateway: a field-level validation error, not an NPE -------------------------------

	/** The reported defect. This threw {@code Cannot invoke "String.trim()" because "name" is null}. */
	@Test
	void editScenarioByIdWithoutANameIsRejectedAsInvalidInput() {
		authenticate(editorUsername);

		GatewayException thrown = assertThrows(GatewayException.class,
				() -> gateway.execute(new GatewayRequest("EditScenario", Map.of(
						"projectName", projectName, "scenarioId", scenarioId,
						"text", "edited without supplying a name"))));

		assertEquals(GatewayException.Kind.INVALID_INPUT, thrown.getKind(),
				"a missing name is a caller error, not an execution failure: " + thrown);
	}

	/** Blank is the other half of @NotBlank, and was never covered. */
	@Test
	void editScenarioByIdWithABlankNameIsRejectedAsInvalidInput() {
		authenticate(editorUsername);

		GatewayException thrown = assertThrows(GatewayException.class,
				() -> gateway.execute(new GatewayRequest("EditScenario", Map.of(
						"projectName", projectName, "scenarioId", scenarioId,
						"name", "   ", "text", "edited with a blank name"))));

		assertEquals(GatewayException.Kind.INVALID_INPUT, thrown.getKind());
	}

	// ---- the commands: direct callers bypass the DTO, so they must not throw either -------------

	@Test
	void editingAScenarioCommandWithoutANameKeepsTheCurrentName() throws Exception {
		Scenario edited = editScenarioWithNullName("text changed, name left alone");

		assertEquals(scenarioName, edited.getName());
		assertEquals("text changed, name left alone", edited.getText());
	}

	@Test
	void editingAGoalCommandWithoutANameKeepsTheCurrentName() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setGoal(getProjectRepository().findGoalByProjectOrDomainAndName(project, goalName));
		cmd.setText("goal text changed, name left alone");
		cmd = getCommandHandler().execute(cmd);

		assertEquals(goalName, cmd.getGoal().getName());
		assertEquals("goal text changed, name left alone", cmd.getGoal().getText());
	}

	@Test
	void editingAStoryCommandWithoutANameKeepsTheCurrentName() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
		cmd.setEditedBy(admin);
		cmd.setStoryContainer(project);
		cmd.setStory(getProjectRepository().findStoryByProjectOrDomainAndName(project, storyName));
		cmd.setText("story text changed, name left alone");
		cmd.setStoryTypeName(StoryType.Success.name());
		cmd = getCommandHandler().execute(cmd);

		assertEquals(storyName, cmd.getStory().getName());
		assertEquals("story text changed, name left alone", cmd.getStory().getText());
	}

	/** Naming it again must still work — the guard must not swallow a real rename. */
	@Test
	void supplyingANameStillRenamesTheScenario() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		String renamed = scenarioName + "-renamed";
		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setScenario(getProjectRepository()
				.findScenarioByProjectOrDomainAndName(project, scenarioName));
		cmd.setName(renamed);
		cmd.setText("renamed on purpose");
		cmd.setScenarioTypeName(ScenarioType.Primary.name());
		cmd = getCommandHandler().execute(cmd);

		assertEquals(renamed, cmd.getScenario().getName());
		// put it back, so the fixture name still resolves for the other tests
		scenarioName = renamed;
	}

	// ---- helpers -------------------------------------------------------------------------------

	private Scenario editScenarioWithNullName(String text) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setScenario(getProjectRepository()
				.findScenarioByProjectOrDomainAndName(project, scenarioName));
		cmd.setText(text);
		cmd.setScenarioTypeName(ScenarioType.Primary.name());
		return getCommandHandler().execute(cmd).getScenario();
	}

	private void authenticate(String username) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, "x", List.of()));
	}

	private void createUser(String username) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(admin);
		cmd.setUsername(username);
		cmd.setPassword("pw");
		cmd.setRepassword("pw");
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("NullNameOrg");
		cmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(cmd);
	}

	private void addUserStakeholder(Project project, String username, Set<String> permissionKeys)
			throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserStakeholderCommand cmd = getProjectCommandFactory().newEditUserStakeholderCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setUsername(username);
		cmd.setStakeholderPermissions(permissionKeys);
		getCommandHandler().execute(cmd);
	}

	private static Set<String> keys(StakeholderPermissionType type, Class<?>... entityTypes) {
		return Arrays.stream(entityTypes)
				.map(c -> StakeholderPermissionImpl.generatePermissionKey(c, type))
				.collect(Collectors.toSet());
	}
}
