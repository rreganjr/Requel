/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.repository.jpa.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.command.ExportProjectCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.repository.init.StakeholderPermissionsInitializer;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;

import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.repository.init.AdminUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.AssistantUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.ProjectUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.UserRolePermissionsInitializer;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import com.rreganjr.requel.user.command.UserCommandFactory;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Regression coverage that builds a project with sample data, exports it, and verifies that re-importing
 * the XML via the streaming StAX importer produces an equivalent project structure.
 */
@SpringBootTest(classes = Application.class)
@TestPropertySource(locations = "classpath:db.properties")
@org.springframework.test.context.ActiveProfiles("test")
class ProjectXmlStreamingRoundTripIT {

	private static final String SAMPLE_PROJECT_NAME_PREFIX = "Regression Project ";

	@Autowired
	private ProjectCommandFactory projectCommandFactory;

	@Autowired
	private CommandHandler commandHandler;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRolePermissionsInitializer userRolePermissionsInitializer;

	@Autowired
	private StakeholderPermissionsInitializer stakeholderPermissionsInitializer;

	@Autowired
	private AdminUserInitializer adminUserInitializer;

	@Autowired
	private AssistantUserInitializer assistantUserInitializer;

	@Autowired
	private ProjectUserInitializer projectUserInitializer;

	@Autowired
	private AnnotationCommandFactory annotationCommandFactory;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private UserCommandFactory userCommandFactory;

	@Autowired
	private com.rreganjr.requel.tagging.command.TagCommandFactory tagCommandFactory;

	@Autowired
	private com.rreganjr.requel.tagging.TagRepository tagRepository;

	@Autowired
	private com.rreganjr.nlp.dictionary.DictionaryRepository dictionaryRepository;

	@Autowired
	private com.rreganjr.requel.project.IgnoredFindingStore ignoredFindingStore;

	@jakarta.persistence.PersistenceContext
	private jakarta.persistence.EntityManager entityManager;

	@Test
	@Transactional
	void importExportRoundTripKeepsProjectRoundTrippable() throws Exception {
		initializeBaselineData();

		User projectUser = ensureProjectUserExists();
		Project originalProject = createSampleProject(projectUser);
		Scenario standaloneScenario = originalProject.getScenarios()
				.stream()
				.filter(scenario -> scenario.getName() != null && scenario.getName().startsWith("Standalone Scenario"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Standalone scenario not created"));
		String standaloneScenarioName = standaloneScenario.getName();
		String standaloneScenarioStepName = standaloneScenario.getSteps()
				.iterator()
				.next()
				.getName();
		ProjectSnapshot originalSnapshot = snapshotProject(originalProject);
		System.out.println("Original project annotations: " + originalSnapshot.annotationCount());

		byte[] exportedBytes = exportProject(originalProject);
		assertThat(exportedBytes)
				.as("Exported XML payload")
				.isNotEmpty();
		String exportedXmlString = new String(exportedBytes, StandardCharsets.UTF_8);
		assertThat(exportedXmlString)
				.as("Exported XML content")
				.contains("<project")
				.contains("<stakeholders>")
				.contains("<goals>")
				.contains("<actors>")
				.contains(standaloneScenarioName)
				.contains(standaloneScenarioStepName)
				.contains("<password>")
				.contains("<passwordSalt>")
				.contains("<passwordEncryptingAlgorithm>")
				.contains("<passwordEncryptingIterations>");
		assertXmlMatchesProjectSchema(exportedBytes);

		String reimportedProjectName = originalProject.getName() + " Reimport " + Instant.now().toEpochMilli();
		Project reimportedProject = importProject(exportedBytes, projectUser, reimportedProjectName);
		ProjectSnapshot reimportedSnapshot = snapshotProject(reimportedProject);
		System.out.println("Re-imported project annotations: " + reimportedSnapshot.annotationCount());
		assertSnapshotsEquivalent(originalSnapshot, reimportedSnapshot);
	}

	@Test
	@Transactional
	void tagsRoundTripByName() throws Exception {
		initializeBaselineData();
		User projectUser = ensureProjectUserExists();
		Project originalProject = createSampleProject(projectUser);
		Goal goal = originalProject.getGoals().stream().findFirst()
				.orElseThrow(() -> new AssertionError("sample project has no goal"));

		// A namespaced project-scoped tag and a global tag, both on the goal.
		assignImportedTagForTest(projectUser, originalProject, goal, "type", "business-rule");
		assignImportedTagForTest(projectUser, null, goal, "scope", "shared");
		entityManager.flush();

		byte[] exportedBytes = exportProject(originalProject);
		String xml = new String(exportedBytes, StandardCharsets.UTF_8);
		assertThat(xml).as("exported XML tag tokens")
				.contains("<tags>")
				.contains("type:business-rule")
				.contains("scope:shared");
		assertXmlMatchesProjectSchema(exportedBytes);

		String reName = originalProject.getName() + " TagReimport " + Instant.now().toEpochMilli();
		Project reimported = importProject(exportedBytes, projectUser, reName);
		entityManager.flush();
		Goal reGoal = reimported.getGoals().stream().findFirst()
				.orElseThrow(() -> new AssertionError("reimported project has no goal"));

		Set<String> tokens = tagRepository.findTagsOnEntity("Goal", reGoal.getId()).stream()
				.map(t -> new com.rreganjr.requel.tagging.TagToken(
						t.getCategory(), t.getValue(), t.getColor()).toToken())
				.collect(Collectors.toSet());
		assertThat(tokens).as("tags on the reimported goal")
				.contains("type:business-rule", "scope:shared");

		// The global tag was resolved by key on import, not duplicated.
		assertThat(tagRepository.findTag(null, "scope", "shared"))
				.as("global tag resolved by key").isNotNull();
	}

	/**
	 * The project's own dictionary travels with the project file (issue #313).
	 * <p>
	 * The words are written and read through DictionaryRepository, not through the project's
	 * collection — that collection is a read-only export view. So this covers the whole path:
	 * repository write, JAXB export off the collection, STAX read on import, repository write
	 * against the new project's id.
	 */
	@Test
	@Transactional
	void projectDictionaryWordsRoundTrip() throws Exception {
		initializeBaselineData();
		User projectUser = ensureProjectUserExists();
		Project originalProject = createSampleProject(projectUser);

		dictionaryRepository.addToDictionary(originalProject.getId(), "requelspeak");
		dictionaryRepository.addToDictionary(originalProject.getId(), "Elicitron");
		entityManager.flush();

		byte[] exportedBytes = exportProject(originalProject);
		String xml = new String(exportedBytes, StandardCharsets.UTF_8);
		assertThat(xml).as("exported XML dictionary block")
				.contains("<dictionary>")
				.contains("requelspeak")
				.contains("Elicitron");
		assertXmlMatchesProjectSchema(exportedBytes);

		String reName = originalProject.getName() + " DictReimport " + Instant.now().toEpochMilli();
		Project reimported = importProject(exportedBytes, projectUser, reName);
		entityManager.flush();

		Set<String> lemmas = dictionaryRepository.findProjectWords(reimported.getId()).stream()
				.map(com.rreganjr.nlp.dictionary.ProjectDictionaryWord::getLemma)
				.collect(Collectors.toSet());
		assertThat(lemmas).as("dictionary words on the reimported project")
				.containsExactlyInAnyOrder("requelspeak", "Elicitron");

		// The original keeps its own copy: the import added words to the new project, it did not
		// move them.
		assertThat(dictionaryRepository.findProjectWords(originalProject.getId())).hasSize(2);

		// And the words are known in each project, which is the point of the layer.
		assertThat(dictionaryRepository.isKnownWord(reimported.getId(), "requelspeak")).isTrue();
		assertThat(dictionaryRepository.isKnownWord(reimported.getId(), "elicitron")).isTrue();
	}

	/**
	 * A word removed from the project's Dictionary page (issue #319) is gone from the next
	 * export: the export reads the repository, and the removal is a row delete there.
	 */
	@Test
	@Transactional
	void aRemovedDictionaryWordIsAbsentFromTheExport() throws Exception {
		initializeBaselineData();
		User projectUser = ensureProjectUserExists();
		Project project = createSampleProject(projectUser);

		dictionaryRepository.addToDictionary(project.getId(), "requelkeepword");
		dictionaryRepository.addToDictionary(project.getId(), "requelgoneword");
		entityManager.flush();
		Long goneId = dictionaryRepository.findProjectWord(project.getId(), "requelgoneword")
				.getId();
		assertThat(dictionaryRepository.deleteProjectWord(project.getId(), goneId)).isTrue();

		String xml = new String(exportProject(project), StandardCharsets.UTF_8);
		assertThat(xml).as("exported XML dictionary block")
				.contains("requelkeepword")
				.doesNotContain("requelgoneword");
	}

	/**
	 * Issue #320: an export carries each issue's resolution and every plain issue, and the import
	 * used to drop both. {@code AnnotationImportXml} didn't read {@code resolvedByPosition},
	 * {@code resolvedByUser} or {@code dateResolved}, and {@code AnnotationStaxImporter} read
	 * only {@code <note>} and {@code <lexicalIssue>}, so every {@code <issue>} was lost.
	 */
	@Test
	@Transactional
	void issueResolutionsAndPlainIssuesRoundTrip() throws Exception {
		initializeBaselineData();
		User projectUser = ensureProjectUserExists();
		Project originalProject = createSampleProject(projectUser);
		Goal goal = originalProject.getGoals().iterator().next();
		String ts = Long.toString(System.nanoTime());

		// A lexical issue resolved as ignored, a plain issue resolved by a human position, and an
		// open plain issue.
		com.rreganjr.requel.annotation.command.EditLexicalIssueCommand lexical =
				annotationCommandFactory.newEditLexicalIssueCommand();
		lexical.setEditedBy(projectUser);
		lexical.setGroupingObject(originalProject);
		lexical.setAnnotatable(goal);
		lexical.setWord("zorblat" + ts);
		lexical.setAnnotatableEntityPropertyName("Name");
		lexical.setText("The word zorblat" + ts + " is not recognized.");
		lexical.setMustBeResolved(true);
		com.rreganjr.requel.annotation.Issue lexicalIssue = commandHandler.execute(lexical).getIssue();
		resolveWith(lexicalIssue, projectUser, "Ignore this word.");

		com.rreganjr.requel.annotation.Issue resolvedPlain = addIssue(originalProject, goal,
				projectUser, "Who signs off on this? " + ts);
		resolveWith(resolvedPlain, projectUser, "The product owner " + ts);
		addIssue(originalProject, goal, projectUser, "Is this still needed? " + ts);
		entityManager.flush();
		java.util.Date originalResolvedDate = lexicalIssue.getResolvedDate();

		byte[] exportedBytes = exportProject(originalProject);
		String xml = new String(exportedBytes, StandardCharsets.UTF_8);
		assertThat(xml).as("exported XML").contains("<issue ").contains("resolvedByPosition=");
		assertXmlMatchesProjectSchema(exportedBytes);

		String reName = originalProject.getName() + " Resolutions " + Instant.now().toEpochMilli();
		Project reimported = importProject(exportedBytes, projectUser, reName);
		entityManager.flush();
		entityManager.clear();

		Goal importedGoal = projectRepository.findProjectByName(reName).getGoals().stream()
				.filter(candidate -> candidate.getName().equals(goal.getName())).findFirst()
				.orElseThrow(() -> new AssertionError("goal " + goal.getName() + " not imported"));
		List<com.rreganjr.requel.annotation.Issue> issues = importedGoal.getAnnotations().stream()
				.map(org.hibernate.Hibernate::unproxy)
				.filter(com.rreganjr.requel.annotation.Issue.class::isInstance)
				.map(com.rreganjr.requel.annotation.Issue.class::cast).collect(Collectors.toList());

		com.rreganjr.requel.annotation.Issue importedLexical = issueWithText(issues,
				"The word zorblat" + ts + " is not recognized.");
		assertThat(importedLexical.isResolved()).as("ignored lexical issue stays resolved").isTrue();
		assertThat(importedLexical.getResolvedByPosition().getText()).isEqualTo("Ignore this word.");
		assertThat(importedLexical.getResolvedDate()).as("resolution date is kept, to the second")
				.isCloseTo(originalResolvedDate, 1000);

		com.rreganjr.requel.annotation.Issue importedResolvedPlain = issueWithText(issues,
				"Who signs off on this? " + ts);
		assertThat(importedResolvedPlain.isResolved()).as("plain resolved issue").isTrue();
		assertThat(importedResolvedPlain.getResolvedByPosition().getText())
				.isEqualTo("The product owner " + ts);

		com.rreganjr.requel.annotation.Issue importedOpenPlain = issueWithText(issues,
				"Is this still needed? " + ts);
		assertThat(importedOpenPlain.isResolved()).as("plain open issue").isFalse();

		// A resolution that points at a position missing from the file imports the issue open
		// instead of failing the import.
		String positionId = xml.replaceAll("(?s).*resolvedByPosition=\"([^\"]+)\".*", "$1");
		byte[] brokenBytes = xml.replace("resolvedByPosition=\"" + positionId + "\"",
				"resolvedByPosition=\"POS_missing\"").getBytes(StandardCharsets.UTF_8);
		String brokenName = originalProject.getName() + " Dangling " + Instant.now().toEpochMilli();
		assertThat(importProject(brokenBytes, projectUser, brokenName)).isNotNull();
	}

	/**
	 * Issue #271: an issue's severity is exported and imported, and a file exported before #271
	 * (no {@code severity} attribute) imports each issue with its kind's default.
	 */
	@Test
	@Transactional
	void issueSeverityRoundTripsAndOldFilesGetTheDefault() throws Exception {
		initializeBaselineData();
		User projectUser = ensureProjectUserExists();
		Project originalProject = createSampleProject(projectUser);
		Goal goal = originalProject.getGoals().iterator().next();
		String ts = Long.toString(System.nanoTime());

		com.rreganjr.requel.annotation.command.EditIssueCommand high =
				annotationCommandFactory.newEditIssueCommand();
		high.setEditedBy(projectUser);
		high.setGroupingObject(originalProject);
		high.setAnnotatable(goal);
		high.setText("Admin console may be tenant-wide " + ts);
		high.setMustBeResolved(true);
		high.setSeverity(com.rreganjr.requel.annotation.IssueSeverity.HIGH);
		commandHandler.execute(high);

		com.rreganjr.requel.annotation.command.EditLexicalIssueCommand lexical =
				annotationCommandFactory.newEditLexicalIssueCommand();
		lexical.setEditedBy(projectUser);
		lexical.setGroupingObject(originalProject);
		lexical.setAnnotatable(goal);
		lexical.setWord("zorblat" + ts);
		lexical.setAnnotatableEntityPropertyName("Name");
		lexical.setText("The word zorblat" + ts + " is not recognized.");
		lexical.setMustBeResolved(true);
		commandHandler.execute(lexical);
		entityManager.flush();

		byte[] exportedBytes = exportProject(originalProject);
		String xml = new String(exportedBytes, StandardCharsets.UTF_8);
		assertThat(xml).as("exported XML").contains("severity=\"HIGH\"").contains("severity=\"LOW\"");
		assertXmlMatchesProjectSchema(exportedBytes);

		String reName = originalProject.getName() + " Severity " + Instant.now().toEpochMilli();
		importProject(exportedBytes, projectUser, reName);
		// An export from before #271: the same file with every severity attribute removed.
		byte[] oldBytes = xml.replaceAll(" severity=\"[A-Z]+\"", "")
				.getBytes(StandardCharsets.UTF_8);
		assertXmlMatchesProjectSchema(oldBytes);
		String oldName = originalProject.getName() + " PreSeverity " + Instant.now().toEpochMilli();
		importProject(oldBytes, projectUser, oldName);
		entityManager.flush();
		entityManager.clear();

		List<com.rreganjr.requel.annotation.Issue> roundTripped = issuesOnGoal(reName, goal);
		assertThat(issueWithText(roundTripped, "Admin console may be tenant-wide " + ts)
				.getSeverity()).isEqualTo(com.rreganjr.requel.annotation.IssueSeverity.HIGH);
		assertThat(issueWithText(roundTripped, "The word zorblat" + ts + " is not recognized.")
				.getSeverity()).isEqualTo(com.rreganjr.requel.annotation.IssueSeverity.LOW);

		List<com.rreganjr.requel.annotation.Issue> fromOldFile = issuesOnGoal(oldName, goal);
		assertThat(issueWithText(fromOldFile, "Admin console may be tenant-wide " + ts)
				.getSeverity()).as("plain issue default")
				.isEqualTo(com.rreganjr.requel.annotation.IssueSeverity.MEDIUM);
		assertThat(issueWithText(fromOldFile, "The word zorblat" + ts + " is not recognized.")
				.getSeverity()).as("lexical issue default")
				.isEqualTo(com.rreganjr.requel.annotation.IssueSeverity.LOW);
	}

	private List<com.rreganjr.requel.annotation.Issue> issuesOnGoal(String projectName, Goal goal) {
		Goal importedGoal = projectRepository.findProjectByName(projectName).getGoals().stream()
				.filter(candidate -> candidate.getName().equals(goal.getName())).findFirst()
				.orElseThrow(() -> new AssertionError("goal " + goal.getName() + " not imported"));
		return importedGoal.getAnnotations().stream()
				.map(org.hibernate.Hibernate::unproxy)
				.filter(com.rreganjr.requel.annotation.Issue.class::isInstance)
				.map(com.rreganjr.requel.annotation.Issue.class::cast).collect(Collectors.toList());
	}

	/**
	 * Issue #320: ignored findings and the IgnorePosition marker round-trip, with each key
	 * rebuilt for the entity's new id and pointing at the imported issue.
	 */
	@Test
	@Transactional
	void ignoredFindingsRoundTrip() throws Exception {
		initializeBaselineData();
		User projectUser = ensureProjectUserExists();
		Project originalProject = createSampleProject(projectUser);
		Goal goal = originalProject.getGoals().iterator().next();
		String ts = Long.toString(System.nanoTime());

		com.rreganjr.requel.annotation.command.EditLexicalIssueCommand lexical =
				annotationCommandFactory.newEditLexicalIssueCommand();
		lexical.setEditedBy(projectUser);
		lexical.setGroupingObject(originalProject);
		lexical.setAnnotatable(goal);
		lexical.setWord("zorblat" + ts);
		lexical.setAnnotatableEntityPropertyName("Name");
		lexical.setText("The word zorblat" + ts + " is not recognized.");
		lexical.setMustBeResolved(true);
		com.rreganjr.requel.annotation.Issue issue = commandHandler.execute(lexical).getIssue();
		com.rreganjr.requel.annotation.command.EditIgnorePositionCommand ignorePosition =
				annotationCommandFactory.newEditIgnorePositionCommand();
		ignorePosition.setEditedBy(projectUser);
		ignorePosition.setIssue(issue);
		ignorePosition.setText("Ignore this word.");
		com.rreganjr.requel.annotation.Position ignore = commandHandler.execute(ignorePosition)
				.getPosition();
		com.rreganjr.requel.annotation.command.ResolveIssueCommand resolve =
				annotationCommandFactory.newResolveIssueCommand(ignore);
		resolve.setEditedBy(projectUser);
		resolve.setIssue(issue);
		resolve.setPosition(ignore);
		commandHandler.execute(resolve);
		String suffix = "unknown-word:Name:zorblat" + ts;
		ignoredFindingStore.record(new com.rreganjr.requel.project.IgnoredFindingStore.Spec(
				originalProject.getId(), "Goal", goal.getId(), "legacy-lexical", "unknown-word",
				"Name", suffix, "zorblat" + ts, issue.getId()), projectUser);
		entityManager.flush();

		byte[] exportedBytes = exportProject(originalProject);
		String xml = new String(exportedBytes, StandardCharsets.UTF_8);
		assertThat(xml).as("exported XML").contains("<ignorePosition ").contains("<ignoredFinding ");
		assertXmlMatchesProjectSchema(exportedBytes);

		String reName = originalProject.getName() + " Ignores " + Instant.now().toEpochMilli();
		Project reimported = importProject(exportedBytes, projectUser, reName);
		entityManager.flush();

		Goal importedGoal = reimported.getGoals().stream()
				.filter(candidate -> candidate.getName().equals(goal.getName())).findFirst()
				.orElseThrow(() -> new AssertionError("goal " + goal.getName() + " not imported"));
		List<com.rreganjr.requel.project.IgnoredFinding> ignored = ignoredFindingStore
				.list(reimported.getId());
		assertThat(ignored).as("ignored findings on the reimported project").hasSize(1);
		com.rreganjr.requel.project.IgnoredFinding row = ignored.get(0);
		assertThat(row.getIdempotencyKey())
				.isEqualTo("legacy-lexical:Goal:" + importedGoal.getId() + ":" + suffix);
		com.rreganjr.requel.annotation.Issue importedIssue = importedGoal.getAnnotations().stream()
				.map(org.hibernate.Hibernate::unproxy)
				.filter(com.rreganjr.requel.annotation.Issue.class::isInstance)
				.map(com.rreganjr.requel.annotation.Issue.class::cast)
				.filter(candidate -> candidate.getText().startsWith("The word zorblat" + ts))
				.findFirst().orElseThrow();
		assertThat(row.getAnnotationId()).isEqualTo(importedIssue.getId());
		assertThat(org.hibernate.Hibernate.unproxy(importedIssue.getResolvedByPosition()))
				.isInstanceOf(com.rreganjr.requel.annotation.impl.IgnorePosition.class);
		// The original keeps its own.
		assertThat(ignoredFindingStore.list(originalProject.getId())).hasSize(1);
	}

	private com.rreganjr.requel.annotation.Issue addIssue(Project project, Goal goal, User editor,
			String text) throws Exception {
		com.rreganjr.requel.annotation.command.EditIssueCommand command =
				annotationCommandFactory.newEditIssueCommand();
		command.setEditedBy(editor);
		command.setGroupingObject(project);
		command.setAnnotatable(goal);
		command.setText(text);
		command.setMustBeResolved(true);
		return commandHandler.execute(command).getIssue();
	}

	private void resolveWith(com.rreganjr.requel.annotation.Issue issue, User editor,
			String positionText) throws Exception {
		com.rreganjr.requel.annotation.command.EditPositionCommand position =
				annotationCommandFactory.newEditPositionCommand();
		position.setEditedBy(editor);
		position.setIssue(issue);
		position.setText(positionText);
		com.rreganjr.requel.annotation.Position created = commandHandler.execute(position)
				.getPosition();
		com.rreganjr.requel.annotation.command.ResolveIssueCommand resolve =
				annotationCommandFactory.newResolveIssueCommand(created);
		resolve.setEditedBy(editor);
		resolve.setIssue(issue);
		resolve.setPosition(created);
		commandHandler.execute(resolve);
	}

	private static com.rreganjr.requel.annotation.Issue issueWithText(
			List<com.rreganjr.requel.annotation.Issue> issues, String text) {
		return issues.stream().filter(issue -> text.equals(issue.getText())).findFirst()
				.orElseThrow(() -> new AssertionError("no imported issue '" + text + "'; got "
						+ issues.stream().map(Annotation::getText).collect(Collectors.toList())));
	}

	private void assignImportedTagForTest(User user, Project projectScope, Goal goal,
			String category, String value) throws Exception {
		com.rreganjr.requel.tagging.command.EditTagCommand edit = tagCommandFactory.newEditTagCommand();
		edit.setEditedBy(user);
		edit.setProjectScope(projectScope);
		edit.setCategory(category);
		edit.setValue(value);
		edit.execute();

		com.rreganjr.requel.tagging.command.AssignTagCommand assign = tagCommandFactory.newAssignTagCommand();
		assign.setEditedBy(user);
		assign.setProjectScope(projectScope);
		assign.setTag(edit.getTag());
		assign.setTaggable((com.rreganjr.requel.tagging.Taggable) goal);
		assign.execute();
	}

	@Test
	@Transactional
	void importingSampleXmlPreservesCanonicalGlossaryTerms() throws Exception {
		initializeBaselineData();
		User projectUser = ensureProjectUserExists();
		// The canonical sample lives at <repo-root>/doc/samples/Requel.xml.
		// Reading it via a relative filesystem path doesn't work under surefire
		// (CWD is the module root, not the repo root). The repo-root copy is
		// copied onto the test classpath at doc/samples/Requel.xml by a
		// <testResource> block in this module's pom.xml.
		byte[] sampleXml;
		try (InputStream sampleStream = getClass().getClassLoader()
				.getResourceAsStream("doc/samples/Requel.xml")) {
			if (sampleStream == null) {
				throw new IllegalStateException(
						"doc/samples/Requel.xml not found on test classpath. "
								+ "Check the <testResource> block in modules/requel-app/pom.xml.");
			}
			sampleXml = sampleStream.readAllBytes();
		}
		String importedProjectName = "Sample Import " + UUID.randomUUID();
		Project imported = importProject(sampleXml, projectUser, importedProjectName);

		GlossaryTerm alias = imported.getGlossaryTerms().stream()
				.filter(term -> "Pee Pee Snow Cone".equals(term.getName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Alias glossary term not imported"));
		GlossaryTerm canonical = imported.getGlossaryTerms().stream()
				.filter(term -> "Yellow Snow".equals(term.getName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Canonical glossary term not imported"));

		assertThat(alias.getCanonicalTerm())
				.as("Alias term canonical reference")
				.isNotNull()
				.isEqualTo(canonical);
		assertThat(canonical.getAlternateTerms())
				.as("Canonical term alternates include alias")
				.contains(alias);
	}

	private User ensureProjectUserExists() throws NoSuchUserException {
		try {
			return userRepository.findUserByUsername("project");
		} catch (NoSuchUserException missing) {
			createProjectUser();
			return userRepository.findUserByUsername("project");
		}
	}

	private void createProjectUser() {
		try {
			EditUserCommand command = userCommandFactory.newEditUserCommand();
			command.setUsername("project");
			command.setPassword("project");
			command.setRepassword("project");
			command.setName("Builtin Project User");
			command.setEmailAddress("project@requel.invalid");
			command.setOrganizationName("Requel");
			command.addUserRoleName(ProjectUserRole.getRoleName(ProjectUserRole.class));
			command.addUserRolePermissionName(ProjectUserRole.getRoleName(ProjectUserRole.class),
					ProjectUserRole.createProjects.getName());
			commandHandler.execute(command);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize project user for streaming round-trip test", e);
		}
	}

	/**
	 * Establishes baseline users/roles/permissions before the round-trip
	 * exercises. As of the move to Spring Boot's {@code DatabaseInitializationRunner}
	 * (an {@code @EventListener(ApplicationReadyEvent.class)} that iterates
	 * every {@code SystemInitializer} bean), every initializer already runs
	 * during application context startup — long before any test method
	 * executes. The previous body of this method re-ran each initializer and
	 * also explicitly granted the project role to admin/assistant/project,
	 * which collided with the application-startup state and produced
	 * "detached entity passed to persist: JpaUserRolePermission" errors when
	 * the test's {@code @Transactional} context tried to re-persist permission
	 * entities that were already managed in a different persistence context.
	 *
	 * <p>Keeping the method (rather than deleting the call sites) makes the
	 * intent explicit at the call site and gives us a single place to add
	 * additional baseline state if the test fixture needs it later.
	 */
	private void initializeBaselineData() {
		// DatabaseInitializationRunner has already established the baseline.
	}

	private Project createSampleProject(User creator) throws Exception {
		String uniqueifier = UUID.randomUUID().toString();
		String projectName = SAMPLE_PROJECT_NAME_PREFIX + uniqueifier;
		String organizationName = "Org " + uniqueifier;

		String goalName = "Goal " + uniqueifier;
		String actorName = "Actor " + uniqueifier;
		String storyName = "Story " + uniqueifier;
		String useCaseName = "Use Case " + uniqueifier;
		String standaloneScenarioName = "Standalone Scenario " + uniqueifier;
		String standaloneScenarioStepName = "Standalone Scenario Step " + uniqueifier;
		String standaloneScenarioStepText = "The standalone scenario step executes unique flow " + uniqueifier + ".";
		String stepNameOne = "Authenticate User " + uniqueifier;
		String stepNameTwo = "Persist Project " + uniqueifier;
		String nonUserStakeholderName = "Regulatory Board " + uniqueifier;

		EditProjectCommand projectCommand = projectCommandFactory.newEditProjectCommand();
		projectCommand.setAnalysisEnabled(false);
		projectCommand.setEditedBy(creator);
		projectCommand.setName(projectName);
		projectCommand.setText("Sample project created for XML regression testing.");
		projectCommand.setOrganizationName(organizationName);
		projectCommand = commandHandler.execute(projectCommand);

		Project project = projectCommand.getProject();

		createGoal(project, goalName, "Ensure requirements are captured accurately.", creator);
		createActor(project, actorName, "Represents a primary system user.", creator);
		createStory(project, storyName, "As a user I want to manage requirements.", actorName, creator);

		List<EditScenarioStepCommand> stepCommands = List.of(
				newScenarioStepCommand(project, creator, stepNameOne,
						"The system validates user credentials.", ScenarioType.Primary),
				newScenarioStepCommand(project, creator, stepNameTwo,
						"The system saves the new project details.", ScenarioType.Primary));

	createUseCase(project, actorName, stepCommands,
			useCaseName, "Facilitates project creation.", creator);

	createStandaloneScenario(project, creator, standaloneScenarioName,
			standaloneScenarioStepName, standaloneScenarioStepText,
			"Standalone scenario outside any use case.", ScenarioType.Primary);

		createUserStakeholder(project, creator, "assistant" + uniqueifier);
		createNonUserStakeholder(project, nonUserStakeholderName,
				"Ensures system compliance with regulations.", creator);

		Project refreshed = projectRepository.findProjectByName(projectName);

		Goal goal = findGoalByName(refreshed, goalName);
		addNote(refreshed, goal, creator, "Goal note");

		Actor actor = findActorByName(refreshed, actorName);
		addNote(refreshed, actor, creator, "Actor note");

		Story story = findStoryByName(refreshed, storyName);
		addNote(refreshed, story, creator, "Story note");

		UseCase useCase = findUseCaseByName(refreshed, useCaseName);
		addNote(refreshed, useCase, creator, "Use case note");

		Scenario scenario = findScenarioByName(refreshed, useCaseName);
		addNote(refreshed, scenario, creator, "Scenario note");
		for (Step step : scenario.getSteps()) {
			addNote(refreshed, (Annotatable) step, creator, "Scenario step note: " + step.getName());
		}

		Scenario standaloneScenario = findScenarioByName(refreshed, standaloneScenarioName);
		addNote(refreshed, standaloneScenario, creator, "Standalone scenario note");
		for (Step step : standaloneScenario.getSteps()) {
			addNote(refreshed, (Annotatable) step, creator, "Standalone scenario step note: " + step.getName());
		}

		annotateCreatorStakeholder(refreshed, creator);

		UserStakeholder assistantStakeholder = findUserStakeholderByUsername(refreshed, "assistant");
		if (assistantStakeholder != null) {
			addNote(refreshed, assistantStakeholder, creator, "Assistant stakeholder note");
		}

		NonUserStakeholder regulator = findNonUserStakeholderByName(refreshed, nonUserStakeholderName);
		addNote(refreshed, regulator, creator, "Non-user stakeholder note");

		GlossaryTerm glossaryTerm = createGlossaryTerm(refreshed, "Term " + uniqueifier,
				"A sample glossary definition.", creator);
		linkGlossaryTermToEntities(glossaryTerm,
				findActorByName(refreshed, actorName),
				findGoalByName(refreshed, goalName),
				findStoryByName(refreshed, storyName));

		addNote(refreshed, refreshed, creator, "Project note");

		Project finalProject = projectRepository.findProjectByName(projectName);
		assertThat(collectAllProjectAnnotations(finalProject))
				.as("Project annotations persisted")
				.isNotEmpty();
		return finalProject;
	}

	private Goal createGoal(Project project, String name, String text, User creator) throws Exception {
		EditGoalCommand command = projectCommandFactory.newEditGoalCommand();
		command.setAnalysisEnabled(false);
		command.setEditedBy(creator);
		command.setGoalContainer(project);
		command.setName(name);
		command.setText(text);
		command = commandHandler.execute(command);
		assertThat(command.getGoal()).as("Created goal").isNotNull();
		return command.getGoal();
	}

	private Actor createActor(Project project, String name, String text, User creator) throws Exception {
		EditActorCommand command = projectCommandFactory.newEditActorCommand();
		command.setAnalysisEnabled(false);
		command.setEditedBy(creator);
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setText(text);
		command = commandHandler.execute(command);
		assertThat(command.getActor()).as("Created actor").isNotNull();
		return command.getActor();
	}

	private Story createStory(Project project, String name, String text, String primaryActorName, User creator) throws Exception {
		EditStoryCommand command = projectCommandFactory.newEditStoryCommand();
		command.setAnalysisEnabled(false);
		command.setEditedBy(creator);
		command.setStoryContainer(project);
		command.setName(name);
		command.setText(text);
		command.setStoryTypeName(StoryType.Success.name());
		command.setPrimaryActorName(primaryActorName);
		command = commandHandler.execute(command);
		assertThat(command.getStory()).as("Created story").isNotNull();
		return command.getStory();
	}

	private EditScenarioStepCommand newScenarioStepCommand(Project project, User creator, String name,
			String text, ScenarioType scenarioType) {
		EditScenarioStepCommand command = projectCommandFactory.newEditScenarioStepCommand();
		command.setAnalysisEnabled(false);
		command.setEditedBy(creator);
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setText(text);
		command.setScenarioTypeName(scenarioType.name());
		return command;
	}

private UseCase createUseCase(Project project, String primaryActorName,
		List<EditScenarioStepCommand> stepCommands, String name, String text, User creator)
		throws Exception {
		EditUseCaseCommand command = projectCommandFactory.newEditUseCaseCommand();
		command.setAnalysisEnabled(false);
		command.setEditedBy(creator);
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setText(text);
		command.setPrimaryActorName(primaryActorName);
		command.setStepCommands(stepCommands);
		command = commandHandler.execute(command);
		assertThat(command.getUseCase()).as("Created use case").isNotNull();
		return command.getUseCase();
}

	private Scenario createStandaloneScenario(Project project, User creator, String scenarioName,
			String stepName, String stepText, String scenarioText, ScenarioType scenarioType)
		throws Exception {
		EditScenarioCommand scenarioCommand = projectCommandFactory.newEditScenarioCommand();
		scenarioCommand.setAnalysisEnabled(false);
		scenarioCommand.setEditedBy(creator);
		scenarioCommand.setProjectOrDomain(project);
		scenarioCommand.setName(scenarioName);
		scenarioCommand.setText(scenarioText);
		scenarioCommand.setScenarioTypeName(scenarioType.name());
		scenarioCommand.setStepCommands(List.of(
				newScenarioStepCommand(project, creator, stepName, stepText, scenarioType)));
		scenarioCommand = commandHandler.execute(scenarioCommand);
		Scenario scenario = scenarioCommand.getScenario();
		assertThat(scenario).as("Created standalone scenario").isNotNull();
		return scenario;
	}

	private UserStakeholder createUserStakeholder(Project project, User creator, String username) throws Exception {
		try {
			User targetUser = userRepository.findUserByUsername(username);
			EditUserStakeholderCommand command = projectCommandFactory.newEditUserStakeholderCommand();
			command.setAnalysisEnabled(false);
			command.setEditedBy(creator);
			command.setProjectOrDomain(project);
			command.setUsername(targetUser.getUsername());
			command = commandHandler.execute(command);
			return command.getStakeholder();
		} catch (NoSuchUserException e) {
			return null;
		}
	}

	private NonUserStakeholder createNonUserStakeholder(Project project, String name, String text, User creator)
			throws Exception {
		EditNonUserStakeholderCommand command = projectCommandFactory.newEditNonUserStakeholderCommand();
		command.setAnalysisEnabled(false);
		command.setEditedBy(creator);
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setText(text);
		command = commandHandler.execute(command);
		assertThat(command.getStakeholder()).as("Created non-user stakeholder").isNotNull();
		return command.getStakeholder();
	}

	private byte[] exportProject(Project project) throws Exception {
		ByteArrayOutputStream exportedXml = new ByteArrayOutputStream();
		ExportProjectCommand exportCommand = projectCommandFactory.newExportProjectCommand();
		exportCommand.setProject(project);
		exportCommand.setOutputStream(exportedXml);
		commandHandler.execute(exportCommand);
		byte[] exportedBytes = exportedXml.toByteArray();
		Path tempFile = Files.createTempFile("project-roundtrip-export-", ".xml");
		Files.write(tempFile, exportedBytes);
		Path workspaceFile = Path.of("target", "project-roundtrip-export.xml");
		Files.createDirectories(workspaceFile.getParent());
		Files.write(workspaceFile, exportedBytes);
		System.out.println("Exported project XML written to " + tempFile + " and " + workspaceFile);
		return exportedBytes;
	}

	private Project importProject(byte[] exportedBytes, User importUser, String importedProjectName) throws Exception {
		try (ByteArrayInputStream reimportStream = new ByteArrayInputStream(exportedBytes)) {
			ImportProjectCommand importCommand = applicationContext.getBean(
					"importProjectCommand", ImportProjectCommand.class);
			importCommand.setAnalysisEnabled(false);
			importCommand.setEditedBy(importUser);
			importCommand.setName(importedProjectName);
			importCommand.setInputStream(reimportStream);
			commandHandler.execute(importCommand);
		}
		return projectRepository.findProjectByName(importedProjectName);
	}

	private ProjectSnapshot snapshotProject(Project project) {
		String createdByUsername = project.getCreatedBy() != null ? project.getCreatedBy().getUsername() : null;
		String organizationName = project.getOrganization() != null ? project.getOrganization().getName() : null;
		Set<Annotation> allAnnotations = collectAllProjectAnnotations(project);
		int annotationCount = countAnnotationsFor(allAnnotations, project);

		Set<EntitySummary> actorSummaries = project.getActors()
				.stream()
				.map(actor -> new EntitySummary(actor.getName(), actor.getText(),
						countAnnotationsFor(allAnnotations, actor)))
				.collect(Collectors.toSet());

		Set<EntitySummary> goalSummaries = project.getGoals()
				.stream()
				.map(goal -> new EntitySummary(goal.getName(), goal.getText(),
						countAnnotationsFor(allAnnotations, goal)))
				.collect(Collectors.toSet());

		Set<StorySummary> storySummaries = project.getStories()
				.stream()
				.map(story -> new StorySummary(story.getName(), story.getText(),
						story.getStoryType() != null ? story.getStoryType().name() : null,
						story.getPrimaryActor() != null ? story.getPrimaryActor().getName() : null,
						countAnnotationsFor(allAnnotations, story)))
				.collect(Collectors.toSet());

		Set<ScenarioSummary> scenarioSummaries = project.getScenarios()
				.stream()
				.map(scenario -> new ScenarioSummary(
						scenario.getName(),
						scenario.getText(),
						typeName(scenario.getType()),
						scenario.getSteps()
							.stream()
							.map(step -> new StepSummary(step.getName(), step.getText(),
									typeName(step.getType()), countAnnotationsFor(allAnnotations, (Annotatable) step)))
							.collect(Collectors.toSet()),
						countAnnotationsFor(allAnnotations, scenario)))
				.collect(Collectors.toSet());

		Set<UseCaseSummary> useCaseSummaries = project.getUseCases()
				.stream()
				.map(useCase -> new UseCaseSummary(
						useCase.getName(),
						useCase.getText(),
						useCase.getPrimaryActor() != null ? useCase.getPrimaryActor().getName() : null,
						useCase.getScenario() != null ? useCase.getScenario().getName() : null,
						countAnnotationsFor(allAnnotations, useCase)))
				.collect(Collectors.toSet());

		Set<UserStakeholderSummary> userStakeholders = project.getStakeholders()
				.stream()
				.filter(Stakeholder::isUserStakeholder)
				.map(stakeholder -> (UserStakeholder) stakeholder)
				.map(userStakeholder -> new UserStakeholderSummary(
						userStakeholder.getUser().getUsername(),
						countAnnotationsFor(allAnnotations, userStakeholder)))
				.collect(Collectors.toSet());

		Set<NonUserStakeholderSummary> nonUserStakeholders = project.getStakeholders()
				.stream()
				.filter(stakeholder -> !stakeholder.isUserStakeholder())
				.map(stakeholder -> (NonUserStakeholder) stakeholder)
				.map(nonUserStakeholder -> new NonUserStakeholderSummary(
						nonUserStakeholder.getName(),
						nonUserStakeholder.getText(),
						countAnnotationsFor(allAnnotations, nonUserStakeholder)))
				.collect(Collectors.toSet());

		Set<GlossaryTermSummary> glossaryTerms = project.getGlossaryTerms()
				.stream()
				.map(term -> new GlossaryTermSummary(
						term.getName(),
						term.getText(),
						term.getCanonicalTerm() != null ? term.getCanonicalTerm().getName() : null,
						countAnnotationsFor(allAnnotations, term)))
				.collect(Collectors.toSet());

		return new ProjectSnapshot(organizationName, createdByUsername, annotationCount, actorSummaries,
				goalSummaries, storySummaries, scenarioSummaries, useCaseSummaries, userStakeholders,
				nonUserStakeholders, glossaryTerms);
	}

	private void assertSnapshotsEquivalent(ProjectSnapshot expected, ProjectSnapshot actual) {
		assertThat(actual.organizationName())
				.as("Organization name")
				.isEqualTo(expected.organizationName());
		if (actual.annotationCount() != expected.annotationCount()) {
			System.out.println("Project annotation count mismatch: expected " + expected.annotationCount()
					+ " but was " + actual.annotationCount());
		}
		assertThat(actual.createdByUsername())
				.as("Created by username")
				.isEqualTo(expected.createdByUsername());
		assertThat(actual.actors())
				.as("Actors")
				.containsExactlyInAnyOrderElementsOf(expected.actors());
		assertThat(actual.goals())
				.as("Goals")
				.containsExactlyInAnyOrderElementsOf(expected.goals());
		assertThat(actual.stories())
				.as("Stories")
				.containsExactlyInAnyOrderElementsOf(expected.stories());
		assertThat(actual.scenarios())
				.as("Scenarios")
				.containsExactlyInAnyOrderElementsOf(expected.scenarios());
		assertThat(actual.useCases())
				.as("Use cases")
				.containsExactlyInAnyOrderElementsOf(expected.useCases());
		assertThat(actual.userStakeholders())
				.as("User stakeholders")
				.containsExactlyInAnyOrderElementsOf(expected.userStakeholders());
			assertThat(actual.nonUserStakeholders())
					.as("Non-user stakeholders")
					.containsExactlyInAnyOrderElementsOf(expected.nonUserStakeholders());
			assertThat(actual.glossaryTerms())
					.as("Glossary terms")
					.containsExactlyInAnyOrderElementsOf(expected.glossaryTerms());
	}

	private void annotateCreatorStakeholder(Project project, User creator) throws Exception {
		for (Stakeholder stakeholder : project.getStakeholders()) {
			if (stakeholder.isUserStakeholder()) {
				UserStakeholder userStakeholder = (UserStakeholder) stakeholder;
				if (userStakeholder.getUser().getUsername().equals(creator.getUsername())) {
					addNote(project, userStakeholder, creator, "Creator stakeholder note");
					break;
				}
			}
		}
	}

	private void assertXmlMatchesProjectSchema(byte[] xmlBytes) throws Exception {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try (ByteArrayInputStream xmlInput = new ByteArrayInputStream(xmlBytes);
				InputStream schemaStream = getClass().getClassLoader()
						.getResourceAsStream("doc/samples/project.xsd")) {
			if (schemaStream == null) {
				throw new IllegalStateException("project.xsd not found on test classpath");
			}
			StreamSource schemaSource = new StreamSource(schemaStream);
			schemaSource.setSystemId("classpath:doc/samples/project.xsd");
			Schema schema = schemaFactory.newSchema(schemaSource);
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(xmlInput));
		}
	}

	private Set<Annotation> collectAllProjectAnnotations(Project project) {
		Set<Annotation> annotations = new HashSet<>(project.getAnnotations());
		project.getProjectEntities().forEach(entity -> annotations.addAll(entity.getAnnotations()));
		return annotations;
	}

	private void addNote(Project project, Annotatable annotatable, User editor, String text) throws Exception {
		EditNoteCommand command = annotationCommandFactory.newEditNoteCommand();
		command.setEditedBy(editor);
		command.setGroupingObject(project);
		command.setAnnotatable(annotatable);
		command.setText(text);
		EditNoteCommand executed = commandHandler.execute(command);
		assertThat(executed.getNote()).as("Created note for " + annotatable.getClass().getSimpleName()).isNotNull();
	}

	private int countAnnotationsFor(Set<Annotation> allAnnotations, Annotatable annotatable) {
		return (int) allAnnotations.stream()
				.filter(annotation -> sameEntity(annotation.getGroupingObject(), annotatable)
						|| annotation.getAnnotatables().stream()
								.anyMatch(candidate -> sameEntity(candidate, annotatable)))
				.count();
	}

	private boolean sameEntity(Object left, Object right) {
		if ((left == null) || (right == null)) {
			return false;
		}
		if (left == right) {
			return true;
		}
		if (left.equals(right)) {
			return true;
		}
		Object leftId = extractId(left);
		Object rightId = extractId(right);
		if ((leftId != null) && leftId.equals(rightId)) {
			return left.getClass().isAssignableFrom(right.getClass())
					|| right.getClass().isAssignableFrom(left.getClass());
		}
		return false;
	}

	private Object extractId(Object entity) {
		Class<?> entityType = entity.getClass();
		while (entityType != null) {
			try {
				java.lang.reflect.Method getId = entityType.getDeclaredMethod("getId");
				getId.setAccessible(true);
				return getId.invoke(entity);
			} catch (NoSuchMethodException e) {
				entityType = entityType.getSuperclass();
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	private Goal findGoalByName(Project project, String name) {
		return project.getGoals().stream()
				.filter(goal -> goal.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Goal not found: " + name));
	}

	private Actor findActorByName(Project project, String name) {
		return project.getActors().stream()
				.filter(actor -> actor.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Actor not found: " + name));
	}

	private Story findStoryByName(Project project, String name) {
		return project.getStories().stream()
				.filter(story -> story.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Story not found: " + name));
	}

	private UseCase findUseCaseByName(Project project, String name) {
		return project.getUseCases().stream()
				.filter(useCase -> useCase.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Use case not found: " + name));
	}

	private Scenario findScenarioByName(Project project, String name) {
		return project.getScenarios().stream()
				.filter(scenario -> scenario.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Scenario not found: " + name));
	}

	private UserStakeholder findUserStakeholderByUsername(Project project, String username) {
		return project.getStakeholders().stream()
				.filter(Stakeholder::isUserStakeholder)
				.map(stakeholder -> (UserStakeholder) stakeholder)
				.filter(userStakeholder -> userStakeholder.getUser().getUsername().equals(username))
				.findFirst()
				.orElse(null);
	}

	private NonUserStakeholder findNonUserStakeholderByName(Project project, String name) {
		return project.getStakeholders().stream()
				.filter(stakeholder -> !stakeholder.isUserStakeholder())
				.map(stakeholder -> (NonUserStakeholder) stakeholder)
				.filter(nonUserStakeholder -> nonUserStakeholder.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Non-user stakeholder not found: " + name));
	}

	private GlossaryTerm createGlossaryTerm(Project project, String name, String text, User creator) throws Exception {
		EditGlossaryTermCommand command = projectCommandFactory.newEditGlossaryTermCommand();
		command.setAnalysisEnabled(false);
		command.setEditedBy(creator);
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setText(text);
		command = commandHandler.execute(command);
		assertThat(command.getGlossaryTerm()).as("Created glossary term").isNotNull();
		return command.getGlossaryTerm();
	}

	private void linkGlossaryTermToEntities(GlossaryTerm term, ProjectOrDomainEntity... entities) {
		for (ProjectOrDomainEntity entity : entities) {
			entity.getGlossaryTerms().add(term);
			((GlossaryTermImpl) term).getReferers().add(entity);
		}
	}

	private String typeName(ScenarioType type) {
		return type != null ? type.name() : null;
	}

	private record EntitySummary(String name, String text, int annotationCount) {}

	private record StorySummary(String name, String text, String type, String primaryActorName, int annotationCount) {}

	private record StepSummary(String name, String text, String type, int annotationCount) {}

	private record ScenarioSummary(String name, String text, String type, Set<StepSummary> steps,
			int annotationCount) {}

	private record UseCaseSummary(String name, String text, String primaryActorName, String scenarioName,
			int annotationCount) {}

	private record UserStakeholderSummary(String username, int annotationCount) {}

	private record NonUserStakeholderSummary(String name, String text, int annotationCount) {}

	private record GlossaryTermSummary(String name, String text, String canonicalTermName, int annotationCount) {}

	private record ProjectSnapshot(String organizationName, String createdByUsername, int annotationCount,
			Set<EntitySummary> actors, Set<EntitySummary> goals, Set<StorySummary> stories,
			Set<ScenarioSummary> scenarios, Set<UseCaseSummary> useCases,
			Set<UserStakeholderSummary> userStakeholders, Set<NonUserStakeholderSummary> nonUserStakeholders,
			Set<GlossaryTermSummary> glossaryTerms) {}
}
