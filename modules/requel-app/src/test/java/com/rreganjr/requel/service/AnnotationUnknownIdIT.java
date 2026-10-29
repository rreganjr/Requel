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
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.service.api.dto.IssueDto;
import com.rreganjr.requel.service.api.dto.NoteDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * #342: the annotation binders looked child ids up with {@code entityManager.find} and passed a
 * miss on as null, so an edit with an unknown id created a new annotation and a delete failed with
 * a NullPointerException. Both are now refused as NOT_FOUND (#345; INVALID_INPUT before it). Also covers {@code findNote}, the
 * {@code EditNote} reuse lookup, which matched with LIKE and failed on duplicate text.
 *
 * <p>Runs through the in-process gateway, the MCP path, as a stakeholder with Annotation[Edit] and
 * Annotation[Delete]. Cross-project ids are covered in {@code AuthorizationIT}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AnnotationUnknownIdIT extends AbstractIntegrationTestCase {

	/** No row has this id in any table. */
	private static final Long UNKNOWN = Long.MAX_VALUE;

	@Autowired
	private CommandGateway gateway;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String projectName;
	private String editorUsername;
	private Long goalId;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();

		projectName = "unknown-id-test-" + ts;
		EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
		projectCmd.setEditedBy(admin);
		projectCmd.setName(projectName);
		projectCmd.setText("annotation unknown-id integration test project");
		projectCmd.setOrganizationName("UnknownIdTestOrg-" + ts);
		Project project = getCommandHandler().execute(projectCmd).getProject();

		editorUsername = "unknown-id-editor-" + ts;
		createUser(editorUsername);
		Set<String> perms = new HashSet<>();
		perms.addAll(keys(StakeholderPermissionType.Edit, Project.class, Goal.class, Annotation.class));
		perms.addAll(keys(StakeholderPermissionType.Delete, Goal.class, Annotation.class));
		addUserStakeholder(project, editorUsername, perms);

		EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
		goalCmd.setEditedBy(admin);
		goalCmd.setGoalContainer(project);
		goalCmd.setName("unknown-id-goal-" + ts);
		goalCmd.setText("the goal these annotations hang off");
		goalId = getCommandHandler().execute(goalCmd).getGoal().getId();
	}

	@BeforeEach
	void signIn() {
		authenticate(editorUsername);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void anEditWithAnUnknownChildIdIsRefusedAndCreatesNothing() throws Exception {
		Long issueId = createIssue("unknown-child issue " + System.nanoTime());
		Long positionId = createPosition(issueId, "unknown-child position " + System.nanoTime());
		int annotations = rowCount("SELECT COUNT(*) FROM annotations");
		int positions = rowCount("SELECT COUNT(*) FROM positions");
		int arguments = rowCount("SELECT COUNT(*) FROM arguments");

		assertNotFound("EditNote", Map.of("projectName", projectName, "entityType", "Goal",
				"entityId", goalId, "noteId", UNKNOWN, "text", "a note"), "Note not found");
		assertNotFound("EditIssue", Map.of("projectName", projectName, "entityType", "Goal",
				"entityId", goalId, "issueId", UNKNOWN, "text", "an issue"), "Issue not found");
		assertNotFound("EditPosition", Map.of("projectName", projectName, "issueId", issueId,
				"positionId", UNKNOWN, "text", "a position"), "Position not found");
		assertNotFound("EditArgument", Map.of("projectName", projectName, "positionId", positionId,
				"argumentId", UNKNOWN, "text", "an argument", "supportLevel", "For"),
				"Argument not found");

		assertEquals(annotations, rowCount("SELECT COUNT(*) FROM annotations"));
		assertEquals(positions, rowCount("SELECT COUNT(*) FROM positions"));
		assertEquals(arguments, rowCount("SELECT COUNT(*) FROM arguments"));
	}

	@Test
	void anEditUnderAnUnknownParentIsRefusedAndCreatesNothing() throws Exception {
		int positions = rowCount("SELECT COUNT(*) FROM positions");
		int arguments = rowCount("SELECT COUNT(*) FROM arguments");

		assertNotFound("EditPosition", Map.of("projectName", projectName, "issueId", UNKNOWN,
				"text", "an orphan position " + System.nanoTime()), "Issue not found");
		assertNotFound("EditArgument", Map.of("projectName", projectName, "positionId", UNKNOWN,
				"text", "an orphan argument", "supportLevel", "For"), "Position not found");

		assertEquals(positions, rowCount("SELECT COUNT(*) FROM positions"),
				"EditPosition used to persist a position attached to no issue");
		assertEquals(arguments, rowCount("SELECT COUNT(*) FROM arguments"));
	}

	/** These used to fail with a NullPointerException, reported as EXECUTION_ERROR. */
	@Test
	void aDeleteWithAnUnknownIdIsRefused() {
		assertNotFound("DeleteNote", Map.of("projectName", projectName, "noteId", UNKNOWN),
				"Note not found");
		assertNotFound("DeleteIssue", Map.of("projectName", projectName, "issueId", UNKNOWN),
				"Issue not found");
		assertNotFound("DeletePosition", Map.of("projectName", projectName, "positionId", UNKNOWN),
				"Position not found");
		assertNotFound("DeleteArgument", Map.of("projectName", projectName, "argumentId", UNKNOWN),
				"Argument not found");
	}

	/** With LIKE, "a_b" matched an existing "axb" note, so EditNote attached that one instead. */
	@Test
	void anUnderscoreInNoteTextIsNotAWildcard() throws Exception {
		long ts = System.nanoTime();
		Long axb = editNote("note " + ts + " axb").id();
		Long aUnderscoreB = editNote("note " + ts + " a_b").id();

		assertNotEquals(axb, aUnderscoreB, "a_b must create its own note, not reuse axb");
	}

	/** With getSingleResult, two notes with the same text made the reusing EditNote fail. */
	@Test
	void duplicateNoteTextReusesTheLowestId() throws Exception {
		long ts = System.nanoTime();
		String text = "duplicate note " + ts;
		Long first = editNote(text).id();
		Long second = editNote("will become a duplicate " + ts).id();
		assertTrue(first < second);
		jdbcTemplate.update("UPDATE annotations SET text = ? WHERE id = ?", text, second);

		assertEquals(first, editNote(text).id());
	}

	// ---- helpers -------------------------------------------------------------------------------

	private void assertNotFound(String commandType, Map<String, Object> input, String message) {
		GatewayException ex = assertThrows(GatewayException.class,
				() -> gateway.execute(new GatewayRequest(commandType, input)), commandType);
		assertEquals(GatewayException.Kind.NOT_FOUND, ex.getKind(), commandType);
		assertTrue(String.valueOf(ex.getMessage()).contains(message),
				commandType + ": " + ex.getMessage());
	}

	private NoteDto editNote(String text) throws Exception {
		return (NoteDto) gateway.execute(new GatewayRequest("EditNote",
				Map.of("projectName", projectName, "entityType", "Goal", "entityId", goalId,
						"text", text))).result();
	}

	private Long createIssue(String text) throws Exception {
		return ((IssueDto) gateway.execute(new GatewayRequest("EditIssue",
				Map.of("projectName", projectName, "entityType", "Goal", "entityId", goalId,
						"text", text))).result()).id();
	}

	private Long createPosition(Long issueId, String text) throws Exception {
		return ((PositionDto) gateway.execute(new GatewayRequest("EditPosition",
				Map.of("projectName", projectName, "issueId", issueId, "text", text))).result()).id();
	}

	private int rowCount(String sql, Object... args) {
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
		return count == null ? 0 : count;
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
		cmd.setOrganizationName("UnknownIdTestOrg");
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

	@SafeVarargs
	private static Set<String> keys(StakeholderPermissionType type, Class<?>... entityTypes) {
		return Arrays.stream(entityTypes)
				.map(c -> StakeholderPermissionImpl.generatePermissionKey(c, type))
				.collect(Collectors.toSet());
	}
}
