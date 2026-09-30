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

package com.rreganjr.requel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.IssueSeverity;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProvenanceStore;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.SourceLinkRelation;
import com.rreganjr.requel.project.SourceLocatorType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.AddScenarioToUseCaseCommand;
import com.rreganjr.requel.project.command.DeleteReportGeneratorCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditReportGeneratorCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.ExportProjectCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.impl.BuiltinReportGenerators;
import com.rreganjr.requel.project.impl.ReportGeneratorImpl;
import com.rreganjr.requel.project.impl.repository.init.BuiltinReportGeneratorUpgrader;
import com.rreganjr.requel.service.api.dto.ErrorResponse;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.query.ProjectQueryController;
import com.rreganjr.requel.user.User;

/**
 * Issue #275: emit through ReportGenerator, end to end — the bundled ticket generator over a
 * project built through the commands, the project version, failures, the bundled generators on
 * new, imported and existing projects, and the export changes the generators depend on.
 */
public class ReportGenerationIT extends AbstractIntegrationTestCase {

	private static final String TICKET = BuiltinReportGenerators.TICKET_MARKDOWN.key();
	private static final String HTML = BuiltinReportGenerators.PROJECT_HTML.key();

	@Autowired
	private ProjectQueryController projectQueryController;

	@Autowired
	private ProvenanceStore provenanceStore;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private com.rreganjr.requel.assistant.core.AssistantRunWorker assistantRunWorker;

	@Autowired
	private com.rreganjr.requel.assistant.core.persistence.AssistantRunRepository assistantRunRepository;

	private User admin;
	private Project project;
	private Goal replaceable;
	private Goal invisible;
	private UseCase useCase;
	private Scenario additional;

	@BeforeEach
	void setUpFixture() throws Exception {
		initializeBaselineData();
		admin = getUserRepository().findUserByUsername("admin");
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin", "x", List.of()));
		project = createProject("emit-" + System.nanoTime());

		replaceable = goal("Operators are replaceable",
				"More than one person can fill the Operator role.");
		invisible = goal("Viewers stay invisible", "Viewers are never shown to the panel.");
		Actor operator = actor("Operator", "Runs a live room.");
		actor("Viewer", "Watches the relay.");
		useCase = useCase("End a room", "An operator ends a room.", operator.getName(),
				"Open the admin console", "Press End");
		Scenario sub = scenario("Confirm the end", List.of(newStep("Confirm in the dialog")));
		additional = scenario("End from the room list", List.of(
				existingStep(useCase.getScenario().getSteps().get(1)), existingStep(sub)));
		addScenario(useCase, additional);
		GlossaryTerm webinar = term("webinar", "A Zoom webinar.", null);
		term("livestream", "The viewer relay.", webinar);

		issue(replaceable, "Who holds the second seat?", IssueSeverity.HIGH);
		issue(invisible, "Is the watch page public?", IssueSeverity.LOW);

		inTransaction(() -> {
			ExternalSource ticket = provenanceStore.recordSource(new ProvenanceStore.SourceSpec(
					project.getId(), "jira", "CON-1", SourceLocatorType.URL,
					"https://example.com/browse/CON-1", "CON-1", null, "ticket", null), admin)
					.source();
			ExternalSource guide = provenanceStore.recordSource(new ProvenanceStore.SourceSpec(
					project.getId(), "doc", "guide", SourceLocatorType.PATH, "docs/guide.pdf",
					"Production Guide", null, "guide", "Authoritative for behaviour."), admin)
					.source();
			provenanceStore.addAuthority(ticket, guide, "the guide wins", admin);
			provenanceStore.link(new ProvenanceStore.LinkSpec(ticket,
					SourceLinkRelation.DERIVED_FROM, "Goal", replaceable.getId(), "AC1", null, null,
					null), admin);
			return null;
		});
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	// --- the ticket generator ------------------------------------------------------------------

	@Test
	void theTicketGeneratorRendersEveryStructuralSection() throws Exception {
		ResponseEntity<?> response = run(generator(TICKET));
		assertEquals(200, response.getStatusCode().value());
		assertEquals("text/markdown;charset=UTF-8",
				response.getHeaders().getContentType().toString());
		assertTrue(response.getHeaders().getFirst("Content-Disposition").endsWith(".md\""));
		String ticket = body(response);

		assertTrue(ticket.startsWith("# " + project.getName() + "\n"), ticket);
		assertTrue(ticket.contains("1. **Operators are replaceable** — More than one person"), ticket);
		assertTrue(ticket.contains("2. **Viewers stay invisible**"), ticket);
		assertTrue(ticket.contains("- **Operator** — Runs a live room."), ticket);
		assertTrue(ticket.contains("### End a room"), ticket);
		assertTrue(ticket.contains("Primary actor: Operator"), ticket);
		assertTrue(ticket.contains("**Scenario: End from the room list**\n\n1. Press End\n"
				+ "2. Confirm the end\n   1. Confirm in the dialog\n"), ticket);
		assertTrue(ticket.contains("- **livestream** — The viewer relay. (see **webinar**)"), ticket);
		assertTrue(ticket.indexOf("**HIGH** Who holds the second seat?")
				< ticket.indexOf("**LOW** Is the watch page public?"), ticket);
		assertTrue(ticket.contains("- **CON-1** (ticket) — jira CON-1, https://example.com/browse/CON-1\n"
				+ "  - Defers to: Production Guide (the guide wins)\n"
				+ "  - Derived: Operators are replaceable (AC1)\n"), ticket);
		assertTrue(ticket.contains("Sources: Production Guide, CON-1"), ticket);
	}

	@Test
	void traceabilityCountsMatchTheProjectSummary() throws Exception {
		String ticket = body(run(generator(TICKET)));
		ProjectDto summary = inTransaction(() -> projectQueryController.toProjectDto(
				getProjectRepository().findProjectByName(project.getName())));
		String expected = "Requel project \"" + project.getName() + "\": " + summary.goalCount()
				+ " goals, " + summary.actorCount() + " actors, " + summary.useCaseCount()
				+ " use cases, " + summary.scenarioCount() + " scenarios, 3 steps, "
				+ summary.storyCount() + " stories, " + summary.glossaryTermCount()
				+ " glossary terms, ";
		assertTrue(ticket.contains(expected), expected + "\n---\n" + ticket);
	}

	@Test
	void anUnchangedProjectRendersByteIdentically() throws Exception {
		byte[] first = bytes(run(generator(TICKET)));
		byte[] second = bytes(run(generator(TICKET)));
		assertEquals(new String(first, StandardCharsets.UTF_8),
				new String(second, StandardCharsets.UTF_8));
	}

	@Test
	void theVersionChangesWithTheContentAndComesBackWhenTheEditIsReverted() throws Exception {
		String original = version(body(run(generator(TICKET))));
		assertEquals(12, original.length(), original);

		editGoalText(replaceable, "More than two people can fill the Operator role.");
		String edited = version(body(run(generator(TICKET))));
		assertNotEquals(original, edited);

		editGoalText(replaceable, "More than one person can fill the Operator role.");
		assertEquals(original, version(body(run(generator(TICKET)))),
				"the fingerprint ignores JPA versions, so a reverted edit is the original version");
	}

	@Test
	void theHtmlGeneratorIsHtmlAndNamesTheVersion() throws Exception {
		ResponseEntity<?> response = run(generator(HTML));
		assertEquals("text/html;charset=UTF-8", response.getHeaders().getContentType().toString());
		String html = body(response);
		assertTrue(html.contains("Project version: " + version(body(run(generator(TICKET))))), html);
		assertFalse(html.contains("Revision:"), html);
	}

	@Test
	void aGeneratorAskingForAMissingEntityIsA422NamingIt() throws Exception {
		ReportGenerator broken = createGenerator("Needs a missing goal", """
				<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				    xmlns:rp="http://www.rreganjr.com/requel">
				  <xsl:output method="text"/>
				  <xsl:template match="/rp:project">
				    <xsl:if test="count(rp:goals/rp:goal[rp:name = 'Blocking check']) = 0">
				      <xsl:message terminate="yes">The report references goal named Blocking check, which is not in the project.</xsl:message>
				    </xsl:if>
				  </xsl:template>
				</xsl:stylesheet>
				""", null);
		ResponseEntity<?> response = run(broken);
		assertEquals(422, response.getStatusCode().value());
		ErrorResponse error = (ErrorResponse) response.getBody();
		assertEquals("REPORT_FAILED", error.error());
		assertEquals("Report \"Needs a missing goal\" failed: The report references goal named "
				+ "Blocking check, which is not in the project.", error.message());
	}

	@Test
	void aTemplateErrorIsA422AndLeavesNoExportFileBehind() throws Exception {
		File tmp = new File(System.getProperty("java.io.tmpdir"));
		Set<String> before = exportFiles(tmp);
		ResponseEntity<?> response = run(createGenerator("Not XSLT", "not a stylesheet", null));
		assertEquals(422, response.getStatusCode().value());
		run(generator(TICKET));
		assertEquals(before, exportFiles(tmp), "the temporary export is deleted, on failure too");
	}

	// --- bundled generators --------------------------------------------------------------------

	@Test
	void aNewProjectGetsEveryBundledGeneratorKeyed() {
		Map<String, String> keyed = inTransaction(() -> reloaded().getReportGenerators().stream()
				.filter(r -> r.getBuiltinKey() != null)
				.collect(Collectors.toMap(ReportGenerator::getBuiltinKey, ReportGenerator::getName)));
		assertEquals(Map.of(HTML, "HTML Specification", TICKET, "Ticket (Markdown)"), keyed);
	}

	@Test
	void newTextDetachesABundledGeneratorAndARenameDoesNot() throws Exception {
		ReportGenerator html = generator(HTML);
		EditReportGeneratorCommand rename = getProjectCommandFactory().newEditReportGeneratorCommand();
		rename.setEditedBy(admin);
		rename.setProjectOrDomain(project);
		rename.setReportGenerator(html);
		rename.setName("Spec");
		rename.setText(html.getText());
		getCommandHandler().execute(rename);
		assertEquals(HTML, inTransaction(() -> getProjectRepository()
				.findById(ReportGenerator.class, html.getId()).getBuiltinKey()));

		EditReportGeneratorCommand edit = getProjectCommandFactory().newEditReportGeneratorCommand();
		edit.setEditedBy(admin);
		edit.setProjectOrDomain(project);
		edit.setReportGenerator(getProjectRepository().findById(ReportGenerator.class, html.getId()));
		edit.setText(html.getText() + "<!-- mine -->");
		getCommandHandler().execute(edit);
		assertNull(inTransaction(() -> getProjectRepository()
				.findById(ReportGenerator.class, html.getId()).getBuiltinKey()));
	}

	@Test
	void theUpgraderAdoptsUneditedCopiesAddsMissingOnesAndLeavesEditedOnes() throws Exception {
		// A project from before #275: an unkeyed CRLF copy of the HTML bundle, no ticket generator.
		ReportGenerator html = generator(HTML);
		inTransaction(() -> {
			ReportGeneratorImpl impl = (ReportGeneratorImpl) getProjectRepository()
					.findById(ReportGenerator.class, html.getId());
			impl.setBuiltinKey(null);
			impl.setText(BuiltinReportGenerators.PROJECT_HTML.text().replace("\n", "\r\n"));
			return null;
		});
		DeleteReportGeneratorCommand delete = getProjectCommandFactory()
				.newDeleteReportGeneratorCommand();
		delete.setEditedBy(admin);
		delete.setReportGenerator(generator(TICKET));
		getCommandHandler().execute(delete);
		// And a second project whose HTML copy was edited.
		Project other = createProject("emit-edited-" + System.nanoTime());
		Long editedId = inTransaction(() -> {
			ReportGeneratorImpl impl = (ReportGeneratorImpl) getProjectRepository()
					.findProjectByName(other.getName()).getReportGenerators().stream()
					.filter(r -> HTML.equals(r.getBuiltinKey())).findFirst().orElseThrow();
			impl.setBuiltinKey(null);
			impl.setText(impl.getText() + "<!-- edited -->");
			return impl.getId();
		});

		upgrader().initialize();
		upgrader().initialize(); // idempotent

		List<String> keys = inTransaction(() -> reloaded().getReportGenerators().stream()
				.map(ReportGenerator::getBuiltinKey).sorted().toList());
		assertEquals(List.of(HTML, TICKET), keys, "adopted the copy, added the ticket generator once");
		assertNull(inTransaction(() -> getProjectRepository()
				.findById(ReportGenerator.class, editedId).getBuiltinKey()), "edited copy left alone");
	}

	// --- export ------------------------------------------------------------------------------

	@Test
	void anExportRoundTripKeepsBundledKeysAndAdditionalScenarios() throws Exception {
		byte[] xml = export(project);
		String text = new String(xml, StandardCharsets.UTF_8);
		assertTrue(text.contains("builtin=\"" + TICKET + "\""), "the export carries the key");
		assertTrue(text.contains("<additionalScenarios>"), "and the additional scenarios");

		ImportProjectCommand command = getProjectCommandFactory().newImportProjectCommand();
		command.setAnalysisEnabled(false);
		command.setEditedBy(admin);
		String importedName = "emit-imported-" + System.nanoTime();
		command.setName(importedName);
		command.setInputStream(new ByteArrayInputStream(xml));
		getCommandHandler().execute(command);

		inTransaction(() -> {
			Project imported = getProjectRepository().findProjectByName(importedName);
			assertEquals(Set.of(HTML, TICKET), imported.getReportGenerators().stream()
					.map(ReportGenerator::getBuiltinKey).collect(Collectors.toSet()),
					"keys kept and no duplicate bundled generators added");
			UseCase importedUseCase = imported.getUseCases().stream()
					.filter(u -> u.getName().equals(useCase.getName())).findFirst().orElseThrow();
			assertEquals(List.of(additional.getName()), importedUseCase.getAdditionalScenarios()
					.stream().map(Scenario::getName).toList());
			return null;
		});
	}

	@Test
	void anIssueStaleOnAnEntityCarriesItInTheExport() throws Exception {
		ensureDictionaryLoaded();
		// "groal" is a deliberate misspelling: the legacy lexical assistant raises a finding on it.
		Goal groal = goal("Hand-over groal " + System.nanoTime(), "a clear requirement.");
		runLatestQueuedRun(groal.getId());
		Long issueId = inTransaction(() -> getProjectRepository().findById(Goal.class, groal.getId())
				.getAnnotations().stream()
				.filter(a -> a.getText() != null && a.getText().contains("groal"))
				.map(a -> a.getId()).findFirst()
				.orElseThrow(() -> new AssertionError("expected a finding on 'groal'")));
		String staleOn = "staleOn=\"GOL_" + groal.getId() + "\"";
		assertFalse(annotationElement(export(project), issueId).contains("staleOn"),
				"fresh right after the run");

		editGoalText(groal, "a clearer requirement.");
		assertTrue(annotationElement(export(project), issueId).contains(staleOn),
				"the edit makes it stale on that goal");
	}

	// --- helpers -------------------------------------------------------------------------------

	/** The opening tag of the exported annotation with this id. */
	private static String annotationElement(byte[] xml, Long annotationId) {
		String text = new String(xml, StandardCharsets.UTF_8);
		int at = text.indexOf("id=\"ANN_" + annotationId + "\"");
		assertTrue(at >= 0, "annotation " + annotationId + " is in the export");
		return text.substring(text.lastIndexOf('<', at), text.indexOf('>', at) + 1);
	}

	private void runLatestQueuedRun(Long goalId) {
		com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity queued = assistantRunRepository
				.findAll().stream()
				.filter(run -> "Goal".equals(run.getTargetType()) && goalId.equals(run.getTargetId())
						&& "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED assistant run for the goal"));
		assistantRunWorker.run(queued.getRunId());
	}

	/**
	 * The call inside a transaction, as open-session-in-view gives a real request, rolled back
	 * because a run only reads (and a failing one leaves the transaction rollback-only).
	 */
	private ResponseEntity<?> run(ReportGenerator generator) {
		return new TransactionTemplate(transactionManager).execute(status -> {
			status.setRollbackOnly();
			return projectQueryController.runReport(project.getName(), generator.getId());
		});
	}

	private static String body(ResponseEntity<?> response) {
		assertEquals(200, response.getStatusCode().value(), String.valueOf(response.getBody()));
		return new String((byte[]) response.getBody(), StandardCharsets.UTF_8);
	}

	private static byte[] bytes(ResponseEntity<?> response) {
		assertEquals(200, response.getStatusCode().value(), String.valueOf(response.getBody()));
		return (byte[]) response.getBody();
	}

	private static String version(String rendered) {
		int at = rendered.indexOf("version: ");
		assertTrue(at >= 0, rendered);
		int from = at + "version: ".length();
		int to = from;
		while (to < rendered.length() && Character.isLetterOrDigit(rendered.charAt(to))) {
			to++;
		}
		return rendered.substring(from, to);
	}

	private static Set<String> exportFiles(File dir) {
		String[] names = dir.list((d, name) -> name.startsWith("projectExport")
				&& name.endsWith(".xml"));
		return names == null ? Set.of() : Set.of(names);
	}

	private ReportGenerator generator(String key) {
		return inTransaction(() -> reloaded().getReportGenerators().stream()
				.filter(r -> key.equals(r.getBuiltinKey())).findFirst()
				.orElseThrow(() -> new AssertionError("no bundled generator " + key)));
	}

	private Project reloaded() {
		return getProjectRepository().findProjectByName(project.getName());
	}

	private BuiltinReportGeneratorUpgrader upgrader() {
		return applicationContext.getBean(BuiltinReportGeneratorUpgrader.class);
	}

	private byte[] export(Project exported) {
		return inTransaction(() -> {
			try {
				ExportProjectCommand command = getProjectCommandFactory().newExportProjectCommand();
				command.setProject(getProjectRepository().findProjectByName(exported.getName()));
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				command.setOutputStream(out);
				getCommandHandler().execute(command);
				return out.toByteArray();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
	}

	private <T> T inTransaction(Supplier<T> work) {
		return new TransactionTemplate(transactionManager).execute(status -> work.get());
	}

	private Project createProject(String name) throws Exception {
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(name);
		cmd.setText("emit integration test");
		cmd.setOrganizationName("EmitOrg-" + System.nanoTime());
		return getCommandHandler().execute(cmd).getProject();
	}

	private ReportGenerator createGenerator(String name, String text, String key) throws Exception {
		EditReportGeneratorCommand cmd = getProjectCommandFactory().newEditReportGeneratorCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText(text);
		if (key != null) {
			cmd.setBuiltinKey(key);
		}
		return getCommandHandler().execute(cmd).getReportGenerator();
	}

	private Goal goal(String name, String text) throws Exception {
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getGoal();
	}

	private void editGoalText(Goal goal, String text) throws Exception {
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoal(getProjectRepository().findById(Goal.class, goal.getId()));
		cmd.setText(text);
		getCommandHandler().execute(cmd);
	}

	private Actor actor(String name, String text) throws Exception {
		EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
		cmd.setEditedBy(admin);
		cmd.setActorContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		return getCommandHandler().execute(cmd).getActor();
	}

	private EditScenarioStepCommand newStep(String name) {
		EditScenarioStepCommand cmd = getProjectCommandFactory().newEditScenarioStepCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText("Text for " + name);
		cmd.setScenarioTypeName(ScenarioType.Primary.name());
		return cmd;
	}

	private EditScenarioStepCommand existingStep(com.rreganjr.requel.project.Step step) {
		EditScenarioStepCommand cmd = getProjectCommandFactory().newEditScenarioStepCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setStep(step);
		return cmd;
	}

	private UseCase useCase(String name, String text, String primaryActorName, String... stepNames)
			throws Exception {
		List<EditScenarioStepCommand> steps = new ArrayList<>();
		for (String stepName : stepNames) {
			steps.add(newStep(stepName));
		}
		EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText(text);
		cmd.setPrimaryActorName(primaryActorName);
		cmd.setStepCommands(steps);
		return getCommandHandler().execute(cmd).getUseCase();
	}

	private Scenario scenario(String name, List<EditScenarioStepCommand> steps) throws Exception {
		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText("Scenario " + name);
		cmd.setScenarioTypeName(ScenarioType.Primary.name());
		cmd.setStepCommands(steps);
		return getCommandHandler().execute(cmd).getScenario();
	}

	private void addScenario(UseCase target, Scenario scenario) throws Exception {
		AddScenarioToUseCaseCommand cmd = getProjectCommandFactory().newAddScenarioToUseCaseCommand();
		cmd.setEditedBy(admin);
		cmd.setUseCase(target);
		cmd.setScenario(scenario);
		getCommandHandler().execute(cmd);
	}

	private GlossaryTerm term(String name, String text, GlossaryTerm canonical) throws Exception {
		EditGlossaryTermCommand cmd = getProjectCommandFactory().newEditGlossaryTermCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText(text);
		if (canonical != null) {
			cmd.setCanonicalTerm(canonical);
		}
		return getCommandHandler().execute(cmd).getGlossaryTerm();
	}

	private void issue(Goal goal, String text, IssueSeverity severity) throws Exception {
		EditIssueCommand cmd = getAnnotationCommandFactory().newEditIssueCommand();
		cmd.setEditedBy(admin);
		cmd.setGroupingObject(project);
		cmd.setAnnotatable(goal);
		cmd.setText(text);
		cmd.setMustBeResolved(true);
		cmd.setSeverity(severity);
		getCommandHandler().execute(cmd);
	}
}
