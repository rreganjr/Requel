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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.assistant.legacynlp.StepSequenceAssistant;
import com.rreganjr.requel.command.AnalysisRequestDispatcher;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.DeleteScenarioStepCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditProjectAssistantSettingCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.StepImpl;
import com.rreganjr.validator.EntityValidationException;

/**
 * Issue #455: whether a scenario's steps alternate between an actor and the system, with the real
 * parser, through the real dispatch, run and apply path.
 */
public class StepSequenceAssistantIT extends AbstractLexicalAssistantTest {

	private static final String HOST_STARTS = "The host starts the livestream";
	private static final String SYSTEM_SHOWS = "The system shows the stream to the viewers";
	private static final String HOST_ENDS = "The host ends the livestream";
	private static final String SYSTEM_SAVES = "The system saves the recording";
	private static final String SYSTEM_EMAILS = "The system sends the recording to the host";
	private static final String HOST_SHARES = "The host shares the screen";
	private static final String MERGED = "The host starts the livestream and shares the screen";
	private static final String MERGE = "Merge into \"" + MERGED + "\".";

	@Autowired
	private StepSequenceAssistant assistant;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private AnalysisRequestDispatcher analysisRequestDispatcher;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Project project;

	@BeforeEach
	void fixture() throws Exception {
		ensureDictionaryLoaded();
		project = newProject("Turns");
		newActor("Host");
	}

	@Test
	void theHostActingAndTheSystemRespondingRaisesNothing() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, SYSTEM_SHOWS, HOST_ENDS, SYSTEM_SAVES));

		assertEquals(List.of(), analyze(scenario));
	}

	@Test
	void twoSystemStepsInARowRaiseAnIssueNamingBoth() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, SYSTEM_SHOWS, HOST_ENDS, SYSTEM_SAVES,
				SYSTEM_EMAILS));

		List<AnnotationAction> actions = analyze(scenario);

		AnnotationAction issue = actions.get(0);
		assertEquals("LOW", issue.severity());
		assertEquals("step-sequence", issue.metadata().get("findingType"));
		assertTrue(issue.text().contains("\"" + SYSTEM_SAVES + "\" and \"" + SYSTEM_EMAILS
				+ "\" are both system steps"), issue.text());
		assertEquals(List.of("Merge into \"The system saves the recording and sends the recording"
				+ " to the host\".", "Leave the steps as they are."), positions(actions));
	}

	@Test
	void aScenarioThatOpensWithASystemStepRaisesAnIssue() throws Exception {
		Scenario scenario = newScenario(List.of(SYSTEM_SHOWS, HOST_ENDS, SYSTEM_SAVES));

		assertTrue(analyze(scenario).get(0).text()
				.contains("it starts with the system step \"" + SYSTEM_SHOWS + "\""));
	}

	@Test
	void theSameActorTwiceInARowRaisesAnIssue() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, "The host shares the screen",
				SYSTEM_SHOWS));

		assertTrue(analyze(scenario).get(0).text().contains("\"" + HOST_STARTS
				+ "\" and \"The host shares the screen\" are both by Host"));
	}

	@Test
	void aReRunUpdatesTheIssueInPlace() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, SYSTEM_SAVES, SYSTEM_EMAILS));
		runQueued(scenario, null);
		Issue first = openSequenceIssue(scenario.getId());

		runPostEdit(scenario);

		Issue again = openSequenceIssue(scenario.getId());
		assertEquals(first.getId(), again.getId());
		assertEquals(1, sequenceIssues(scenario.getId()).size());
	}

	/**
	 * #486: rc5 sequence issues took the step-structure check's "Do nothing." (positions are
	 * shared by text). A re-run takes a shared position it no longer offers off its issue, and
	 * leaves it on the other issue. The shared position is manufactured natively: another
	 * scenario's merge linked to this scenario's issue.
	 */
	@Test
	void aReRunTakesASharedPositionItNoLongerOffersOffItsIssueOnly() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, SYSTEM_SAVES, SYSTEM_EMAILS));
		Scenario other = newScenario(List.of("The host joins the livestream",
				"The system saves the chat", "The system sends the chat to the host"));
		runQueued(scenario, null);
		runQueued(other, null);
		Issue issue = openSequenceIssue(scenario.getId());
		Issue otherIssue = openSequenceIssue(other.getId());
		java.util.Set<String> offered = positionTexts(issue);
		Position shared = otherIssue.getPositions().stream()
				.filter(position -> !offered.contains(position.getText())).findFirst()
				.orElseThrow();
		jdbcTemplate.update("insert into position_issue (position_id, issue_id) values (?, ?)",
				shared.getId(), issue.getId());
		assertTrue(positionTexts(openSequenceIssue(scenario.getId())).contains(shared.getText()));

		runPostEdit(scenario);

		assertEquals(offered, positionTexts(openSequenceIssue(scenario.getId())));
		assertTrue(positionTexts(openSequenceIssue(other.getId())).contains(shared.getText()),
				"the other issue keeps it");
	}

	/** Creating a scenario queues no step-sequence re-check: its own analysis covers it. */
	@Test
	void creatingAScenarioQueuesNoStepSequenceRun() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, SYSTEM_SAVES, SYSTEM_EMAILS));

		assertTrue(queuedRuns(scenario, StepSequenceAssistant.TASK_TYPE).isEmpty());
	}

	@Test
	void renamingAStepReChecksTheScenarioThatUsesIt() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, SYSTEM_SAVES, SYSTEM_EMAILS));
		runQueued(scenario, null);
		openSequenceIssue(scenario.getId());

		renameStep(stepNamed(scenario, SYSTEM_EMAILS), "The host downloads the recording");

		runQueued(scenario, StepSequenceAssistant.TASK_TYPE);
		assertEquals(List.of(), openSequenceIssues(scenario.getId()),
				"the scenario alternates now; the issue nobody argued on is removed");
	}

	@Test
	void deletingAStepReChecksTheScenarioThatUsedIt() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, SYSTEM_SAVES, SYSTEM_EMAILS));
		runQueued(scenario, null);
		openSequenceIssue(scenario.getId());

		deleteStep(stepNamed(scenario, SYSTEM_EMAILS));

		runQueued(scenario, StepSequenceAssistant.TASK_TYPE);
		assertEquals(List.of(), openSequenceIssues(scenario.getId()));
	}

	@Test
	void wholeProjectAnalysisRaisesIt() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, SYSTEM_SAVES, SYSTEM_EMAILS));

		analysisRequestDispatcher.dispatchProject(
				inTransaction(() -> getProjectRepository().findById(Project.class, project.getId())),
				projectUser());
		runQueued(scenario, null);

		openSequenceIssue(scenario.getId());
	}

	@Test
	void aProjectWithTheCheckSwitchedOffGetsNothing() throws Exception {
		EditProjectAssistantSettingCommand command = getProjectCommandFactory()
				.newEditProjectAssistantSettingCommand();
		command.setEditedBy(projectUser());
		command.setProject(project);
		command.setAssistantId(StepSequenceAssistant.ASSISTANT_ID);
		command.setEnabled(false);
		getCommandHandler().execute(command);

		Scenario scenario = newScenario(List.of(HOST_STARTS, SYSTEM_SAVES, SYSTEM_EMAILS));
		runQueued(scenario, null);

		assertEquals(List.of(), sequenceIssues(scenario.getId()));
	}

	// ---- #472: merging two steps ----------------------------------------------------------------

	@Test
	void acceptingTheMergeLeavesOneStepAndAReRunRaisesNothing() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, HOST_SHARES, SYSTEM_SHOWS));
		runQueued(scenario, null);
		Issue issue = openSequenceIssue(scenario.getId());
		assertEquals(java.util.Set.of(MERGE, "Leave the steps as they are."), positionTexts(issue));
		Long sharesId = stepNamed(scenario, HOST_SHARES).getId();

		resolve(issue, positionWithText(issue, MERGE), null, projectUser());

		assertEquals(List.of(MERGED, SYSTEM_SHOWS), stepNames(scenario));
		assertEquals(null, freshStep(sharesId), "the dropped step, used nowhere else, is deleted");
		assertTrue(sequenceIssues(scenario.getId()).stream().allMatch(Issue::isResolved));
		runQueued(scenario, null);
		assertEquals(List.of(), openSequenceIssues(scenario.getId()));
	}

	@Test
	void aMergeIsRefusedOnceTheSecondStepIsRenamed() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, HOST_SHARES, SYSTEM_SHOWS));
		runQueued(scenario, null);
		Issue issue = openSequenceIssue(scenario.getId());
		Position merge = positionWithText(issue, MERGE);
		renameStep(stepNamed(scenario, HOST_SHARES), "The host mutes the audience");

		assertThrows(EntityValidationException.class,
				() -> resolve(issue, merge, null, projectUser()));
		assertEquals(List.of(HOST_STARTS, "The host mutes the audience", SYSTEM_SHOWS),
				stepNames(scenario));
	}

	@Test
	void aMergeIsRefusedOnceTheStepsAreReordered() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, HOST_SHARES, SYSTEM_SHOWS));
		runQueued(scenario, null);
		Issue issue = openSequenceIssue(scenario.getId());
		Position merge = positionWithText(issue, MERGE);
		setSteps(scenario, List.of(stepNamed(scenario, HOST_SHARES),
				stepNamed(scenario, HOST_STARTS), stepNamed(scenario, SYSTEM_SHOWS)));

		assertThrows(EntityValidationException.class,
				() -> resolve(issue, merge, null, projectUser()));
		assertEquals(List.of(HOST_SHARES, HOST_STARTS, SYSTEM_SHOWS), stepNames(scenario));
	}

	@Test
	void aMergeLeavesAnotherScenarioThatUsesTheSecondStepAlone() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, HOST_SHARES, SYSTEM_SHOWS));
		Step shares = stepNamed(scenario, HOST_SHARES);
		Scenario other = newScenario(List.of());
		setSteps(other, List.of(shares));
		runQueued(scenario, null);
		Issue issue = openSequenceIssue(scenario.getId());

		resolve(issue, positionWithText(issue, MERGE), null, projectUser());

		assertEquals(List.of(MERGED, SYSTEM_SHOWS), stepNames(scenario));
		assertEquals(List.of(HOST_SHARES), stepNames(other));
		assertEquals(HOST_SHARES, freshStep(shares.getId()).getName());
	}

	@Test
	void aMergeIsRefusedOnceTheFirstStepIsShared() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, HOST_SHARES, SYSTEM_SHOWS));
		runQueued(scenario, null);
		Issue issue = openSequenceIssue(scenario.getId());
		Position merge = positionWithText(issue, MERGE);
		Scenario other = newScenario(List.of());
		setSteps(other, List.of(stepNamed(scenario, HOST_STARTS)));

		assertThrows(EntityValidationException.class,
				() -> resolve(issue, merge, null, projectUser()));
		assertEquals(List.of(HOST_STARTS, HOST_SHARES, SYSTEM_SHOWS), stepNames(scenario));
	}

	@Test
	void aSharedFirstStepGetsNoMerge() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, HOST_SHARES, SYSTEM_SHOWS));
		Scenario other = newScenario(List.of());
		setSteps(other, List.of(stepNamed(scenario, HOST_STARTS)));

		runQueued(scenario, null);

		assertEquals(java.util.Set.of("Leave the steps as they are."),
				positionTexts(openSequenceIssue(scenario.getId())));
	}

	@Test
	void aReRunKeepsTheSameMergePosition() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, HOST_SHARES, SYSTEM_SHOWS));
		runQueued(scenario, null);
		Long first = positionWithText(openSequenceIssue(scenario.getId()), MERGE).getId();

		runPostEdit(scenario);

		Issue issue = openSequenceIssue(scenario.getId());
		assertEquals(first, positionWithText(issue, MERGE).getId());
		assertEquals(2, issue.getPositions().size());
	}

	/** Two breaks, two merges; accepting one resolves the issue, and the re-run raises the other. */
	@Test
	void afterOneMergeTheReRunRaisesTheRemainingBreak() throws Exception {
		Scenario scenario = newScenario(List.of(HOST_STARTS, HOST_SHARES, SYSTEM_SHOWS,
				SYSTEM_SAVES));
		runQueued(scenario, null);
		Issue issue = openSequenceIssue(scenario.getId());
		assertEquals(3, issue.getPositions().size());

		resolve(issue, positionWithText(issue, MERGE), null, projectUser());
		runQueued(scenario, null);

		Issue remaining = openSequenceIssue(scenario.getId());
		assertTrue(remaining.getText().contains("are both system steps"), remaining.getText());
		assertFalse(remaining.getText().contains("are both by Host"), remaining.getText());
	}

	private java.util.Set<String> positionTexts(Issue issue) {
		return issue.getPositions().stream().map(Position::getText)
				.collect(Collectors.toSet());
	}

	private List<String> stepNames(Scenario scenario) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(ScenarioImpl.class, scenario.getId()).getSteps().stream()
					.map(Step::getName).toList();
		} finally {
			em.close();
		}
	}

	private Step freshStep(Long stepId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(StepImpl.class, stepId);
		} finally {
			em.close();
		}
	}

	/** Sets the scenario's steps to {@code steps}, existing ones, in that order. */
	private void setSteps(Scenario scenario, List<Step> steps) throws Exception {
		User user = projectUser();
		List<EditScenarioStepCommand> commands = new ArrayList<>();
		for (Step step : steps) {
			EditScenarioStepCommand command = getProjectCommandFactory().newEditScenarioStepCommand();
			command.setEditedBy(user);
			command.setProjectOrDomain(project);
			command.setStep(step);
			command.setName(step.getName());
			command.setScenarioTypeName(ScenarioType.Primary.name());
			commands.add(command);
		}
		EditScenarioCommand command = getProjectCommandFactory().newEditScenarioCommand();
		command.setEditedBy(user);
		command.setProjectOrDomain(project);
		command.setScenario(inTransaction(
				() -> getProjectRepository().findById(Scenario.class, scenario.getId())));
		command.setStepCommands(commands);
		getCommandHandler().execute(command);
	}

	// ---- helpers --------------------------------------------------------------------------------

	private List<AnnotationAction> analyze(Scenario scenario) {
		return inTransaction(() -> assistant.analyze(context(),
				getProjectRepository().findById(Scenario.class, scenario.getId())))
				.annotationActions();
	}

	private void runPostEdit(Scenario scenario) {
		Scenario loaded = inTransaction(
				() -> getProjectRepository().findById(Scenario.class, scenario.getId()));
		analysisRequestDispatcher.dispatch(loaded, projectUser(), null);
		runQueued(scenario, null);
	}

	/** Runs the latest QUEUED run on the scenario for {@code taskType} (null: post-edit). */
	private void runQueued(Scenario scenario, String taskType) {
		List<AssistantRunEntity> queued = queuedRuns(scenario, taskType);
		if (queued.isEmpty()) {
			throw new AssertionError("no QUEUED " + taskType + " run for scenario "
					+ scenario.getId());
		}
		assistantRunWorker.run(queued.get(queued.size() - 1).getRunId());
	}

	private List<AssistantRunEntity> queuedRuns(Scenario scenario, String taskType) {
		return assistantRunRepository.findAll().stream()
				.filter(run -> "Scenario".equals(run.getTargetType())
						&& scenario.getId().equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus())
						&& java.util.Objects.equals(taskType, run.getTaskType()))
				.toList();
	}

	private List<Issue> sequenceIssues(Long scenarioId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			ScenarioImpl scenario = em.find(ScenarioImpl.class, scenarioId);
			List<Issue> issues = scenario.getAnnotations().stream().map(Hibernate::unproxy)
					.filter(Issue.class::isInstance).map(Issue.class::cast)
					.filter(issue -> issue.getText() != null
							&& issue.getText().contains("don't alternate"))
					.collect(Collectors.toList());
			issues.forEach(issue -> {
				issue.isResolved();
				issue.getPositions().forEach(Position::getText);
			});
			return issues;
		} finally {
			em.close();
		}
	}

	private List<String> openSequenceIssues(Long scenarioId) {
		return sequenceIssues(scenarioId).stream().filter(issue -> !issue.isResolved())
				.map(Issue::getText).toList();
	}

	private Issue openSequenceIssue(Long scenarioId) {
		List<Issue> open = sequenceIssues(scenarioId).stream().filter(issue -> !issue.isResolved())
				.toList();
		assertEquals(1, open.size(), "one open step-sequence issue: "
				+ open.stream().map(Issue::getText).toList());
		return open.get(0);
	}

	private Step stepNamed(Scenario scenario, String name) {
		return inTransaction(() -> getProjectRepository().findById(Scenario.class, scenario.getId())
				.getSteps().stream().filter(step -> name.equals(step.getName())).findFirst()
				.orElseThrow());
	}

	private void renameStep(Step step, String name) throws Exception {
		EditScenarioStepCommand command = getProjectCommandFactory().newEditScenarioStepCommand();
		command.setEditedBy(projectUser());
		command.setProjectOrDomain(project);
		command.setStep(step);
		command.setName(name);
		getCommandHandler().execute(command);
	}

	private void deleteStep(Step step) throws Exception {
		DeleteScenarioStepCommand command = getProjectCommandFactory()
				.newDeleteScenarioStepCommand();
		command.setEditedBy(projectUser());
		command.setScenarioStep(step);
		getCommandHandler().execute(command);
	}

	private <T> T inTransaction(java.util.function.Supplier<T> work) {
		return new TransactionTemplate(transactionManager).execute(status -> work.get());
	}

	private static List<String> positions(List<AnnotationAction> actions) {
		return actions.stream()
				.filter(a -> a.actionType() == AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION)
				.map(AnnotationAction::text).toList();
	}

	private AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(3L, "project"),
				new UserRef(11L, "assistant"), EntityRef.of("Project", project.getId()), Locale.US,
				Clock.systemUTC(), Map.of());
	}

	private void newActor(String name) throws Exception {
		EditActorCommand command = getProjectCommandFactory().newEditActorCommand();
		command.setEditedBy(projectUser());
		command.setActorContainer(project);
		command.setName(name);
		command.setText("The " + name.toLowerCase(Locale.ROOT) + ".");
		getCommandHandler().execute(command);
	}

	private Scenario newScenario(List<String> stepNames) throws Exception {
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
		command.setName("Run the session " + stamp());
		command.setScenarioTypeName(ScenarioType.Primary.name());
		command.setStepCommands(steps);
		return getCommandHandler().execute(command).getScenario();
	}
}
