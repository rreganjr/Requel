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
package com.rreganjr.requel.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.hibernate.Hibernate;

import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.legacynlp.StepStructureAssistant;
import com.rreganjr.requel.command.AnalysisRequestDispatcher;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.platform.identity.User;

/**
 * Issue #349: how the step-structure check reads step and scenario names, with the real parser.
 * Each case runs the assistant on a saved step or scenario and reads the actions it returns.
 */
public class StepStructureAssistantIT extends AbstractLexicalAssistantTest {

	@Autowired
	private StepStructureAssistant assistant;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private AnalysisRequestDispatcher analysisRequestDispatcher;

	private Project project;

	@BeforeEach
	void fixture() throws Exception {
		ensureDictionaryLoaded();
		project = newProject("Steps");
		newActor(project, "Host");
	}

	@Test
	void aCommandStepGetsTheNoActorIssueAndNoNote() throws Exception {
		List<AnnotationAction> actions = analyzeStep("Start the livestream");
		AnnotationAction issue = only(actions, "no-actor");
		assertEquals("LOW", issue.severity());
		assertEquals(Boolean.FALSE, issue.metadata().get("mustResolve"));
		assertTrue(texts(actions).contains("Do nothing."), texts(actions).toString());
		assertTrue(actions.stream().noneMatch(
				a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_NOTE));
	}

	@Test
	void aStepNamingAKnownActorRaisesNothing() throws Exception {
		assertEquals(List.of(), findingTypes(analyzeStep("The host starts the livestream")));
	}

	@Test
	void aStepWhereTheSystemRespondsRaisesNothing() throws Exception {
		assertEquals(List.of(), findingTypes(analyzeStep("The system starts the livestream")));
	}

	@Test
	void aStepWithNoVerbGetsTheUnreadableIssue() throws Exception {
		List<AnnotationAction> actions = analyzeStep("Webinar");
		assertEquals("LOW", only(actions, "unreadable").severity());
		assertEquals(List.of(), findingTypes(analyzeStep(
				"[Ingested verbatim from the source, criterion 1.]")).stream()
				.filter("no-actor"::equals).toList());
	}

	@Test
	void aStepNamingAnUnknownSubjectGetsTheMustResolveIssue() throws Exception {
		assertEquals(List.of("subject-not-actor"),
				findingTypes(analyzeStep("The producer starts the stream")));
	}

	@Test
	void aCommandScenarioNameRaisesNothing() throws Exception {
		assertEquals(List.of(), findingTypes(analyzeScenario("Create the room")));
	}

	@Test
	void aScenarioNameWithNoVerbGetsTheUnreadableIssue() throws Exception {
		assertEquals(List.of("unreadable"), findingTypes(analyzeScenario("Webinar dry run")));
	}

	@Test
	void aScenarioNameNamingAKnownActorRaisesNothing() throws Exception {
		assertEquals(List.of(), findingTypes(analyzeScenario("The host runs the session")));
	}

	// ---- the project's name as the actor ----------------------------------------------------------

	@Test
	void aStepNamingTheProjectGetsTheProjectAsActorIssueWithItsThreePositions() throws Exception {
		project = namedProject("Roundtable");
		String name = project.getName();
		List<AnnotationAction> actions = analyzeStep(name + " starts the stream");
		AnnotationAction issue = only(actions, "project-as-actor");
		assertEquals(Boolean.TRUE, issue.metadata().get("mustResolve"));
		assertEquals("LEXICAL", issue.metadata().get("kind"));
		assertEquals(name, issue.metadata().get("word"));
		assertTrue(texts(actions).containsAll(List.of("Replace \"" + name + "\" with \"the system\".",
				"Add \"" + name + "\" as an actor.", "Do nothing.")), texts(actions).toString());
		assertTrue(findingTypes(actions).stream().noneMatch("subject-not-actor"::equals));

		// With an actor of that name, it is a known actor.
		newActor(project, name);
		assertEquals(List.of(), findingTypes(analyzeStep(name + " starts the livestream")));
	}

	@Test
	void replacingTheProjectNameWithTheSystemRewritesTheNameAndAReRunRaisesNothing()
			throws Exception {
		project = namedProject("Switchboard");
		Scenario scenario = newScenario(project.getName() + " runs the session", List.of());
		analyzeAndApply(scenario);

		LexicalIssue issue = projectAsActorIssue(scenario.getId());
		resolve(issue, positionWithText(issue, "Replace \"" + project.getName()
				+ "\" with \"the system\"."), null, projectUser());

		assertEquals("The system runs the session", freshScenarioName(scenario.getId()));
		analyzeAndApply(scenario);
		assertTrue(lexicalIssuesOn(scenario.getId()).stream()
				.noneMatch(i -> isProjectAsActor(i) && !i.isResolved()),
				"the rewrite leaves no project-as-actor issue open");
	}

	@Test
	void addingTheProjectNameAsAnActorCreatesTheActor() throws Exception {
		project = namedProject("Lighthouse");
		Scenario scenario = newScenario(project.getName() + " runs the session", List.of());
		analyzeAndApply(scenario);

		LexicalIssue issue = projectAsActorIssue(scenario.getId());
		resolve(issue, positionWithText(issue, "Add \"" + project.getName() + "\" as an actor."),
				null, projectUser());

		assertTrue(inTransaction(() -> getProjectRepository().findById(Project.class,
				project.getId()).getActors().stream()
				.anyMatch(actor -> actor.getName().equals(project.getName()))));
	}

	// ---- helpers --------------------------------------------------------------------------------

	/**
	 * A project named with one dictionary word, once per test class run. A made-up name would also
	 * raise a spelling issue on the same word, and a lexical issue is reused by word and property,
	 * so the two would share one issue.
	 */
	private Project namedProject(String name) throws Exception {
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(projectUser());
		command.setName(name);
		command.setOrganizationName("Org " + name + " " + stamp());
		return getCommandHandler().execute(command).getProject();
	}

	/** Run the step check on the scenario through the real dispatch, run and apply path. */
	private void analyzeAndApply(Scenario scenario) {
		Scenario loaded = inTransaction(
				() -> getProjectRepository().findById(Scenario.class, scenario.getId()));
		analysisRequestDispatcher.dispatch(loaded, projectUser(), null);
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> "Scenario".equals(run.getTargetType())
						&& scenario.getId().equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED run for " + scenario.getId()));
		assistantRunWorker.run(queued.getRunId());
	}

	private LexicalIssue projectAsActorIssue(Long scenarioId) {
		return lexicalIssuesOn(scenarioId).stream()
				.filter(issue -> isProjectAsActor(issue) && !issue.isResolved())
				.findFirst().orElseThrow(() -> new AssertionError("no project-as-actor issue: "
						+ lexicalIssuesOn(scenarioId).stream().map(LexicalIssue::getText).toList()));
	}

	private static boolean isProjectAsActor(LexicalIssue issue) {
		return issue.getText() != null && issue.getText().contains("names the project as its actor");
	}

	private List<LexicalIssue> lexicalIssuesOn(Long scenarioId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			ScenarioImpl scenario = em.find(ScenarioImpl.class, scenarioId);
			List<LexicalIssue> issues = scenario.getAnnotations().stream().map(Hibernate::unproxy)
					.filter(LexicalIssue.class::isInstance).map(LexicalIssue.class::cast)
					.collect(Collectors.toList());
			issues.forEach(issue -> {
				issue.isResolved();
				Hibernate.initialize(issue.getPositions());
			});
			return issues;
		} finally {
			em.close();
		}
	}

	private String freshScenarioName(Long scenarioId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(ScenarioImpl.class, scenarioId).getName();
		} finally {
			em.close();
		}
	}

	private List<AnnotationAction> analyzeStep(String name) throws Exception {
		Scenario scenario = newScenario("Holder " + stamp(), List.of(name));
		return inTransaction(() -> {
			Scenario loaded = getProjectRepository().findById(Scenario.class, scenario.getId());
			Step step = loaded.getSteps().get(0);
			return assistant.analyze(context(), step).annotationActions();
		});
	}

	private List<AnnotationAction> analyzeScenario(String name) throws Exception {
		Scenario scenario = newScenario(name, List.of());
		return inTransaction(() -> assistant.analyze(context(),
				getProjectRepository().findById(Scenario.class, scenario.getId()))
				.annotationActions());
	}

	private <T> T inTransaction(java.util.function.Supplier<T> work) {
		return new TransactionTemplate(transactionManager).execute(status -> work.get());
	}

	private static AnnotationAction only(List<AnnotationAction> actions, String findingType) {
		List<AnnotationAction> issues = actions.stream()
				.filter(a -> findingType.equals(a.metadata().get("findingType"))).toList();
		assertEquals(1, issues.size(), "expected one " + findingType + " issue: " + texts(actions));
		return issues.get(0);
	}

	private static List<String> findingTypes(List<AnnotationAction> actions) {
		return actions.stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE
						|| a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_NOTE)
				.map(a -> String.valueOf(a.metadata().get("findingType")))
				.collect(Collectors.toList());
	}

	private static List<String> texts(List<AnnotationAction> actions) {
		return actions.stream().map(AnnotationAction::text).toList();
	}

	private AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "project"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", project.getId()), Locale.US,
				Clock.systemUTC(), Map.of());
	}

	private void newActor(Project project, String name) throws Exception {
		EditActorCommand command = getProjectCommandFactory().newEditActorCommand();
		command.setEditedBy(projectUser());
		command.setActorContainer(project);
		command.setName(name);
		command.setText("The " + name.toLowerCase(Locale.ROOT) + ".");
		getCommandHandler().execute(command);
	}

	private Scenario newScenario(String name, List<String> stepNames) throws Exception {
		User user = projectUser();
		List<EditScenarioStepCommand> steps = new ArrayList<>();
		for (String stepName : stepNames) {
			EditScenarioStepCommand step = getProjectCommandFactory().newEditScenarioStepCommand();
			step.setEditedBy(user);
			step.setProjectOrDomain(project);
			step.setName(stepName);
			step.setText("");
			step.setScenarioTypeName(ScenarioType.Primary.name());
			steps.add(step);
		}
		EditScenarioCommand command = getProjectCommandFactory().newEditScenarioCommand();
		command.setEditedBy(user);
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setScenarioTypeName(ScenarioType.Primary.name());
		command.setStepCommands(steps);
		return getCommandHandler().execute(command).getScenario();
	}
}
