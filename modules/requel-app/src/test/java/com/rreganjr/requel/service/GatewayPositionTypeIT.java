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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.EditAddWordToDictionaryPositionCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
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
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.IssueDto;
import com.rreganjr.requel.service.api.dto.PositionDto;
import com.rreganjr.requel.service.query.AnnotationQueryController;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code positionType} contract, end to end through the real command gateway (issue #253).
 * <p>
 * {@code EditPosition} used to answer with {@code "PositionImpl$$EnhancerByCGLIB$$1f1035a5"}: a
 * position reaching a DTO mapper by way of a {@code Command} getter is always inside
 * {@code DomainObjectWrappingAdvice}'s pointcut, so it is always CGLIB-wrapped. {@code EditIssue}
 * had the same defect in every nested position, because the wrapper re-wraps collection entries on
 * traversal.
 * <p>
 * Two assertions guard it, deliberately at different scopes — see
 * {@link #assertNoProxyNames(GatewayResult)}.
 *
 * @author ron
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GatewayPositionTypeIT extends AbstractIntegrationTestCase {

	/**
	 * The substring a careless assertion would trip over. Nothing in the positionType resolution
	 * rewrites text, and these fixtures are what prove it.
	 */
	private static final String DOLLARS = "$$";

	/** Any position text here carries {@link #DOLLARS}; that is the only thing special about it. */
	private static final String PRICED = "cost is " + DOLLARS + " per seat";

	/**
	 * The subclass test renames an existing position, so its text must not collide with any other
	 * position in this project: EditPosition looks an existing position up by text with
	 * getSingleResult(), and two matches throw NonUniqueResultException. Issue #284 makes that
	 * lookup deterministic and de-duplicates; this constant can go with it.
	 */
	private static final String PRICED_PER_WORD = "cost is " + DOLLARS + " per dictionary word";

	@Autowired
	private CommandGateway gateway;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AnnotationQueryController annotationQueryController;

	@PersistenceContext
	private EntityManager entityManager;

	private String projectName;
	private String editorUsername;
	private Long goalId;
	private Project project;
	private Goal goal;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");

		long ts = System.currentTimeMillis();
		projectName = "pt-test-" + ts;
		EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
		projectCmd.setEditedBy(admin);
		projectCmd.setName(projectName);
		projectCmd.setText("positionType integration test project");
		projectCmd.setOrganizationName("PtTestOrg-" + ts);
		projectCmd = getCommandHandler().execute(projectCmd);
		project = projectCmd.getProject();

		editorUsername = "pt-editor-" + ts;
		createUser(editorUsername);
		Set<String> perms = new HashSet<>();
		perms.addAll(keys(StakeholderPermissionType.Edit, Project.class, Goal.class,
				Annotation.class));
		perms.addAll(keys(StakeholderPermissionType.Delete, Goal.class, Annotation.class));
		addUserStakeholder(project, editorUsername, perms);

		EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
		goalCmd.setEditedBy(admin);
		goalCmd.setGoalContainer(project);
		goalCmd.setName("pt-goal-" + ts);
		goalCmd.setText("the goal these issues hang off");
		goalCmd = getCommandHandler().execute(goalCmd);
		goal = goalCmd.getGoal();
		goalId = goal.getId();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	// ---- the write surface ---------------------------------------------------------------------

	@Test
	void editPositionReportsAStableTypeAndNoProxyName() throws Exception {
		authenticate(editorUsername);
		Long issueId = createIssue("an issue whose position is plain");

		GatewayResult result = gateway.execute(new GatewayRequest("EditPosition", Map.of(
				"projectName", projectName, "issueId", issueId, "text", PRICED)));

		PositionDto position = (PositionDto) result.result();
		assertNotNull(position);
		assertEquals("Position", position.positionType());
		assertNoProxyNames(result);
	}

	/**
	 * The defect this test exists for: {@code toIssueDto} maps each nested position, and
	 * {@code DomainObjectWrapper} re-wraps collection entries on traversal, so every position in an
	 * {@code EditIssue} response was proxied and leaked a generated name.
	 */
	@Test
	void editIssueReportsEveryNestedPositionWithoutProxyNames() throws Exception {
		authenticate(editorUsername);
		String text = "an issue with two positions";
		Long issueId = createIssue(text);
		gateway.execute(new GatewayRequest("EditPosition", Map.of(
				"projectName", projectName, "issueId", issueId, "text", PRICED)));
		gateway.execute(new GatewayRequest("EditPosition", Map.of(
				"projectName", projectName, "issueId", issueId,
				"text", "or " + DOLLARS + " per user")));

		// Re-dispatching EditIssue on the existing issue returns it with its positions nested.
		GatewayResult result = gateway.execute(new GatewayRequest("EditIssue",
				Map.of("projectName", projectName, "entityType", "Goal", "entityId", goalId,
						"issueId", issueId, "text", text)));

		IssueDto issue = (IssueDto) result.result();
		assertNotNull(issue);
		// Two texts because two distinct positions are wanted: matching text is reused rather
		// than duplicated (issue #281), which would leave one position here.
		assertEquals(2, issue.positions().size());
		assertEquals(Set.of("Position"), positionTypesOf(issue));
		assertNoProxyNames(result);
	}

	/**
	 * The subclass half of the contract, on the write surface. Subclass positions are only ever
	 * attached to a {@link com.rreganjr.requel.annotation.impl.LexicalIssue} — both
	 * {@code EditAddWordToDictionaryPosition} and {@code EditAddActorToProjectPosition} cast the
	 * issue to one — so the fixture builds a lexical issue the way NLP analysis does, then edits the
	 * resulting position back through the gateway.
	 */
	@Test
	void editPositionReportsASubclassPositionByItsOwnType() throws Exception {
		LexicalFixture fixture = addWordToDictionaryPosition("requel",
				"add 'requel' to the dictionary");

		authenticate(editorUsername);
		// issueId is @NotNull on EditPositionInput, so an edit-by-id carries both.
		GatewayResult result = gateway.execute(new GatewayRequest("EditPosition",
				Map.of("projectName", projectName, "issueId", fixture.issueId(),
						"positionId", fixture.positionId(), "text", PRICED_PER_WORD)));

		PositionDto position = (PositionDto) result.result();
		assertNotNull(position);
		assertEquals("AddWordToDictionaryPosition", position.positionType(),
				"a subclass position must survive proxying with its own type");
		assertNoProxyNames(result);
	}

	@Test
	void textIsNeverRewrittenAndNeverTripsTheProxyAssertions() throws Exception {
		authenticate(editorUsername);
		Long issueId = createIssue("an issue whose position text contains dollars");
		String text = PRICED;

		GatewayResult result = gateway.execute(new GatewayRequest("EditPosition",
				Map.of("projectName", projectName, "issueId", issueId, "text", text)));

		PositionDto position = (PositionDto) result.result();
		assertEquals(text, position.text(), "position text must round-trip untouched");
		assertTrue(objectMapper.writeValueAsString(result.result()).contains(DOLLARS),
				"a $$ in a text value must survive into the response");
		// The point of the fixture: a "$$" in a text value is not a proxy leak.
		assertNoProxyNames(result);
	}

	// ---- the read surface ----------------------------------------------------------------------

	/**
	 * {@code @Transactional} because the controller walks a lazy {@code annotations} collection and
	 * is called here in-process, without the session a real request would have. Same shape as
	 * {@code TagApiIT.dispatchesTagAndCategoryCommandsAndReadsThemBack}: dispatch, flush, then read
	 * back through the query controller.
	 */
	@Test
	@Transactional
	void theQueryPathReportsTheSameTypes() throws Exception {
		authenticate(editorUsername);
		String text = "an issue read back over the query controller";
		Long issueId = createIssue(text);
		gateway.execute(new GatewayRequest("EditPosition", Map.of(
				"projectName", projectName, "issueId", issueId, "text", PRICED)));
		// A lexical issue on the same goal, carrying a subclass position, so the read model under
		// assertion holds both shapes.
		addWordToDictionaryPosition("elicitation", "add 'elicitation' to the dictionary");

		entityManager.flush();
		AnnotationsDto annotations = annotationQueryController
				.getAnnotations(projectName, "Goal", goalId).getBody();

		assertNotNull(annotations);
		IssueDto issue = annotations.issues().stream()
				.filter(candidate -> issueId.equals(candidate.id()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("issue " + issueId + " not in the read model"));
		assertEquals(Set.of("Position"), positionTypesOf(issue));

		Set<String> everyType = annotations.issues().stream()
				.flatMap(candidate -> candidate.positions().stream())
				.map(PositionDto::positionType)
				.collect(Collectors.toSet());
		assertTrue(everyType.contains("AddWordToDictionaryPosition"),
				"the read model lost the subclass position type: " + everyType);
		for (String positionType : everyType) {
			assertTrue(positionType.matches("[A-Za-z0-9]+"),
					"the read path reported a generated name: " + positionType);
		}
	}

	// ---- helpers -------------------------------------------------------------------------------

	/**
	 * Two checks, scoped differently on purpose.
	 * <p>
	 * <strong>Field-scoped and strict:</strong> every {@code positionType} in the tree must be a
	 * bare alphanumeric name. That is where a proxy name would actually land, the pattern rejects
	 * {@code $$}, package dots and the ByteBuddy {@code $HibernateProxy$} shape alike, and user
	 * text can never reach it.
	 * <p>
	 * <strong>Document-scoped and narrow:</strong> the body must not contain
	 * {@code $$EnhancerBy} — the literal acceptance criterion. Sweeping the whole document for that
	 * exact string is safe where sweeping it for a bare {@code $$} would not be, because a text
	 * value legitimately may contain {@code $$} and this fixture makes sure one does.
	 */
	private void assertNoProxyNames(GatewayResult result) throws Exception {
		String json = objectMapper.writeValueAsString(result.result());

		List<JsonNode> positionTypes = objectMapper.readTree(json).findValues("positionType");
		assertFalse(positionTypes.isEmpty(), "no positionType to check in: " + json);
		for (JsonNode positionType : positionTypes) {
			assertTrue(positionType.asText().matches("[A-Za-z0-9]+"),
					"positionType is not a bare, stable name: " + positionType.asText());
		}

		assertFalse(json.contains("$$EnhancerBy"), "a CGLIB proxy name reached a client: " + json);
	}

	private Set<String> positionTypesOf(IssueDto issue) {
		return issue.positions().stream().map(PositionDto::positionType)
				.collect(Collectors.toSet());
	}

	private Long createIssue(String text) throws Exception {
		GatewayResult result = gateway.execute(new GatewayRequest("EditIssue",
				Map.of("projectName", projectName, "entityType", "Goal", "entityId", goalId,
						"text", text)));
		return ((IssueDto) result.result()).id();
	}

	/**
	 * A lexical issue on the fixture goal plus the dictionary position that resolves it — the way
	 * NLP spell-check analysis creates both. A subclass position cannot hang off a plain issue:
	 * EditAddWordToDictionaryPositionCommandImpl casts the issue to LexicalIssue.
	 *
	 * @return the ids of the new lexical issue and its position
	 */
	private LexicalFixture addWordToDictionaryPosition(String word, String text) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");

		EditLexicalIssueCommand issueCmd = getAnnotationCommandFactory()
				.newEditLexicalIssueCommand();
		issueCmd.setEditedBy(admin);
		issueCmd.setGroupingObject(project);
		issueCmd.setAnnotatable(goal);
		issueCmd.setText("'" + word + "' is not in the dictionary");
		issueCmd.setMustBeResolved(false);
		issueCmd.setWord(word);
		issueCmd = getCommandHandler().execute(issueCmd);
		Issue issue = issueCmd.getIssue();
		assertNotNull(issue, "the lexical issue for '" + word + "' was not persisted");

		EditAddWordToDictionaryPositionCommand positionCmd = getAnnotationCommandFactory()
				.newEditAddWordToDictionaryPositionCommand();
		positionCmd.setEditedBy(admin);
		positionCmd.setIssue(issue);
		positionCmd.setText(text);
		positionCmd = getCommandHandler().execute(positionCmd);
		Position position = positionCmd.getPosition();
		assertNotNull(position, "the dictionary position for '" + word + "' was not persisted");
		return new LexicalFixture(issue.getId(), position.getId());
	}

	/** The ids a lexical-issue fixture produces. */
	private record LexicalFixture(Long issueId, Long positionId) {
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
		cmd.setOrganizationName("PtTestOrg");
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
