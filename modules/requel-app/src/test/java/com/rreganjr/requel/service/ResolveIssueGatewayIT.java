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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.EditAddWordToDictionaryPositionCommand;
import com.rreganjr.requel.annotation.command.EditChangeSpellingPositionCommand;
import com.rreganjr.requel.annotation.command.EditIgnorePositionCommand;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.command.EditAddActorToProjectPositionCommand;
import com.rreganjr.requel.project.command.EditAddWordToGlossaryPositionCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.service.audit.CommandAuditLog;
import com.rreganjr.requel.service.audit.CommandAuditLogRepository;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #330: ResolveIssue over the gateway (MCP {@code runCommand}/tool, CLI). Accepting each
 * kind of position does what it does from the UI; the caller needs Annotation[Edit] and Edit on
 * whatever the position changes; a position that isn't one of the issue's, or an issue from
 * another project, is refused as input.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ResolveIssueGatewayIT extends AbstractIntegrationTestCase {

	@Autowired
	private CommandGateway gateway;

	@Autowired
	private CommandAuditLogRepository commandAuditLogRepository;

	private Project project;
	private Project otherProject;
	private User admin;
	/** Annotation, Goal, Actor and GlossaryTerm Edit. */
	private String editor;
	/** Annotation[Edit] only. */
	private String annotator;
	/** Goal[Edit] only. */
	private String outsider;
	private int counter;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		admin = getUserRepository().findUserByUsername("admin");
		long ts = System.nanoTime();
		project = createProject("resolve-gw-" + ts);
		otherProject = createProject("resolve-gw-other-" + ts);
		editor = stakeholder("rg-editor-" + ts, Annotation.class, Goal.class, Actor.class, GlossaryTerm.class);
		annotator = stakeholder("rg-annotator-" + ts, Annotation.class);
		outsider = stakeholder("rg-outsider-" + ts, Goal.class);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	// ---- each kind of position ----------------------------------------------------------------

	@Test
	void aPlainPositionResolvesTheIssue() throws Exception {
		Goal goal = goal("plain");
		Issue issue = plainIssue(goal, "Is this goal measurable?");
		Position position = plainPosition(issue, "Yes, by the weekly report.");

		resolve(editor, project, issue, position);

		assertResolvedBy(issue, position);
	}

	@Test
	void anIgnorePositionResolvesTheIssue() throws Exception {
		Goal goal = goal("ignore");
		LexicalIssue issue = lexicalIssue(goal, "Name", "zorbl" + counter);
		EditIgnorePositionCommand cmd = getAnnotationCommandFactory().newEditIgnorePositionCommand();
		cmd.setEditedBy(admin);
		cmd.setIssue(issue);
		cmd.setText("Ignore this word.");
		Position position = getCommandHandler().execute(cmd).getPosition();

		resolve(editor, project, issue, position);

		assertResolvedBy(issue, position);
	}

	@Test
	void fixSpellingRewritesTheWordInTheEntity() throws Exception {
		Goal goal = goal("spelling", "Store the systm logs");
		LexicalIssue issue = lexicalIssue(goal, "Name", "systm");
		Position position = spellingPosition(issue, "system");

		resolve(editor, project, issue, position);

		assertResolvedBy(issue, position);
		assertEquals("Store the system logs", getProjectRepository().get(goal).getName());
	}

	@Test
	void addToDictionaryAddsTheWordToTheProjectDictionary() throws Exception {
		Goal goal = goal("dictionary");
		String word = "Requelish" + Long.toString(System.nanoTime(), 36);
		LexicalIssue issue = lexicalIssue(goal, "Name", word);
		EditAddWordToDictionaryPositionCommand cmd = getAnnotationCommandFactory()
				.newEditAddWordToDictionaryPositionCommand();
		cmd.setEditedBy(admin);
		cmd.setIssue(issue);
		cmd.setText("Add '" + word + "' to the project dictionary");
		Position position = getCommandHandler().execute(cmd).getPosition();

		resolve(editor, project, issue, position);

		assertResolvedBy(issue, position);
		assertTrue(getDictionaryRepository().isKnownWord(project.getId(), word));
	}

	@Test
	void addAsActorAddsTheActor() throws Exception {
		Goal goal = goal("actor");
		String word = "Auditor" + counter;
		LexicalIssue issue = lexicalIssue(goal, "Text", word);
		Position position = actorPosition(issue, word);

		resolve(editor, project, issue, position);

		assertResolvedBy(issue, position);
		assertNotNull(getProjectRepository().findActorByProjectOrDomainAndName(project, word));
	}

	@Test
	void addToGlossaryAddsTheTerm() throws Exception {
		Goal goal = goal("glossary");
		String word = "Ledger" + counter;
		LexicalIssue issue = lexicalIssue(goal, "Text", word);
		EditAddWordToGlossaryPositionCommand cmd = getProjectCommandFactory()
				.newEditAddWordToGlossaryPositionCommand();
		cmd.setEditedBy(admin);
		cmd.setIssue(issue);
		cmd.setProjectOrDomain(project);
		cmd.setText("Add \"" + word + "\" to the project glossary.");
		Position position = getCommandHandler().execute(cmd).getPosition();

		resolve(editor, project, issue, position);

		assertResolvedBy(issue, position);
		assertNotNull(getProjectRepository().findGlossaryTermForProjectOrDomain(project, word));
	}

	// ---- permissions --------------------------------------------------------------------------

	@Test
	void withoutAnnotationEditItIsRefused() throws Exception {
		Goal goal = goal("no-annotation");
		Issue issue = plainIssue(goal, "Who owns this?");
		Position position = plainPosition(issue, "Operations.");

		GatewayException refused = assertThrows(GatewayException.class,
				() -> resolve(outsider, project, issue, position));

		assertEquals(GatewayException.Kind.UNAUTHORIZED, refused.getKind());
		assertFalse(getAnnotationRepository().get(issue).isResolved());
	}

	@Test
	void fixSpellingOnAnEntityTheCallerCantEditIsRefusedAsAWhole() throws Exception {
		Goal goal = goal("spelling-refused", "Keep the systm up");
		LexicalIssue issue = lexicalIssue(goal, "Name", "systm");
		Position position = spellingPosition(issue, "system");

		GatewayException refused = assertThrows(GatewayException.class,
				() -> resolve(annotator, project, issue, position));

		assertEquals(GatewayException.Kind.UNAUTHORIZED, refused.getKind());
		assertEquals("Keep the systm up", getProjectRepository().get(goal).getName());
		assertFalse(getAnnotationRepository().get(issue).isResolved());
	}

	@Test
	void addAsActorWithoutActorEditIsRefusedAndAddsNothing() throws Exception {
		Goal goal = goal("actor-refused");
		String word = "Clerk" + counter;
		LexicalIssue issue = lexicalIssue(goal, "Text", word);
		Position position = actorPosition(issue, word);

		GatewayException refused = assertThrows(GatewayException.class,
				() -> resolve(annotator, project, issue, position));

		assertEquals(GatewayException.Kind.UNAUTHORIZED, refused.getKind());
		assertThrows(Exception.class, () -> getProjectRepository().findActorByProjectOrDomainAndName(project, word));
		assertFalse(getAnnotationRepository().get(issue).isResolved());
	}

	// ---- input --------------------------------------------------------------------------------

	@Test
	void aPositionThatIsNotOneOfTheIssuesIsAFieldError() throws Exception {
		Goal goal = goal("other-position");
		Issue issue = plainIssue(goal, "First question?");
		Issue another = plainIssue(goal, "Second question?");
		Position elsewhere = plainPosition(another, "An answer to the second.");

		GatewayException refused = assertThrows(GatewayException.class,
				() -> resolve(editor, project, issue, elsewhere));

		assertEquals(GatewayException.Kind.INVALID_INPUT, refused.getKind());
		assertTrue(refused.getMessage().contains("is not one of issue"), refused.getMessage());
		assertFalse(getAnnotationRepository().get(issue).isResolved());
	}

	@Test
	void aLexicalResolverOnAPlainIssueIsAFieldErrorNotACrash() throws Exception {
		// Positions are shared by text in a project (#284), so an Add-as-Actor position can end up
		// on a plain issue; its resolver needs a lexical issue's word.
		Goal goal = goal("wrong-kind");
		String word = "Steward" + counter;
		LexicalIssue lexical = lexicalIssue(goal, "Text", word);
		Position actor = actorPosition(lexical, word);
		Issue plain = plainIssue(goal, "Should this be an actor?");
		EditPositionCommand link = getAnnotationCommandFactory().newEditPositionCommand();
		link.setEditedBy(admin);
		link.setIssue(plain);
		link.setPosition(actor);
		link.setText(actor.getText());
		getCommandHandler().execute(link);

		GatewayException refused = assertThrows(GatewayException.class,
				() -> resolve(editor, project, plain, actor));

		assertEquals(GatewayException.Kind.INVALID_INPUT, refused.getKind(), refused.getMessage());
		assertTrue(refused.getMessage().contains("LexicalIssue"), refused.getMessage());
	}

	@Test
	void anIssueFromAnotherProjectIsRefused() throws Exception {
		Goal goal = goal("cross-project");
		Issue issue = plainIssue(goal, "Cross-project?");
		Position position = plainPosition(issue, "No.");

		GatewayException refused = assertThrows(GatewayException.class,
				() -> resolve(editor, otherProject, issue, position));

		assertEquals(GatewayException.Kind.INVALID_INPUT, refused.getKind());
		assertFalse(getAnnotationRepository().get(issue).isResolved());
	}

	@Test
	void anUnknownIssueIsNotFound() throws Exception {
		Goal goal = goal("unknown");
		Issue issue = plainIssue(goal, "Exists?");
		Position position = plainPosition(issue, "Yes.");
		authenticate(editor);

		GatewayException refused = assertThrows(GatewayException.class, () -> gateway.execute(
				new GatewayRequest("ResolveIssue", Map.of("projectName", project.getName(),
						"issueId", Long.MAX_VALUE, "positionId", position.getId()))));

		assertEquals(GatewayException.Kind.NOT_FOUND, refused.getKind());
	}

	@Test
	void theOtherDenylistedAnnotationCommandsAreStillRefused() {
		authenticate(editor);
		for (String commandType : List.of("EditLexicalIssue", "EditChangeSpellingPosition",
				"EditAddWordToDictionaryPosition", "RemoveAnnotationFromAnnotatable")) {
			GatewayException refused = assertThrows(GatewayException.class,
					() -> gateway.execute(new GatewayRequest(commandType, Map.of())), commandType);
			assertEquals(GatewayException.Kind.NOT_ALLOWED, refused.getKind(), commandType);
		}
	}

	@Test
	void theResolveIsAuditedLikeAnyGatewayWrite() throws Exception {
		Goal goal = goal("audited");
		Issue issue = plainIssue(goal, "Audited?");
		Position position = plainPosition(issue, "Always.");
		long before = commandAuditLogRepository.count();

		resolve(editor, project, issue, position);

		List<CommandAuditLog> rows = commandAuditLogRepository.findAll();
		assertTrue(rows.size() > before);
		assertTrue(rows.stream().anyMatch(r -> r.getCommandType() != null
				&& r.getCommandType().contains("ResolveIssue")
				&& project.getId().equals(r.getProjectId())));
	}

	// ---- helpers ------------------------------------------------------------------------------

	private void resolve(String username, Project inProject, Issue issue, Position position) throws Exception {
		authenticate(username);
		gateway.execute(new GatewayRequest("ResolveIssue", Map.of("projectName", inProject.getName(),
				"issueId", issue.getId(), "positionId", position.getId()), "test-client"));
	}

	private void assertResolvedBy(Issue issue, Position position) {
		Issue reloaded = getAnnotationRepository().get(issue);
		assertTrue(reloaded.isResolved(), "resolved");
		assertEquals(position.getId(), reloaded.getResolvedByPosition().getId());
	}

	private void authenticate(String username) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, "x", List.of()));
	}

	private Goal goal(String label) throws Exception {
		return goal(label, "Goal " + label + " " + (++counter));
	}

	/** @param name unique in the project; the spelling tests read it back exactly */
	private Goal goal(String label, String name) throws Exception {
		counter++;
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setName(name);
		cmd.setText("A goal for " + label);
		return getCommandHandler().execute(cmd).getGoal();
	}

	private Issue plainIssue(Goal goal, String text) throws Exception {
		EditIssueCommand cmd = getAnnotationCommandFactory().newEditIssueCommand();
		cmd.setEditedBy(admin);
		cmd.setGroupingObject(project);
		cmd.setAnnotatable(goal);
		cmd.setText(text + " " + (++counter));
		cmd.setMustBeResolved(false);
		return getCommandHandler().execute(cmd).getIssue();
	}

	private Position plainPosition(Issue issue, String text) throws Exception {
		EditPositionCommand cmd = getAnnotationCommandFactory().newEditPositionCommand();
		cmd.setEditedBy(admin);
		cmd.setIssue(issue);
		cmd.setText(text + " " + (++counter));
		return getCommandHandler().execute(cmd).getPosition();
	}

	private LexicalIssue lexicalIssue(Goal goal, String property, String word) throws Exception {
		EditLexicalIssueCommand cmd = getAnnotationCommandFactory().newEditLexicalIssueCommand();
		cmd.setEditedBy(admin);
		cmd.setGroupingObject(project);
		cmd.setAnnotatable(goal);
		cmd.setText("Unknown word: " + word + " (" + (++counter) + ")");
		cmd.setMustBeResolved(false);
		cmd.setWord(word);
		cmd.setAnnotatableEntityPropertyName(property);
		return (LexicalIssue) getCommandHandler().execute(cmd).getIssue();
	}

	private Position spellingPosition(LexicalIssue issue, String to) throws Exception {
		EditChangeSpellingPositionCommand cmd = getAnnotationCommandFactory()
				.newEditChangeSpellingPositionCommand();
		cmd.setEditedBy(admin);
		cmd.setIssue(issue);
		cmd.setText("Change to '" + to + "' " + (++counter));
		cmd.setProposedWord(to);
		return getCommandHandler().execute(cmd).getPosition();
	}

	private Position actorPosition(LexicalIssue issue, String word) throws Exception {
		EditAddActorToProjectPositionCommand cmd = getProjectCommandFactory()
				.newEditAddActorToProjectPositionCommand();
		cmd.setEditedBy(admin);
		cmd.setIssue(issue);
		cmd.setProjectOrDomain(project);
		cmd.setText("Add \"" + word + "\" as an actor to the project.");
		return getCommandHandler().execute(cmd).getPosition();
	}

	private Project createProject(String name) throws Exception {
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(name);
		cmd.setText("#330 test project");
		cmd.setOrganizationName("ResolveGatewayOrg");
		return getCommandHandler().execute(cmd).getProject();
	}

	private String stakeholder(String username, Class<?>... editable) throws Exception {
		EditUserCommand user = getUserCommandFactory().newEditUserCommand();
		user.setEditedBy(admin);
		user.setUsername(username);
		user.setPassword("resolve-gw");
		user.setRepassword("resolve-gw");
		user.setName(username);
		user.setEmailAddress(username + "@example.com");
		user.setPhoneNumber("");
		user.setOrganizationName("ResolveGatewayOrg");
		user.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(user);

		Set<String> keys = new java.util.HashSet<>();
		for (Class<?> type : editable) {
			keys.add(StakeholderPermissionImpl.generatePermissionKey(type, StakeholderPermissionType.Edit));
		}
		EditUserStakeholderCommand cmd = getProjectCommandFactory().newEditUserStakeholderCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setUsername(username);
		cmd.setStakeholderPermissions(keys);
		getCommandHandler().execute(cmd);
		return username;
	}
}
