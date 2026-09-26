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
import com.rreganjr.repository.jpa.BeanValidationException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.IssueSeverity;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.IssueDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.service.gateway.InProcessQueryGateway;
import com.rreganjr.requel.service.query.AnnotationQueryController;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import java.util.Arrays;
import java.util.HashMap;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue severity through the real command gateway and the read paths (issue #271): what
 * {@code EditIssue} accepts and rejects, and that every issue list comes back in severity order
 * with no null severity.
 * <p>
 * One project is shared by every test, and the NLP assistants add their own (LOW) lexical issues
 * to each goal, so each test uses its own goal and checks order as "non-increasing rank" plus the
 * relative position of its own issues, never as an exact list.
 * <p>
 * {@link IssueSeverityMySqlIT} runs the same cases on MySQL with the Flyway schema.
 *
 * @author ron
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IssueSeverityIT extends AbstractIntegrationTestCase {

	@Autowired
	private CommandGateway gateway;

	@Autowired
	private InProcessQueryGateway queryGateway;

	@Autowired
	private AnnotationQueryController annotationQueryController;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private String projectName;
	private String editorUsername;
	private Project project;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");

		long ts = System.currentTimeMillis();
		projectName = "severity-test-" + ts;
		EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
		projectCmd.setEditedBy(admin);
		projectCmd.setName(projectName);
		projectCmd.setText("issue severity integration test project");
		projectCmd.setOrganizationName("SeverityTestOrg-" + ts);
		project = getCommandHandler().execute(projectCmd).getProject();

		editorUsername = "severity-editor-" + ts;
		createUser(editorUsername);
		Set<String> perms = new HashSet<>();
		perms.addAll(keys(StakeholderPermissionType.Edit, Project.class, Goal.class,
				Annotation.class));
		addUserStakeholder(project, editorUsername, perms);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	// ---- EditIssue ----------------------------------------------------------------------------

	@Test
	void editIssueAcceptsASeverityCaseInsensitivelyAndStoresItUpperCase() throws Exception {
		authenticate(editorUsername);
		Long goalId = createGoal("sev-accept");

		IssueDto dto = editIssue(goalId, null, "Admin console may be tenant-wide", null, "high");

		assertEquals("HIGH", dto.severity());
		assertEquals(IssueSeverity.HIGH, reload(dto.id()).getSeverity());
	}

	@Test
	void editIssueWithoutASeverityCreatesMedium() throws Exception {
		authenticate(editorUsername);
		Long goalId = createGoal("sev-default");

		IssueDto dto = editIssue(goalId, null, "No severity given", null, null);

		assertEquals("MEDIUM", dto.severity());
	}

	@Test
	void editIssueRejectsAnUnknownSeverityAsAFieldErrorAndWritesNothing() throws Exception {
		authenticate(editorUsername);
		Long goalId = createGoal("sev-reject");
		String text = "Should never be written";

		GatewayException e = assertThrows(GatewayException.class,
				() -> editIssue(goalId, null, text, null, "urgent"));

		assertEquals(GatewayException.Kind.INVALID_INPUT, e.getKind());
		BeanValidationException cause = assertInstanceOf(BeanValidationException.class,
				e.getCause());
		assertTrue(Arrays.asList(cause.getEntityPropertyNames()).contains("severity"),
				"expected a field error on severity, got "
						+ Arrays.toString(cause.getEntityPropertyNames()));
		assertFalse(issuesOn(goalId).stream().anyMatch(i -> text.equals(i.text())),
				"a rejected EditIssue must not create the issue");
	}

	@Test
	void editIssueUpdateThatLeavesOutSeverityAndMustBeResolvedKeepsBoth() throws Exception {
		authenticate(editorUsername);
		Long goalId = createGoal("sev-partial");
		IssueDto created = editIssue(goalId, null, "Original wording", true, "HIGH");

		IssueDto updated = editIssue(goalId, created.id(), "Reworded", null, null);

		assertEquals("Reworded", updated.text());
		assertEquals("HIGH", updated.severity());
		assertTrue(updated.mustBeResolved(), "a text-only update must not clear mustBeResolved");
	}

	// ---- read paths ---------------------------------------------------------------------------

	@Test
	void openIssuesAndProjectContextAreInSeverityOrderWithNoNullSeverity() throws Exception {
		authenticate(editorUsername);
		Long goalId = createGoal("sev-order");
		String low = "Low-" + System.nanoTime();
		String medium = "Medium-" + System.nanoTime();
		String high = "High-" + System.nanoTime();
		// Created lowest first, so creation order alone would list them backwards.
		editIssue(goalId, null, low, null, "LOW");
		editIssue(goalId, null, medium, null, "MEDIUM");
		editIssue(goalId, null, high, null, "HIGH");

		assertInSeverityOrder(queryGateway.getOpenIssues(projectName), low, medium, high);

		@SuppressWarnings("unchecked")
		List<OpenIssueDto> fromContext = (List<OpenIssueDto>) queryGateway
				.getProjectContext(projectName).get("openIssues");
		assertInSeverityOrder(fromContext, low, medium, high);
	}

	@Test
	void entityAnnotationsListIssuesBySeverityThenId() throws Exception {
		authenticate(editorUsername);
		Long goalId = createGoal("sev-entity");
		Long first = editIssue(goalId, null, "Medium, created first", null, "MEDIUM").id();
		Long second = editIssue(goalId, null, "High, created second", null, "HIGH").id();

		List<IssueDto> issues = issuesOn(goalId);

		assertTrue(issues.stream().allMatch(i -> i.severity() != null), "null severity: " + issues);
		List<Integer> ranks = issues.stream().map(i -> rank(i.severity())).toList();
		assertEquals(ranks.stream().sorted((a, b) -> b - a).toList(), ranks,
				"issues not in severity order: " + issues);
		List<Long> ids = issues.stream().map(IssueDto::id).toList();
		assertTrue(ids.indexOf(second) < ids.indexOf(first),
				"the HIGH issue must list above the earlier MEDIUM one: " + issues);
	}

	// ---- helpers ------------------------------------------------------------------------------

	private void assertInSeverityOrder(List<OpenIssueDto> issues, String low, String medium,
			String high) {
		assertNotNull(issues);
		assertTrue(issues.stream().allMatch(i -> i.severity() != null), "null severity: " + issues);
		List<Integer> ranks = issues.stream().map(i -> rank(i.severity())).toList();
		assertEquals(ranks.stream().sorted((a, b) -> b - a).toList(), ranks,
				"open issues not in severity order: " + issues);
		List<String> texts = issues.stream().map(OpenIssueDto::issueText).toList();
		int h = texts.indexOf(high);
		int m = texts.indexOf(medium);
		int l = texts.indexOf(low);
		assertTrue(h >= 0 && m >= 0 && l >= 0, "missing one of the fixture issues: " + texts);
		assertTrue(h < m && m < l, "expected HIGH, MEDIUM, LOW: " + texts);
	}

	private static int rank(String severity) {
		return IssueSeverity.parse(severity).map(IssueSeverity::rank).orElse(0);
	}

	private IssueDto editIssue(Long goalId, Long issueId, String text, Boolean mustBeResolved,
			String severity) throws Exception {
		Map<String, Object> input = new HashMap<>();
		input.put("projectName", projectName);
		input.put("entityType", "Goal");
		input.put("entityId", goalId);
		input.put("text", text);
		if (issueId != null) {
			input.put("issueId", issueId);
		}
		if (mustBeResolved != null) {
			input.put("mustBeResolved", mustBeResolved);
		}
		if (severity != null) {
			input.put("severity", severity);
		}
		GatewayResult result = gateway.execute(new GatewayRequest("EditIssue", input));
		return (IssueDto) result.result();
	}

	/**
	 * The controller walks a lazy {@code annotations} collection and is called in-process, without
	 * the session a real request has, so the read runs in its own transaction. Not the whole test:
	 * the rejected-write case must see what the gateway actually committed.
	 */
	private List<IssueDto> issuesOn(Long goalId) {
		TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
		readOnly.setReadOnly(true);
		AnnotationsDto annotations = readOnly.execute(status -> annotationQueryController
				.getAnnotations(projectName, "Goal", goalId).getBody());
		assertNotNull(annotations);
		return annotations.issues();
	}

	private Issue reload(Long issueId) {
		return getAnnotationRepository().findById(Issue.class, issueId);
	}

	private Long createGoal(String label) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
		goalCmd.setEditedBy(admin);
		goalCmd.setGoalContainer(project);
		goalCmd.setName(label + "-" + System.nanoTime());
		goalCmd.setText("the goal these issues hang off");
		return getCommandHandler().execute(goalCmd).getGoal().getId();
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
		cmd.setOrganizationName("SeverityTestOrg");
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
