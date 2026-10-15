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
package com.rreganjr.requel.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.project.command.AddGlossaryTermRefererCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.validator.EntityValidationException;

/**
 * What the background assistant may do on a project created through the UI (issue #302).
 *
 * <p>
 * Before #302 {@code EditProjectCommandImpl.createProject()} gave the assistant a stakeholder
 * row with no permissions at all, while the import path gave it every permission, so the same
 * project graph behaved differently depending on how it arrived and every assistant annotation
 * on a UI-created project was refused. These tests pin both halves: the assistant can write its
 * findings, and it cannot do anything else.
 *
 * @author ron
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AssistantPermissionsIT extends AbstractIntegrationTestCase {

	private static final String ANNOTATION_EDIT_KEY =
			StakeholderPermissionImpl.generatePermissionKey(Annotation.class,
					StakeholderPermissionType.Edit);

	private static final String ANNOTATION_DELETE_KEY =
			StakeholderPermissionImpl.generatePermissionKey(Annotation.class,
					StakeholderPermissionType.Delete);

	@BeforeAll
	void setUp() throws Exception {
		initializeBaselineData();
	}

	@Test
	void createProjectGrantsTheAssistantExactlyTheAssistantSet() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		User assistant = getUserRepository().findUserByUsername("assistant");
		Project project = createProject(admin, "assistant-perms-create-" + System.currentTimeMillis());

		UserStakeholder stakeholder = getProjectRepository()
				.findStakeholderByProjectOrDomainAndUser(getProjectRepository().get(project),
						assistant);

		assertNotNull(stakeholder, "creation must give the assistant a stakeholder row");
		assertEquals(Set.of(ANNOTATION_EDIT_KEY, ANNOTATION_DELETE_KEY), permissionKeys(stakeholder),
				"the assistant holds exactly the set findAssistantStakeholderPermissions defines");
	}

	/**
	 * The failing case from the ticket: before the fix this threw
	 * {@code AuthorizationException} because the assistant's row carried no permissions.
	 */
	@Test
	void theAssistantCanAnnotateAProjectCreatedThroughTheUi() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		User assistant = getUserRepository().findUserByUsername("assistant");
		Project project = createProject(admin, "assistant-perms-write-" + System.currentTimeMillis());
		Goal goal = createGoal(admin, project, "assistant-perms-goal-" + System.currentTimeMillis());

		EditIssueCommand cmd = getAnnotationCommandFactory().newEditIssueCommand();
		cmd.setEditedBy(assistant);
		cmd.setAnnotatable(goal);
		cmd.setGroupingObject(getProjectRepository().get(project));
		cmd.setText("the assistant has something to say about this goal");
		cmd = getCommandHandler().execute(cmd);

		assertNotNull(cmd.getIssue(), "the assistant's issue must be persisted");
		assertEquals(assistant.getUsername(), cmd.getIssue().getCreatedBy().getUsername(),
				"an assistant finding is authored by the assistant");
	}

	/**
	 * The other half of "the minimum it needs": the assistant must not inherit the creator's
	 * matrix. Editing the project is the cheapest thing to prove it with - it needs
	 * {@code Project[Edit]}, which is not in the assistant's set.
	 */
	@Test
	void theAssistantCannotEditTheProjectItself() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		User assistant = getUserRepository().findUserByUsername("assistant");
		Project project = createProject(admin, "assistant-perms-deny-" + System.currentTimeMillis());

		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(assistant);
		cmd.setProject(getProjectRepository().get(project));
		cmd.setName(project.getName());
		cmd.setText("the assistant should not be able to rewrite the project");

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(cmd));
	}

	/**
	 * The glossary-term back-reference an analysis pass records is annotation bookkeeping, not a
	 * glossary edit, so it goes through AddGlossaryTermRefererCommand and the assistant can make
	 * it with Annotation[Edit] alone — it holds no GlossaryTerm[Edit] (#302).
	 */
	@Test
	void theAssistantCanRecordAGlossaryTermReferer() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		User assistant = getUserRepository().findUserByUsername("assistant");
		Project project = createProject(admin, "assistant-perms-referer-" + System.currentTimeMillis());
		Goal goal = createGoal(admin, project, "assistant-perms-referer-goal-" + System.currentTimeMillis());
		GlossaryTerm term = createGlossaryTerm(admin, project,
				"assistantterm" + System.currentTimeMillis());

		AddGlossaryTermRefererCommand cmd = getProjectCommandFactory()
				.newAddGlossaryTermRefererCommand();
		cmd.setEditedBy(assistant);
		cmd.setGlossaryTerm(term);
		cmd.setReferer(goal);
		cmd = getCommandHandler().execute(cmd);

		assertTrue(cmd.getGlossaryTerm().getReferers().contains(goal),
				"the analyzed entity is recorded as a referer of the term");
		assertTrue(getProjectRepository().get(goal).getGlossaryTerms().contains(term),
				"and the association is set on both sides");
	}

	@Test
	void aNonStakeholderCannotRecordAGlossaryTermReferer() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		Project project = createProject(admin, "assistant-perms-referer-deny-" + ts);
		Goal goal = createGoal(admin, project, "assistant-perms-deny-goal-" + ts);
		GlossaryTerm term = createGlossaryTerm(admin, project, "denyterm" + ts);
		User outsider = createUser(admin, "assistant-perms-outsider-" + ts);

		AddGlossaryTermRefererCommand cmd = getProjectCommandFactory()
				.newAddGlossaryTermRefererCommand();
		cmd.setEditedBy(outsider);
		cmd.setGlossaryTerm(term);
		cmd.setReferer(goal);

		assertThrows(AuthorizationException.class, () -> getCommandHandler().execute(cmd));
	}

	/**
	 * With no term the command still has to resolve its project from the referer, so the
	 * authorization check can run before validation rejects the input.
	 */
	@Test
	void recordingARefererWithoutATermIsRejected() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		User assistant = getUserRepository().findUserByUsername("assistant");
		long ts = System.currentTimeMillis();
		Project project = createProject(admin, "assistant-perms-referer-invalid-" + ts);
		Goal goal = createGoal(admin, project, "assistant-perms-invalid-goal-" + ts);

		AddGlossaryTermRefererCommand cmd = getProjectCommandFactory()
				.newAddGlossaryTermRefererCommand();
		cmd.setEditedBy(assistant);
		cmd.setReferer(goal);

		assertThrows(EntityValidationException.class, () -> getCommandHandler().execute(cmd));
	}

	@Test
	void theAssistantSetIsSmallerThanTheCreatorSet() {
		Set<StakeholderPermission> assistantPermissions = getProjectRepository()
				.findAssistantStakeholderPermissions();
		Set<StakeholderPermission> available = getProjectRepository()
				.findAvailableStakeholderPermissions();

		assertEquals(2, assistantPermissions.size());
		assertTrue(available.containsAll(assistantPermissions),
				"the assistant's permissions must be real rows, not invented ones");
		assertTrue(available.size() > assistantPermissions.size(),
				"granting the assistant the full matrix is what #302 is about");
	}

	// ---- helpers -------------------------------------------------------------

	private Set<String> permissionKeys(UserStakeholder stakeholder) {
		return stakeholder.getStakeholderPermissions().stream()
				.map(StakeholderPermission::getPermissionKey).collect(Collectors.toSet());
	}

	private Project createProject(User owner, String name) throws Exception {
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(owner);
		cmd.setName(name);
		cmd.setText("assistant permissions integration test");
		cmd.setOrganizationName("AssistantPermsOrg-" + name);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	private GlossaryTerm createGlossaryTerm(User actor, Project project, String name)
			throws Exception {
		EditGlossaryTermCommand cmd = getProjectCommandFactory().newEditGlossaryTermCommand();
		cmd.setEditedBy(actor);
		cmd.setProjectOrDomain(getProjectRepository().get(project));
		cmd.setName(name);
		cmd.setText("a term the assistant will link to");
		cmd = getCommandHandler().execute(cmd);
		return cmd.getGlossaryTerm();
	}

	private User createUser(User actor, String username) throws Exception {
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(actor);
		cmd.setUsername(username);
		cmd.setPassword("test-pass");
		cmd.setRepassword("test-pass");
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("AssistantPermsOrg");
		// A user with no role at all fails bean validation on the way in.
		cmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(cmd);
		return getUserRepository().findUserByUsername(username);
	}

	private Goal createGoal(User actor, Project project, String name) throws Exception {
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(actor);
		cmd.setGoalContainer(getProjectRepository().get(project));
		cmd.setName(name);
		cmd.setText("goal the assistant will annotate");
		cmd = getCommandHandler().execute(cmd);
		return cmd.getGoal();
	}
}
