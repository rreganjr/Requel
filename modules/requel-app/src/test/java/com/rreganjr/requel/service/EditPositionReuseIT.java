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
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.service.api.dto.IssueDto;
import com.rreganjr.requel.service.api.dto.PositionDto;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A position whose text already exists in the grouping object is reused and linked to the new
 * issue, rather than throwing (issue #281).
 * <p>
 * {@code EditPositionCommandImpl.execute} looks for such a position and — per its own comment, and
 * per {@code PositionImpl.issues} being a {@code @ManyToMany} over the {@code position_issue} join
 * table — is meant to reference it from the issue at hand. It discarded the result of that lookup,
 * so on the found path {@code position} stayed null and the caller got
 * {@code Cannot invoke "PositionImpl.getIssues()" because "position" is null}. Only the not-found
 * path was ever exercised, because every test and every ordinary interaction used fresh text.
 *
 * @author ron
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EditPositionReuseIT extends AbstractIntegrationTestCase {

	@Autowired
	private CommandGateway gateway;

	private String projectName;
	private String editorUsername;
	private Long goalId;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");

		long ts = System.currentTimeMillis();
		projectName = "reuse-test-" + ts;
		EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
		projectCmd.setEditedBy(admin);
		projectCmd.setName(projectName);
		projectCmd.setText("position reuse integration test project");
		projectCmd.setOrganizationName("ReuseTestOrg-" + ts);
		projectCmd = getCommandHandler().execute(projectCmd);
		Project project = projectCmd.getProject();

		editorUsername = "reuse-editor-" + ts;
		createUser(editorUsername);
		Set<String> perms = new HashSet<>();
		perms.addAll(keys(StakeholderPermissionType.Edit, Project.class, Goal.class,
				Annotation.class));
		perms.addAll(keys(StakeholderPermissionType.Delete, Goal.class, Annotation.class));
		addUserStakeholder(project, editorUsername, perms);

		EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
		goalCmd.setEditedBy(admin);
		goalCmd.setGoalContainer(project);
		goalCmd.setName("reuse-goal-" + ts);
		goalCmd.setText("the goal these issues hang off");
		goalCmd = getCommandHandler().execute(goalCmd);
		goalId = goalCmd.getGoal().getId();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	/**
	 * The reported defect: the second EditPosition used to throw a NullPointerException rather than
	 * link the existing position.
	 */
	@Test
	void aPositionWhoseTextAlreadyExistsIsReusedByTheSecondIssue() throws Exception {
		authenticate(editorUsername);
		String sharedText = "ship it behind a flag";
		Long firstIssueId = createIssue("the first issue this answer applies to");
		Long secondIssueId = createIssue("a second, separate issue with the same answer");

		PositionDto first = editPosition(firstIssueId, sharedText);
		PositionDto second = editPosition(secondIssueId, sharedText);

		assertNotNull(second, "the second EditPosition returned nothing");
		assertEquals(first.id(), second.id(),
				"matching text in one grouping object must reuse the position, not duplicate it");

		// Both issues now reference that one position — the @ManyToMany the command is written for.
		assertTrue(positionIdsOn(firstIssueId).contains(first.id()),
				"the first issue lost its position");
		assertTrue(positionIdsOn(secondIssueId).contains(first.id()),
				"the second issue did not pick up the existing position");
	}

	/** Distinct text still makes a distinct position; reuse must not collapse real alternatives. */
	@Test
	void distinctTextStillCreatesADistinctPosition() throws Exception {
		authenticate(editorUsername);
		Long issueId = createIssue("an issue with two competing answers");

		PositionDto first = editPosition(issueId, "roll it back");
		PositionDto second = editPosition(issueId, "roll it forward");

		assertEquals(2, positionIdsOn(issueId).size(), "expected two distinct positions");
		assertTrue(!first.id().equals(second.id()), "distinct text must not reuse a position");
	}

	// ---- helpers -------------------------------------------------------------------------------

	private PositionDto editPosition(Long issueId, String text) throws Exception {
		GatewayResult result = gateway.execute(new GatewayRequest("EditPosition",
				Map.of("projectName", projectName, "issueId", issueId, "text", text)));
		return (PositionDto) result.result();
	}

	private Long createIssue(String text) throws Exception {
		GatewayResult result = gateway.execute(new GatewayRequest("EditIssue",
				Map.of("projectName", projectName, "entityType", "Goal", "entityId", goalId,
						"text", text)));
		return ((IssueDto) result.result()).id();
	}

	/** Re-reads the issue through the gateway, so the assertion sees persisted state. */
	private Set<Long> positionIdsOn(Long issueId) throws Exception {
		GatewayResult result = gateway.execute(new GatewayRequest("EditIssue",
				Map.of("projectName", projectName, "entityType", "Goal", "entityId", goalId,
						"issueId", issueId, "text", "re-read " + issueId)));
		return ((IssueDto) result.result()).positions().stream()
				.map(PositionDto::id)
				.collect(Collectors.toSet());
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
		cmd.setOrganizationName("ReuseTestOrg");
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
