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
package com.rreganjr.requel.annotation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.command.EditChangeSpellingPositionCommand;
import com.rreganjr.requel.annotation.command.EditIgnorePositionCommand;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.service.api.dto.BulkIssueOutcomeDto;
import com.rreganjr.requel.service.api.dto.BulkResolveIssuesResultDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * #507: {@code BulkResolveIssues} over the in-process gateway, the path MCP and the CLI take. Each
 * issue goes through its own ResolveIssue or EditIssue in its own transaction.
 */
public class BulkResolveIssuesIT extends AbstractIntegrationTestCase {

	@Autowired
	private CommandGateway commandGateway;

	@Autowired
	private QueryGateway queryGateway;

	private User admin;
	private String ts;

	@BeforeEach
	void setUp() throws Exception {
		initializeBaselineData();
		admin = getUserRepository().findUserByUsername("admin");
		ts = Long.toString(System.nanoTime());
	}

	/** AC1: two issues with one fix are fixed; three with several are skipped as ambiguous. */
	@Test
	void appliesTheOneFixAndSkipsTheAmbiguous() throws Exception {
		Project project = createProject("bulk-fix");
		List<Long> single = new ArrayList<>();
		List<Long> several = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			Goal goal = createGoal(project, "Track systm" + i + " health", "");
			Issue issue = lexicalIssue(project, goal, "Name", "systm" + i);
			spelling(issue, "system" + i);
			single.add(issue.getId());
		}
		for (int i = 0; i < 3; i++) {
			Goal goal = createGoal(project, "Cut costz" + i + " now", "");
			Issue issue = lexicalIssue(project, goal, "Name", "costz" + i);
			spelling(issue, "costs" + i);
			spelling(issue, "cost" + i);
			several.add(issue.getId());
		}
		List<Long> all = new ArrayList<>(single);
		all.addAll(several);

		BulkResolveIssuesResultDto result = bulk("admin", project, all, "APPLY_FIX", null);

		assertThat(result.done()).isEqualTo(2);
		assertThat(result.skipped()).isEqualTo(3);
		assertThat(outcomes(result)).containsEntry(single.get(0), BulkIssueOutcomeDto.DONE)
				.containsEntry(single.get(1), BulkIssueOutcomeDto.DONE)
				.containsEntry(several.get(0), BulkIssueOutcomeDto.SKIPPED_AMBIGUOUS);
		assertThat(result.outcomes()).extracting(BulkIssueOutcomeDto::issueId).containsExactlyElementsOf(all);
		assertThat(getProjectRepository().findGoalByProjectOrDomainAndName(project, "Track system0 health"))
				.isNotNull();
		assertThat(openIds(project)).containsExactlyInAnyOrderElementsOf(several);
	}

	/**
	 * AC2: Ignore resolves an issue that offers Ignore and skips one that doesn't (an AI or
	 * person's issue with only a plain position). Re-analysis honouring the ignore is #320's,
	 * covered by its own tests; this checks the issue was resolved by its Ignore position.
	 */
	@Test
	void ignoresWhereIgnoreIsOffered() throws Exception {
		Project project = createProject("bulk-ignore");
		Goal goal = createGoal(project, "Ship the Acmex widget", "");
		Issue lexical = lexicalIssue(project, goal, "Name", "Acmex");
		Long ignoreId = position(getAnnotationCommandFactory().newEditIgnorePositionCommand(), lexical,
				"Ignore 'Acmex'").getId();
		Issue plain = personIssue(project, goal, "Is the widget the right scope?", true);
		position(getAnnotationCommandFactory().newEditPositionCommand(), plain, "Yes, keep it");

		BulkResolveIssuesResultDto result = bulk("admin", project, List.of(lexical.getId(), plain.getId()),
				"IGNORE", null);

		assertThat(result.outcomes()).extracting(BulkIssueOutcomeDto::outcome)
				.containsExactly(BulkIssueOutcomeDto.DONE, BulkIssueOutcomeDto.SKIPPED_NOT_APPLICABLE);
		assertThat(result.outcomes().get(0).positionId()).isEqualTo(ignoreId);
		assertThat(openIds(project)).containsExactly(plain.getId());
	}

	/** AC3: set severity and mark advisory change every selected issue, as the read shows. */
	@Test
	void setsSeverityAndMarksAdvisory() throws Exception {
		Project project = createProject("bulk-edit");
		Goal goal = createGoal(project, "Reduce churn", "");
		Issue first = personIssue(project, goal, "Measured how?", true);
		Issue second = personIssue(project, goal, "Over what period?", true);
		List<Long> ids = List.of(first.getId(), second.getId());

		BulkResolveIssuesResultDto severity = bulk("admin", project, ids, "SET_SEVERITY", "HIGH");
		BulkResolveIssuesResultDto advisory = bulk("admin", project, ids, "MARK_ADVISORY", null);

		assertThat(severity.done()).isEqualTo(2);
		assertThat(advisory.done()).isEqualTo(2);
		List<OpenIssueDto> open = asUser("admin", () -> queryGateway.getOpenIssues(project.getName()));
		assertThat(open).hasSize(2).allSatisfy(issue -> {
			assertThat(issue.severity()).isEqualTo("HIGH");
			assertThat(issue.mustBeResolved()).isFalse();
		});
	}

	/** AC4: no Edit on one target entity refuses that issue; the rest are done, in their own transactions. */
	@Test
	void refusesTheIssueWhoseTargetTheUserCannotEdit() throws Exception {
		Project project = createProject("bulk-refused");
		Goal goal = createGoal(project, "Track systm health", "");
		Actor actor = createActor(project, "Clerkk");
		Issue onGoal = lexicalIssue(project, goal, "Name", "systm");
		spelling(onGoal, "system");
		Issue onActor = lexicalIssue(project, actor, "Name", "Clerkk");
		spelling(onActor, "Clerk");
		String username = "bulk-noedit-" + ts;
		createProjectUser(username);
		addStakeholder(project, username, Set.of(
				StakeholderPermissionImpl.generatePermissionKey(Annotation.class, StakeholderPermissionType.Edit),
				StakeholderPermissionImpl.generatePermissionKey(Goal.class, StakeholderPermissionType.Edit)));

		BulkResolveIssuesResultDto result = bulk(username, project, List.of(onActor.getId(), onGoal.getId()),
				"APPLY_FIX", null);

		assertThat(result.outcomes()).extracting(BulkIssueOutcomeDto::outcome)
				.containsExactly(BulkIssueOutcomeDto.REFUSED, BulkIssueOutcomeDto.DONE);
		assertThat(result.refused()).isEqualTo(1);
		assertThat(openIds(project)).containsExactly(onActor.getId());
	}

	/** AC5: an issue of another project fails the whole request, and nothing changes. */
	@Test
	void anotherProjectsIssueFailsTheRequestWithoutChangingAnything() throws Exception {
		Project project = createProject("bulk-mine");
		Project other = createProject("bulk-other");
		Issue mine = personIssue(project, createGoal(project, "Mine", ""), "Mine?", true);
		Issue theirs = personIssue(other, createGoal(other, "Theirs", ""), "Theirs?", true);

		assertThatThrownBy(() -> bulk("admin", project, List.of(mine.getId(), theirs.getId()),
				"MARK_ADVISORY", null))
				.isInstanceOf(GatewayException.class)
				.hasMessageContaining("belongs to another project");

		List<OpenIssueDto> open = asUser("admin", () -> queryGateway.getOpenIssues(project.getName()));
		assertThat(open).singleElement().satisfies(issue -> assertThat(issue.mustBeResolved()).isTrue());
	}

	/** The input's bounds: the cap, a missing or misplaced severity, an unknown action. */
	@Test
	void refusesBadInput() throws Exception {
		Project project = createProject("bulk-bounds");
		Issue issue = personIssue(project, createGoal(project, "Bounds", ""), "Bounds?", true);
		List<Long> one = List.of(issue.getId());
		List<Long> tooMany = LongStream.rangeClosed(1, 501).boxed().toList();

		assertThatThrownBy(() -> bulk("admin", project, tooMany, "IGNORE", null))
				.isInstanceOf(GatewayException.class);
		assertThatThrownBy(() -> bulk("admin", project, one, "SET_SEVERITY", null))
				.isInstanceOf(GatewayException.class).hasMessageContaining("needs a severity");
		assertThatThrownBy(() -> bulk("admin", project, one, "IGNORE", "LOW"))
				.isInstanceOf(GatewayException.class).hasMessageContaining("only for SET_SEVERITY");
		assertThatThrownBy(() -> bulk("admin", project, one, "DELETE", null))
				.isInstanceOf(GatewayException.class);
		assertThatThrownBy(() -> bulk("admin", project, List.of(Long.MAX_VALUE), "IGNORE", null))
				.isInstanceOf(GatewayException.class);
	}

	// ---- helpers --------------------------------------------------------------------------------

	private BulkResolveIssuesResultDto bulk(String username, Project project, List<Long> ids, String action,
			String severity) throws GatewayException {
		Map<String, Object> input = new HashMap<>();
		input.put("projectName", project.getName());
		input.put("issueIds", ids);
		input.put("action", action);
		if (severity != null) {
			input.put("severity", severity);
		}
		return asUser(username, () -> (BulkResolveIssuesResultDto) commandGateway
				.execute(new GatewayRequest("BulkResolveIssues", input, "bulk-it")).result());
	}

	private List<Long> openIds(Project project) throws GatewayException {
		return asUser("admin", () -> queryGateway.getOpenIssues(project.getName())).stream()
				.map(OpenIssueDto::issueId).toList();
	}

	private static Map<Long, String> outcomes(BulkResolveIssuesResultDto result) {
		return result.outcomes().stream()
				.collect(Collectors.toMap(BulkIssueOutcomeDto::issueId, BulkIssueOutcomeDto::outcome));
	}

	/** A call that may throw the gateway's checked exception. */
	private interface GatewayCall<T> {
		T call() throws GatewayException;
	}

	private static <T> T asUser(String username, GatewayCall<T> call) throws GatewayException {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, "x", List.of()));
		try {
			return call.call();
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private Issue lexicalIssue(Project project, Annotatable on, String property, String word) throws Exception {
		EditLexicalIssueCommand cmd = getAnnotationCommandFactory().newEditLexicalIssueCommand();
		cmd.setEditedBy(admin);
		cmd.setGroupingObject(project);
		cmd.setAnnotatable(on);
		cmd.setText("Possible misspelling: " + word);
		cmd.setMustBeResolved(false);
		cmd.setWord(word);
		cmd.setAnnotatableEntityPropertyName(property);
		return getCommandHandler().execute(cmd).getIssue();
	}

	private Issue personIssue(Project project, Annotatable on, String text, boolean mustBeResolved) throws Exception {
		EditIssueCommand cmd = getAnnotationCommandFactory().newEditIssueCommand();
		cmd.setEditedBy(admin);
		cmd.setGroupingObject(project);
		cmd.setAnnotatable(on);
		cmd.setText(text);
		cmd.setMustBeResolved(mustBeResolved);
		return getCommandHandler().execute(cmd).getIssue();
	}

	private void spelling(Issue issue, String proposedWord) throws Exception {
		EditChangeSpellingPositionCommand cmd = getAnnotationCommandFactory().newEditChangeSpellingPositionCommand();
		cmd.setEditedBy(admin);
		cmd.setIssue(issue);
		cmd.setText("Change to '" + proposedWord + "'");
		cmd.setProposedWord(proposedWord);
		getCommandHandler().execute(cmd);
	}

	private com.rreganjr.requel.annotation.Position position(EditPositionCommand cmd, Issue issue, String text)
			throws Exception {
		cmd.setEditedBy(admin);
		cmd.setIssue(issue);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getPosition();
	}

	private Project createProject(String label) throws Exception {
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(label + "-" + ts);
		cmd.setText("bulk test project");
		cmd.setOrganizationName("BulkTestOrg-" + label + ts);
		return getCommandHandler().execute(cmd).getProject();
	}

	private Goal createGoal(Project project, String name, String text) throws Exception {
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getGoal();
	}

	private Actor createActor(Project project, String name) throws Exception {
		EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
		cmd.setEditedBy(admin);
		cmd.setActorContainer(project);
		cmd.setName(name);
		cmd.setText("");
		return getCommandHandler().execute(cmd).getActor();
	}

	private void createProjectUser(String username) throws Exception {
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(admin);
		cmd.setUsername(username);
		cmd.setPassword("bulk-test");
		cmd.setRepassword("bulk-test");
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("BulkTestOrg");
		cmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(cmd);
	}

	private void addStakeholder(Project project, String username, Set<String> permissionKeys) throws Exception {
		EditUserStakeholderCommand cmd = getProjectCommandFactory().newEditUserStakeholderCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setUsername(username);
		cmd.setStakeholderPermissions(permissionKeys);
		getCommandHandler().execute(cmd);
	}
}
