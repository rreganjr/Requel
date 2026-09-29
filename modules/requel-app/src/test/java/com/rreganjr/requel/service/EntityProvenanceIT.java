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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.assistant.core.context.EntityContextPackBuilder;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.CriterionHash;
import com.rreganjr.requel.project.EntitySourceLink;
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProvenanceStore;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.SourceLinkRelation;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.DeleteGoalCommand;
import com.rreganjr.requel.project.command.DeleteProjectCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.service.api.dto.EntitySourceLinkDto;
import com.rreganjr.requel.service.api.dto.ExternalSourceDto;
import com.rreganjr.requel.service.api.dto.RecordSourceResultDto;
import com.rreganjr.requel.service.api.dto.SourceEntitiesDto;
import com.rreganjr.requel.service.api.dto.UpsertFromSourceResultDto;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #272: entity provenance through the real command and query gateways — the
 * UpsertFromSource decision table, fan-out, the provenance reads, delete paths, and the rule that
 * no general read or context pack carries a source or its locator.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntityProvenanceIT extends AbstractIntegrationTestCase {

	private static final String LOCATOR = "https://tracker.example.com/browse/";

	@Autowired
	private CommandGateway gateway;

	@Autowired
	private QueryGateway queryGateway;

	@Autowired
	private ProvenanceStore provenanceStore;

	@Autowired
	private EntityContextPackBuilder contextPackBuilder;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private ObjectMapper objectMapper;

	private String projectName;
	private Project project;
	private String editorUsername;
	private String readerUsername;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		long ts = System.currentTimeMillis();
		projectName = "provenance-test-" + ts;
		project = createProject(projectName);
		editorUsername = "provenance-editor-" + ts;
		createUser(editorUsername);
		addUserStakeholder(project, editorUsername, keys(StakeholderPermissionType.Edit,
				Project.class, Goal.class, Actor.class, UseCase.class, Scenario.class,
				Annotation.class));
		readerUsername = "provenance-reader-" + ts;
		createUser(readerUsername);
		addUserStakeholder(project, readerUsername, Set.of());
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	// ---- the decision table ----------------------------------------------------------------------

	@Test
	void aFragmentCreatesThenLeavesUnchangedThenUpdatesItsGoal() throws Exception {
		authenticate(editorUsername);
		String key = source("dt");

		UpsertFromSourceResultDto created = upsertGoal(key, "AC-1", "Rooms end on time", null);
		assertEquals("CREATED", created.status());
		assertEquals("Goal", created.entityType());
		assertNotNull(created.entityId());
		assertNotNull(created.entity(), "the edit command's own result comes back");

		UpsertFromSourceResultDto unchanged = upsertGoal(key, "AC-1", "Rooms end on time", null);
		assertEquals("UNCHANGED", unchanged.status());
		assertEquals(created.entityId(), unchanged.entityId());

		UpsertFromSourceResultDto updated = upsertGoal(key, "AC-1",
				"Rooms end within five minutes of the scheduled time", null);
		assertEquals("UPDATED", updated.status(), "an edited criterion updates in place (#71's"
				+ " duplicate bug)");
		assertEquals(created.entityId(), updated.entityId());
		assertEquals("Rooms end within five minutes of the scheduled time", goalText(updated));
		assertEquals(1, queryGateway.findEntitiesBySource(projectName, "jira", key, null).links()
				.size(), "still one goal from the fragment");
	}

	@Test
	void aChangedFragmentOnAGoalEditedInRequelRaisesOneConflictIssueAndLeavesTheGoal()
			throws Exception {
		authenticate(editorUsername);
		String key = source("conflict");
		UpsertFromSourceResultDto created = upsertGoal(key, "AC-2", "Admins can end a room", null);
		editGoalText(created.entityId(), "Only Conduit admins can end a room");

		UpsertFromSourceResultDto conflict = upsertGoal(key, "AC-2",
				"Any admin can end a room", null);
		assertEquals("CONFLICT", conflict.status());
		assertEquals("Only Conduit admins can end a room", goalText(conflict),
				"a Requel edit is never overwritten");
		assertNotNull(conflict.issueId());

		UpsertFromSourceResultDto again = upsertGoal(key, "AC-2",
				"Any admin can end or pause a room", null);
		assertEquals("CONFLICT", again.status());
		assertEquals(conflict.issueId(), again.issueId(), "one conflict issue per link");

		inTransaction(() -> {
			Issue issue = getAnnotationRepository().findById(Issue.class, conflict.issueId());
			assertTrue(issue.getText().contains("jira " + key + " AC-2"), issue.getText());
			assertFalse(issue.getText().contains(LOCATOR), "the locator never enters annotation text");
			List<String> positions = issue.getPositions().stream().map(Position::getText).toList();
			assertTrue(positions.contains("Source now reads: Any admin can end a room"), "" + positions);
			assertTrue(positions.contains("Source now reads: Any admin can end or pause a room"),
					"" + positions);
			return null;
		});
	}

	@Test
	void severalGoalsFromOneFragmentAreAmbiguousUntilTheCallerPicksOne() throws Exception {
		authenticate(editorUsername);
		String key = source("fanout");
		UpsertFromSourceResultDto first = upsertGoal(key, "AC-3", "Ownership moves to Conduit", null);
		Long second = createGoal("Ownership record-" + System.nanoTime(), "An ownership record");
		link("Goal", second, key, "AC-3", "Ownership moves to Conduit");

		UpsertFromSourceResultDto ambiguous = upsertGoal(key, "AC-3",
				"Ownership moves to Conduit with a written record", null);
		assertEquals("AMBIGUOUS", ambiguous.status());
		assertEquals(Set.of(first.entityId(), second), new HashSet<>(ambiguous.candidates()));
		assertNull(ambiguous.entityId());

		UpsertFromSourceResultDto picked = upsertGoal(key, "AC-3",
				"Ownership moves to Conduit with a written record", first.entityId());
		assertEquals("UPDATED", picked.status());
		assertEquals(first.entityId(), picked.entityId());

		GatewayException notFromFragment = assertThrows(GatewayException.class,
				() -> upsertGoal(key, "AC-4", "Something else", first.entityId()));
		assertEquals(GatewayException.Kind.INVALID_INPUT, notFromFragment.getKind());
	}

	@Test
	void aStepEditedInRequelMakesAChangedScenarioAConflict() throws Exception {
		authenticate(editorUsername);
		String key = source("steps");
		String step = "Host opens the room " + key;
		Map<String, Object> scenario = new HashMap<>();
		scenario.put("name", "Open a room " + key);
		scenario.put("text", "The host opens a room");
		scenario.put("steps", List.of(Map.of("name", step, "text", "Opens it", "isScenario", false)));
		UpsertFromSourceResultDto created = upsert("EditScenario", scenario, key, "UC-2",
				"The host opens a room", null);
		assertEquals("CREATED", created.status());

		// A Requel edit to a step only: the scenario's own name and text are unchanged.
		Long stepId = inTransaction(() -> getProjectRepository().findById(Scenario.class,
				created.entityId()).getSteps().get(0).getId());
		Map<String, Object> edit = new HashMap<>();
		edit.put("projectName", projectName);
		edit.put("scenarioId", created.entityId());
		edit.put("name", "Open a room " + key);
		edit.put("steps", List.of(Map.of("stepId", stepId, "name", step,
				"text", "Opens it after the sound check", "isScenario", false)));
		gateway.execute(new GatewayRequest("EditScenario", edit));

		UpsertFromSourceResultDto again = upsert("EditScenario", scenario, key, "UC-2",
				"The host opens a room after checking audio", null);
		assertEquals("CONFLICT", again.status(), "a step edit is a Requel edit too");
	}

	@Test
	void anUpsertSetsTheLocatorOnlyWhenItFirstRecordsTheSource() throws Exception {
		authenticate(editorUsername);
		String key = source("locator");
		upsertGoal(key, "AC-1", "Locator stays", null);
		Map<String, Object> input = goalInput(nameFor(key, "AC-2"), "Another");
		Map<String, Object> request = upsertRequest("EditGoal", input, key, "AC-2", "Another", null);
		request.put("locator", "https://elsewhere.example.com/" + key);
		request.put("title", "Retitled");
		gateway.execute(new GatewayRequest("UpsertFromSource", request));

		ExternalSourceDto recorded = queryGateway.getSource(projectName, "jira", key);
		assertEquals(LOCATOR + key, recorded.locator(), "RecordSource (Project[Edit]) changes it");
		assertNull(recorded.title());
	}

	@Test
	void anAmbiguousUpsertLeavesTheSourceAsItWas() throws Exception {
		authenticate(editorUsername);
		String key = source("ambiguous-version");
		UpsertFromSourceResultDto first = upsert("EditGoal", goalInput(nameFor(key, "AC-1"), "One"),
				key, "AC-1", "One", "v1");
		Long second = createGoal("Second from AC-1 " + System.nanoTime(), "Another");
		link("Goal", second, key, "AC-1", "One");

		UpsertFromSourceResultDto ambiguous = upsert("EditGoal",
				goalInput(nameFor(key, "AC-1"), "One, changed"), key, "AC-1", "One, changed", "v2");
		assertEquals("AMBIGUOUS", ambiguous.status());
		assertEquals("v1", queryGateway.getSource(projectName, "jira", key).contentHash());

		UpsertFromSourceResultDto picked = upsert("EditGoal",
				goalInput(nameFor(key, "AC-1"), "One, changed"), key, "AC-1", "One, changed", "v2",
				first.entityId());
		assertTrue(picked.sourceChanged(), "the retry is the call that records v2");
	}

	@Test
	void theInputMustNotCarryTheEntityId() {
		authenticate(editorUsername);
		Map<String, Object> input = goalInput("Carries an id " + System.nanoTime(), "text");
		input.put("goalId", 1L);
		GatewayException e = assertThrows(GatewayException.class, () -> gateway.execute(
				new GatewayRequest("UpsertFromSource", upsertRequest("EditGoal", input,
						source("id"), "AC-1", "text", null))));
		assertEquals(GatewayException.Kind.INVALID_INPUT, e.getKind());
	}

	@Test
	void upsertGoalFromRequirementGoesThroughUpsertFromSource() throws Exception {
		authenticate(editorUsername);
		String key = source("upserter");
		com.rreganjr.requel.gateway.tracker.RequirementGoalUpserter upserter =
				new com.rreganjr.requel.gateway.tracker.RequirementGoalUpserter(gateway, queryGateway);

		var created = upserter.upsert(com.rreganjr.requel.gateway.tracker.UpsertGoalRequest.of(
				projectName, "Hosts can mute a panelist " + key + ".", "jira", key, LOCATOR + key,
				"AC-9", "claude-desktop"));
		assertEquals("CREATED", created.status());
		var edited = upserter.upsert(com.rreganjr.requel.gateway.tracker.UpsertGoalRequest.of(
				projectName, "Hosts can mute or remove a panelist " + key + ".", "jira", key,
				LOCATOR + key, "AC-9", "claude-desktop"));
		assertEquals("UPDATED", edited.status());
		assertEquals(created.goalId(), edited.goalId());
		assertTrue(inTransaction(() -> getProjectRepository().findById(Goal.class, created.goalId())
				.getAnnotations().stream().noneMatch(a -> a.getText() != null
						&& a.getText().contains("requel-provenance"))),
				"no provenance note is written");
	}

	// ---- AC1: re-ingesting a whole source creates no duplicates ----------------------------------

	@Test
	void reIngestingARoundtableShapedSourceCreatesNoDuplicates() throws Exception {
		authenticate(editorUsername);
		String key = source("roundtable");
		for (int pass = 0; pass < 2; pass++) {
			for (int ac = 1; ac <= 8; ac++) {
				upsertGoal(key, "AC-" + ac, "Criterion number " + ac + " holds", null);
			}
			upsert("EditActor", Map.of("name", "Room host " + key, "description", "Runs a room"),
					key, "actors", "Room host runs a room", null);
			upsert("EditUseCase", Map.of("name", "End a room " + key, "text", "The host ends it",
					"primaryActorName", "Room host " + key),
					key, "UC-1", "The host ends a room", null);
		}
		SourceEntitiesDto produced = queryGateway.findEntitiesBySource(projectName, "jira", key, null);
		assertEquals(10, produced.links().size(), "8 goals, an actor and a use case, once each");
		assertEquals(Set.of("Goal", "Actor", "UseCase"),
				produced.links().stream().map(EntitySourceLinkDto::entityType)
						.collect(Collectors.toSet()));
	}

	// ---- AC2: both directions --------------------------------------------------------------------

	@Test
	void anEntityReportsItsSourcesAndASourceReportsItsEntities() throws Exception {
		authenticate(editorUsername);
		String ticket = source("both-ticket");
		String guide = "docs/guide-" + System.nanoTime() + ".pdf";
		UpsertFromSourceResultDto goal = upsertGoal(ticket, "AC-5", "Recordings are archived", null);
		recordSource("doc", guide, "PATH", guide, null);
		link("Goal", goal.entityId(), "doc", guide, "p.12", "Recordings are archived");

		List<EntitySourceLinkDto> sources = queryGateway.getEntitySources(projectName, "Goal",
				goal.entityId());
		assertEquals(2, sources.size(), "" + sources);
		assertEquals(Set.of("jira " + ticket + " AC-5", "doc " + guide + " p.12"),
				sources.stream().map(l -> l.source().system() + " " + l.source().externalId() + " "
						+ l.fragment()).collect(Collectors.toSet()));

		SourceEntitiesDto fromTicket = queryGateway.findEntitiesBySource(projectName, "JIRA", ticket,
				"AC-5");
		assertEquals(List.of(goal.entityId()),
				fromTicket.links().stream().map(EntitySourceLinkDto::entityId).toList());
		assertNull(queryGateway.findEntitiesBySource(projectName, "jira", "NOPE-1", null));
		assertNull(queryGateway.getSource(projectName, "jira", "NOPE-1"));
	}

	// ---- AC3: a changed source is detectable without reading entities ---------------------------

	@Test
	void aChangedSourceVersionIsReportedAndStaleFragmentsAreFlagged() throws Exception {
		authenticate(editorUsername);
		String key = source("version");
		RecordSourceResultDto first = recordSource("jira", key, "URL", LOCATOR + key, "v1");
		assertTrue(first.created());
		assertFalse(first.changed());
		upsert("EditGoal", goalInput("Version one " + key, "Kept"), key, "AC-1", "Kept", "v1");
		upsert("EditGoal", goalInput("Version one gone " + key, "Dropped"), key, "AC-2", "Dropped",
				"v1");

		assertFalse(recordSource("jira", key, null, null, "v1").changed(), "same hash: unchanged");
		assertEquals("v1", queryGateway.getSource(projectName, "jira", key).contentHash());

		// v2 of the source still has AC-1 but no AC-2.
		UpsertFromSourceResultDto kept = upsert("EditGoal", goalInput("Version one " + key, "Kept"),
				key, "AC-1", "Kept", "v2");
		assertTrue(kept.sourceChanged(), "the upsert reports the new version");
		Map<String, Boolean> stale = queryGateway.findEntitiesBySource(projectName, "jira", key, null)
				.links().stream().collect(Collectors.toMap(EntitySourceLinkDto::fragment,
						EntitySourceLinkDto::notInLatestSource));
		assertEquals(Map.of("AC-1", false, "AC-2", true), stale);
	}

	// ---- identity, locators and validation --------------------------------------------------------

	@Test
	void theExternalIdKeepsItsCaseAndTheSystemIsLowerCased() throws Exception {
		authenticate(editorUsername);
		String key = source("CASE");
		RecordSourceResultDto recorded = recordSource("JIRA", key, "URL", LOCATOR + key, null);
		assertEquals("jira", recorded.source().system());
		assertEquals(key, recorded.source().externalId(), "tags lower-cased this; sources don't");
		assertTrue(recordSource("jira", key.toLowerCase(), null, null, null).created(),
				"a differently-cased id is a different source");
	}

	@Test
	void locatorsAreChecked() {
		authenticate(editorUsername);
		assertInvalid(() -> recordSource("doc", source("path"), "PATH", "../secret.txt", null));
		assertInvalid(() -> recordSource("doc", source("path"), "PATH", "/etc/passwd", null));
		assertInvalid(() -> recordSource("doc", source("path"), "PATH", "file:/etc/passwd", null));
		assertInvalid(() -> recordSource("jira", source("url"), "URL", "ftp://example.com/x", null));
		assertInvalid(() -> recordSource("jira", source("url"), "URL", "javascript:alert(1)", null));
		assertInvalid(() -> recordSource("jira", source("url"), null, LOCATOR + "X-1", null));
	}

	@Test
	void aCallerWithoutEditOnTheEntityTypeCannotIngest() {
		authenticate(readerUsername);
		GatewayException e = assertThrows(GatewayException.class,
				() -> upsertGoal(source("denied"), "AC-1", "Not allowed", null));
		assertEquals(GatewayException.Kind.UNAUTHORIZED, e.getKind());
	}

	// ---- P6: links converted from #71 notes --------------------------------------------------------

	@Test
	void aConvertedLinkIsInferredUneditedWhenTheGoalTextStillMatches() throws Exception {
		authenticate(editorUsername);
		String key = source("p6");
		Long untouched = createGoal("P6 untouched " + key, "Hosts can pause a room");
		Long edited = createGoal("P6 edited " + key, "Hosts can pause any room at all");
		recordSource("jira", key, null, null, null);
		ExternalSource source = provenanceStore.findSource(project.getId(), "jira", key).orElseThrow();
		// As V26 writes them: the note's criterionHash, no fingerprint.
		inTransaction(() -> {
			provenanceStore.link(new ProvenanceStore.LinkSpec(source, SourceLinkRelation.DERIVED_FROM,
					"Goal", untouched, "AC-1", CriterionHash.of("Hosts can pause a room"), null, null),
					null);
			provenanceStore.link(new ProvenanceStore.LinkSpec(source, SourceLinkRelation.DERIVED_FROM,
					"Goal", edited, "AC-2", CriterionHash.of("Hosts can pause a room"), null, null),
					null);
			return null;
		});

		assertEquals("UPDATED", upsertGoal(key, "AC-1", "Hosts can pause or resume a room", null)
				.status());
		assertEquals("CONFLICT", upsertGoal(key, "AC-2", "Hosts can pause or resume a room", null)
				.status());
		inTransaction(() -> {
			assertNotNull(linkOf(key, "AC-1").getEntityFingerprint(), "filled in once known unedited");
			assertNull(linkOf(key, "AC-2").getEntityFingerprint(), "left to keep conflicting");
			return null;
		});
	}

	// ---- delete paths ------------------------------------------------------------------------------

	@Test
	void deletingAGoalRemovesItsLinksAndDeletingTheProjectRemovesItsSources() throws Exception {
		authenticate(editorUsername);
		String key = source("delete");
		UpsertFromSourceResultDto goal = upsertGoal(key, "AC-1", "Goes away", null);
		User admin = getUserRepository().findUserByUsername("admin");
		DeleteGoalCommand delete = getProjectCommandFactory().newDeleteGoalCommand();
		delete.setEditedBy(admin);
		delete.setGoal(getProjectRepository().findById(Goal.class, goal.entityId()));
		getCommandHandler().execute(delete);
		assertTrue(provenanceStore.linksForTarget("Goal", goal.entityId()).isEmpty());
		assertNotNull(queryGateway.getSource(projectName, "jira", key), "the source is kept");

		Project doomed = createProject("provenance-doomed-" + System.nanoTime());
		provenanceStore.recordSource(new ProvenanceStore.SourceSpec(doomed.getId(), "jira", key,
				null, null, null, null), admin);
		DeleteProjectCommand deleteProject = getProjectCommandFactory().newDeleteProjectCommand();
		deleteProject.setEditedBy(admin);
		deleteProject.setProject(getProjectRepository().findById(Project.class, doomed.getId()));
		getCommandHandler().execute(deleteProject);
		assertTrue(provenanceStore.listSources(doomed.getId()).isEmpty());
	}

	// ---- AC4: provenance reaches no general read and no context pack -------------------------------

	@Test
	void noGeneralReadOrContextPackCarriesTheSourceOrItsLocator() throws Exception {
		authenticate(editorUsername);
		String key = source("ac4");
		UpsertFromSourceResultDto goal = upsert("EditGoal",
				goalInput("Alarms route to Conduit " + System.nanoTime(),
						"Every alarm routes to Conduit"), key,
				"AC-6", "Every alarm routes to Conduit", null);
		assertEquals("CREATED", goal.status());

		// The in-process reads walk lazy collections, so each runs in a transaction the way a
		// request's session would give it one.
		String entity = json(() -> queryGateway.getEntity(projectName, "Goal", goal.entityId()));
		String annotations = json(() -> queryGateway.getAnnotations(projectName, "Goal",
				goal.entityId()));
		String context = json(() -> queryGateway.getProjectContext(projectName));
		for (String read : List.of(entity, annotations, context)) {
			assertFalse(read.contains(LOCATOR), read);
			assertFalse(read.contains(key), read);
		}
		String pack = json(() -> contextPackBuilder.build(
				getProjectRepository().findById(Goal.class, goal.entityId())));
		assertFalse(pack.contains(LOCATOR), pack);
		assertFalse(pack.contains(key), pack);
	}

	// ---- helpers -----------------------------------------------------------------------------------

	private String source(String label) {
		return "CON-" + label + "-" + System.nanoTime();
	}

	private UpsertFromSourceResultDto upsertGoal(String key, String fragment, String text,
			Long entityId) throws GatewayException {
		return upsert("EditGoal", goalInput(nameFor(key, fragment), text), key, fragment, text, null,
				entityId);
	}

	private String nameFor(String key, String fragment) {
		return key + " " + fragment;
	}

	private Map<String, Object> goalInput(String name, String text) {
		Map<String, Object> input = new HashMap<>();
		input.put("name", name);
		input.put("text", text);
		return input;
	}

	private UpsertFromSourceResultDto upsert(String command, Map<String, Object> input, String key,
			String fragment, String fragmentText, String sourceVersion) throws GatewayException {
		return upsert(command, input, key, fragment, fragmentText, sourceVersion, null);
	}

	private UpsertFromSourceResultDto upsert(String command, Map<String, Object> input, String key,
			String fragment, String fragmentText, String sourceVersion, Long entityId)
			throws GatewayException {
		Map<String, Object> request = upsertRequest(command, input, key, fragment, fragmentText,
				entityId);
		request.put("sourceVersion", sourceVersion);
		return (UpsertFromSourceResultDto) gateway.execute(new GatewayRequest("UpsertFromSource",
				request)).result();
	}

	private Map<String, Object> upsertRequest(String command, Map<String, Object> input, String key,
			String fragment, String fragmentText, Long entityId) {
		Map<String, Object> request = new HashMap<>();
		request.put("projectName", projectName);
		request.put("command", command);
		request.put("input", new HashMap<>(input));
		request.put("system", "jira");
		request.put("externalId", key);
		request.put("locatorType", "URL");
		request.put("locator", LOCATOR + key);
		request.put("fragment", fragment);
		request.put("fragmentText", fragmentText);
		request.put("entityId", entityId);
		return request;
	}

	private RecordSourceResultDto recordSource(String system, String key, String locatorType,
			String locator, String contentHash) throws GatewayException {
		Map<String, Object> request = new HashMap<>();
		request.put("projectName", projectName);
		request.put("system", system);
		request.put("externalId", key);
		request.put("locatorType", locatorType);
		request.put("locator", locator);
		request.put("contentHash", contentHash);
		return (RecordSourceResultDto) gateway.execute(new GatewayRequest("RecordSource", request))
				.result();
	}

	private void link(String entityType, Long entityId, String key, String fragment,
			String fragmentText) throws GatewayException {
		link(entityType, entityId, "jira", key, fragment, fragmentText);
	}

	private void link(String entityType, Long entityId, String system, String key, String fragment,
			String fragmentText) throws GatewayException {
		if (queryGateway.getSource(projectName, system, key) == null) {
			recordSource(system, key, null, null, null);
		}
		Map<String, Object> request = new HashMap<>();
		request.put("projectName", projectName);
		request.put("entityType", entityType);
		request.put("entityId", entityId);
		request.put("system", system);
		request.put("externalId", key);
		request.put("fragment", fragment);
		request.put("fragmentText", fragmentText);
		gateway.execute(new GatewayRequest("LinkSource", request));
	}

	private EntitySourceLink linkOf(String key, String fragment) {
		ExternalSource source = provenanceStore.findSource(project.getId(), "jira", key).orElseThrow();
		return provenanceStore.linksForFragment(source.getId(), SourceLinkRelation.DERIVED_FROM,
				fragment, "Goal").get(0);
	}

	private void assertInvalid(org.junit.jupiter.api.function.Executable call) {
		GatewayException e = assertThrows(GatewayException.class, call);
		assertEquals(GatewayException.Kind.INVALID_INPUT, e.getKind(), e.getMessage());
	}

	private String goalText(UpsertFromSourceResultDto result) {
		return inTransaction(() -> getProjectRepository().findById(Goal.class, result.entityId())
				.getText());
	}

	private String json(java.util.function.Supplier<Object> read) {
		return inTransaction(() -> {
			try {
				return objectMapper.writeValueAsString(read.get());
			} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
				throw new IllegalStateException(e);
			}
		});
	}

	private <T> T inTransaction(java.util.function.Supplier<T> work) {
		return new TransactionTemplate(transactionManager).execute(status -> work.get());
	}

	private Long createGoal(String name, String text) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
		goalCmd.setEditedBy(admin);
		goalCmd.setGoalContainer(getProjectRepository().findById(Project.class, project.getId()));
		goalCmd.setName(name);
		goalCmd.setText(text);
		return getCommandHandler().execute(goalCmd).getGoal().getId();
	}

	private void editGoalText(Long goalId, String text) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
		Goal goal = getProjectRepository().findById(Goal.class, goalId);
		goalCmd.setEditedBy(admin);
		goalCmd.setGoal(goal);
		goalCmd.setName(goal.getName());
		goalCmd.setText(text);
		getCommandHandler().execute(goalCmd);
	}

	private Project createProject(String name) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
		projectCmd.setEditedBy(admin);
		projectCmd.setName(name);
		projectCmd.setText("entity provenance integration test project");
		projectCmd.setOrganizationName("ProvenanceTestOrg-" + System.nanoTime());
		return getCommandHandler().execute(projectCmd).getProject();
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
		cmd.setOrganizationName("ProvenanceTestOrg");
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
