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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
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
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.command.ExportProjectCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.repository.init.StakeholderPermissionsInitializer;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.repository.init.AdminUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.AssistantUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.ProjectUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.UserRolePermissionsInitializer;
import com.rreganjr.requel.user.exception.NoSuchUserException;

/**
 * Regression coverage that builds a project with sample data, exports it, and verifies that re-importing
 * the XML produces an equivalent project structure.
 */
@SpringBootTest(classes = Application.class)
@TestPropertySource(locations = "classpath:db.properties")
class ProjectXmlRoundTripIT {

	private static final String SAMPLE_PROJECT_NAME_PREFIX = "Regression Project ";
	private static final AtomicBoolean BASELINE_BOOTSTRAPPED = new AtomicBoolean(false);

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

	@Test
	@Transactional
	void importExportRoundTripKeepsProjectRoundTrippable() throws Exception {
		initializeBaselineData();

		User projectUser = ensureProjectUserExists();
		Project originalProject = createSampleProject(projectUser);
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
				.contains("<actors>");
		assertXmlMatchesProjectSchema(exportedBytes);

		String reimportedProjectName = originalProject.getName() + " Reimport " + Instant.now().toEpochMilli();
		Project reimportedProject = importProject(exportedBytes, projectUser, reimportedProjectName);
		ProjectSnapshot reimportedSnapshot = snapshotProject(reimportedProject);
		System.out.println("Re-imported project annotations: " + reimportedSnapshot.annotationCount());
		assertSnapshotsEquivalent(originalSnapshot, reimportedSnapshot);
	}

	private User ensureProjectUserExists() throws NoSuchUserException {
		return userRepository.findUserByUsername("project");
	}

	private void initializeBaselineData() {
		if (BASELINE_BOOTSTRAPPED.compareAndSet(false, true)) {
			userRolePermissionsInitializer.initialize();
			stakeholderPermissionsInitializer.initialize();
			adminUserInitializer.initialize();
			assistantUserInitializer.initialize();
			projectUserInitializer.initialize();
		}
		ensureUserHasProjectRole("admin");
		ensureUserHasProjectRole("assistant");
		ensureUserHasProjectRole("project");
	}

	private void ensureUserHasProjectRole(String username) {
		try {
			User user = userRepository.findUserByUsername(username);
			if (!user.hasRole(ProjectUserRole.class)) {
				user.grantRole(ProjectUserRole.class);
			}
			user.getRoleForType(ProjectUserRole.class)
					.grantUserRolePermission(ProjectUserRole.createProjects);
		} catch (NoSuchUserException ignored) {
			// optional user (e.g., assistant) may not exist in some test configurations
		}
	}

	private Project createSampleProject(User creator) throws Exception {
		String uniqueifier = UUID.randomUUID().toString();
		String projectName = SAMPLE_PROJECT_NAME_PREFIX + uniqueifier;
		String organizationName = "Org " + uniqueifier;

		String goalName = "Goal " + uniqueifier;
		String actorName = "Actor " + uniqueifier;
		String storyName = "Story " + uniqueifier;
		String useCaseName = "Use Case " + uniqueifier;
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
		createStory(project, storyName, "As a user I want to manage requirements.", creator);

		List<EditScenarioStepCommand> stepCommands = List.of(
				newScenarioStepCommand(project, creator, stepNameOne,
						"The system validates user credentials.", ScenarioType.Primary),
				newScenarioStepCommand(project, creator, stepNameTwo,
						"The system saves the new project details.", ScenarioType.Primary));

		createUseCase(project, actorName, stepCommands,
				useCaseName, "Facilitates project creation.", creator);

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

		annotateCreatorStakeholder(refreshed, creator);

		UserStakeholder assistantStakeholder = findUserStakeholderByUsername(refreshed, "assistant");
		if (assistantStakeholder != null) {
			addNote(refreshed, assistantStakeholder, creator, "Assistant stakeholder note");
		}

		NonUserStakeholder regulator = findNonUserStakeholderByName(refreshed, nonUserStakeholderName);
		addNote(refreshed, regulator, creator, "Non-user stakeholder note");

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

	private Story createStory(Project project, String name, String text, User creator) throws Exception {
		EditStoryCommand command = projectCommandFactory.newEditStoryCommand();
		command.setAnalysisEnabled(false);
		command.setEditedBy(creator);
		command.setStoryContainer(project);
		command.setName(name);
		command.setText(text);
		command.setStoryTypeName(StoryType.Success.name());
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
			ImportProjectCommand importCommand = projectCommandFactory.newImportProjectCommand();
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

		return new ProjectSnapshot(organizationName, createdByUsername, annotationCount, actorSummaries,
				goalSummaries, storySummaries, scenarioSummaries, useCaseSummaries, userStakeholders,
				nonUserStakeholders);
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
		Path schemaPath = Path.of("doc", "samples", "project.xsd");
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try (ByteArrayInputStream xmlInput = new ByteArrayInputStream(xmlBytes);
				InputStream schemaStream = Files.newInputStream(schemaPath)) {
			StreamSource schemaSource = new StreamSource(schemaStream);
			schemaSource.setSystemId(schemaPath.toUri().toString());
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

	private String typeName(ScenarioType type) {
		return type != null ? type.name() : null;
	}

	private record EntitySummary(String name, String text, int annotationCount) {}

	private record StorySummary(String name, String text, String type, int annotationCount) {}

	private record StepSummary(String name, String text, String type, int annotationCount) {}

	private record ScenarioSummary(String name, String text, String type, Set<StepSummary> steps,
			int annotationCount) {}

	private record UseCaseSummary(String name, String text, String primaryActorName, String scenarioName,
			int annotationCount) {}

	private record UserStakeholderSummary(String username, int annotationCount) {}

	private record NonUserStakeholderSummary(String name, String text, int annotationCount) {}

	private record ProjectSnapshot(String organizationName, String createdByUsername, int annotationCount,
			Set<EntitySummary> actors, Set<EntitySummary> goals, Set<StorySummary> stories,
			Set<ScenarioSummary> scenarios, Set<UseCaseSummary> useCases,
			Set<UserStakeholderSummary> userStakeholders, Set<NonUserStakeholderSummary> nonUserStakeholders) {}
}
