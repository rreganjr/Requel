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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.EntityLockException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.project.command.AddActorToActorContainerCommand;
import com.rreganjr.requel.project.command.AddGoalToGoalContainerCommand;
import com.rreganjr.requel.project.command.AddScenarioToUseCaseCommand;
import com.rreganjr.requel.project.command.DeleteProjectCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.project.impl.repository.init.StakeholderPermissionsInitializer;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Integration coverage for {@link DeleteProjectCommand} (issue #240, epic #239).
 *
 * <p>
 * Each test builds its own uniquely-named project so the destructive deletes do
 * not interfere with one another. Projects are created by {@code admin}; the
 * project creator is auto-granted every available stakeholder permission - which,
 * with the {@code Project[Delete]} seed added in this ticket, includes the delete
 * permission - so the creator can delete their own project.
 *
 * <p>
 * Authorization is enforced by {@code AuthorizingCommandHandler} on the acting
 * user's ({@code editedBy}) stakeholder permissions; there is no system-role
 * bypass, so the negative auth test uses a stakeholder that lacks
 * {@code Project[Delete]}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeleteProjectIT extends AbstractIntegrationTestCase {

    @Autowired
    private StakeholderPermissionsInitializer stakeholderPermissionsInitializer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String PROJECT_DELETE_KEY =
            StakeholderPermissionImpl.generatePermissionKey(Project.class,
                    StakeholderPermissionType.Delete);
    private static final String PROJECT_EDIT_KEY =
            StakeholderPermissionImpl.generatePermissionKey(Project.class,
                    StakeholderPermissionType.Edit);

    @BeforeAll
    void setUp() throws Exception {
        initializeBaselineData();
    }

    // -------------------------------------------------------------------------
    // Cascade
    // -------------------------------------------------------------------------

    @Test
    void deleteProjectCascadesEveryChildAndPreservesUsers() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        String projectName = "del-cascade-" + ts;

        Project project = createProject(admin, projectName);

        // One of several child types.
        Long goalId = createGoal(admin, project, "goal-" + ts).getId();
        Long actorId = createActor(admin, project, "actor-" + ts).getId();
        Long storyId = createStory(admin, project, "story-" + ts).getId();
        Long termId = createGlossaryTerm(admin, project, "term-" + ts).getId();

        // A user-stakeholder for a second, real user - deleting the project must
        // sever the association but never delete the User.
        String memberUsername = "del-member-" + ts;
        createUser(memberUsername);
        addUserStakeholder(admin, project, memberUsername, Set.of(PROJECT_EDIT_KEY));
        User member = getUserRepository().findUserByUsername(memberUsername);
        Long memberUserId = member.getId();
        Long memberStakeholderId = getProjectRepository()
                .findStakeholderByProjectOrDomainAndUser(project, member).getId();

        // A non-user stakeholder.
        Long nonUserStakeholderId =
                createNonUserStakeholder(admin, project, "nonuser-" + ts).getId();

        // Every project is created with a built-in report generator, so report
        // generators are exercised without extra setup.

        // Act.
        deleteProject(admin, project, null);

        // The project itself is gone.
        assertThrows(NoSuchProjectException.class,
                () -> getProjectRepository().findProjectByName(projectName));

        // Every child row is gone (no orphans).
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Goal.class, goalId));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Actor.class, actorId));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Story.class, storyId));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(GlossaryTerm.class, termId));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(UserStakeholder.class, memberStakeholderId));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(NonUserStakeholder.class, nonUserStakeholderId));

        // No User was deleted - the stakeholder's user survives, unchanged id.
        User memberAfter = getUserRepository().findUserByUsername(memberUsername);
        assertNotNull(memberAfter, "stakeholder's user must not be deleted");
        assertEquals(memberUserId, memberAfter.getId());
        assertNotNull(getUserRepository().findUserByUsername("admin"));
    }

    /**
     * #247: the graph the e2e suite builds and the original cascade could not delete on
     * MySQL - a use case with a primary scenario (with steps) and an additional scenario,
     * an annotated goal, an annotated use case, and a story with a primary actor. Every
     * row must go: use-case scenarios that are not reachable from
     * project.getScenarios(), their steps (same {@code scenarios} table, same pods FK),
     * the usecase_scenarios join rows, and the annotations with their join rows.
     * <p>
     * Against H2 (JPA create-drop) this checks the walk; the MySQL subclass
     * ({@code DeleteProjectMySqlIT}) checks it against the real foreign keys.
     */
    @Test
    void deleteProjectCascadesUseCaseScenariosStepsAndAnnotations() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        String projectName = "del-rich-" + ts;
        Project project = createProject(admin, projectName);

        Actor actor = createActor(admin, project, "rich-actor-" + ts);
        Goal goal = createGoal(admin, project, "rich-goal-" + ts);
        UseCase useCase = createUseCase(admin, project, "rich-usecase-" + ts, actor.getName(),
                "step one " + ts, "step two " + ts);
        Long useCaseId = useCase.getId();
        Long primaryScenarioId = useCase.getScenario().getId();
        List<Long> stepIds = new ArrayList<>();
        for (Step step : useCase.getScenario().getSteps()) {
            stepIds.add(step.getId());
        }
        assertEquals(2, stepIds.size(), "precondition: the primary scenario has two steps");

        // An additional (alternative) scenario, created at project level and attached to
        // the use case - it is in BOTH project.getScenarios() and usecase_scenarios.
        Scenario additional = createScenario(admin, project, "rich-alt-" + ts,
                ScenarioType.Alternative);
        addScenarioToUseCase(admin, useCase, additional);
        Long additionalScenarioId = additional.getId();

        // Stories with a PRIMARY actor and an additional actor, and a goal held by the actor
        // (the e2e stories/actors suites' shape). The primary-actor and actor-goal links are
        // @ManyToAny referer collections on the actor/goal that must not be left pointing at a
        // container already removed in this transaction (#247: Hibernate re-persisted the
        // removed container on merge and failed the commit with TransientObjectException).
        Story story = createStory(admin, project, "rich-story-" + ts, actor.getName());
        Long storyId = story.getId();
        Story secondStory = createStory(admin, project, "rich-story-2-" + ts, actor.getName());
        Long secondStoryId = secondStory.getId();
        Actor extraActor = createActor(admin, project, "rich-extra-actor-" + ts);
        addActorToContainer(admin, extraActor, story);
        addGoalToContainer(admin, goal, actor);
        addGoalToContainer(admin, goal, story);

        // Annotations on a goal (goals_annotations) and on the use case
        // (usecases_annotations), both also linked from annotation_annotatable.
        Long goalNoteId = addNote(admin, project, goal, "note on goal " + ts).getId();
        Long useCaseIssueId = addIssue(admin, project, useCase, "issue on use case " + ts).getId();
        // A note SHARED by two stories and the goal (EditNote reuses an existing note with the
        // same text) - the shape the lexical assistant produces for an unknown word that appears
        // in several entities. Its @ManyToAny collection gets loaded and must not go stale as
        // its annotatables are deleted one by one (#247: "references an unsaved transient
        // instance" at commit).
        Note shared = addNote(admin, project, story, "shared note " + ts);
        assertEquals(shared.getId(), addNote(admin, project, secondStory, "shared note " + ts).getId(),
                "precondition: the same note is reused across annotatables");
        assertEquals(shared.getId(), addNote(admin, project, goal, "shared note " + ts).getId());
        Long sharedNoteId = shared.getId();

        // Act.
        deleteProject(admin, project, null);

        assertThrows(NoSuchProjectException.class,
                () -> getProjectRepository().findProjectByName(projectName));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(UseCase.class, useCaseId));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Scenario.class, primaryScenarioId));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Scenario.class, additionalScenarioId));
        for (Long stepId : stepIds) {
            assertThrows(NoSuchEntityException.class,
                    () -> getProjectRepository().findById(Step.class, stepId), "step " + stepId);
        }
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Story.class, storyId));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Story.class, secondStoryId));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Goal.class, goal.getId()));
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Actor.class, actor.getId()));
        assertNull(getAnnotationRepository().findAnnotationById(goalNoteId),
                "the goal's note must be deleted with its last annotatable");
        assertNull(getAnnotationRepository().findAnnotationById(useCaseIssueId),
                "the use case's issue must be deleted with its last annotatable");
        assertNull(getAnnotationRepository().findAnnotationById(sharedNoteId),
                "the shared note must be deleted once its last annotatable is gone");
    }

    /**
     * #247: annotations still grouped under the project when the project row goes -
     * findings an assistant filed against an entity deleted before they were applied,
     * or links left dangling by the pre-#247 race - must be deleted with the project.
     * {@code annotations.grouping_object_id} is an {@code @Any} without a foreign key, so
     * a survivor is an orphan row, and one loaded during the cascade fails the commit
     * with "persistent instance references an unsaved transient instance of 'null'".
     * The e2e suite hit exactly that on the dev database; H2 never did because a fresh
     * schema has no such rows - so this test manufactures them.
     */
    @Test
    void deleteProjectDeletesOrphanedAnnotationsGroupedUnderIt() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "del-orphans-" + ts);
        Goal goal = createGoal(admin, project, "orphan-goal-" + ts);
        Long unlinkedNoteId = addNote(admin, project, goal, "unlinked note " + ts).getId();
        Long danglingIssueId = addIssue(admin, project, goal, "dangling issue " + ts).getId();

        // (a) a note grouped under the project that annotates nothing any more, and
        // (b) an issue whose only link points at an annotatable row that does not exist.
        jdbcTemplate.update("DELETE FROM goals_annotations WHERE annotations_id IN (?, ?)",
                unlinkedNoteId, danglingIssueId);
        jdbcTemplate.update("DELETE FROM annotation_annotatable WHERE annotation_id = ?",
                unlinkedNoteId);
        jdbcTemplate.update(
                "UPDATE annotation_annotatable SET annotatable_id = 999999999 WHERE annotation_id = ?",
                danglingIssueId);
        assertEquals(2, getAnnotationRepository().findAnnotationIdsByGroupingObject(project).size(),
                "precondition: both annotations are still grouped under the project");

        deleteProject(admin, project, project.getVersion());

        assertThrows(NoSuchProjectException.class,
                () -> getProjectRepository().findProjectByName(project.getName()));
        assertNull(getAnnotationRepository().findAnnotationById(unlinkedNoteId),
                "an unlinked note grouped under the project must go with the project");
        assertNull(getAnnotationRepository().findAnnotationById(danglingIssueId),
                "an issue with a dangling link grouped under the project must go with the project");
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM annotation_annotatable WHERE annotation_id IN (?, ?)",
                Integer.class, unlinkedNoteId, danglingIssueId),
                "no link rows may survive the annotations");
    }

    // -------------------------------------------------------------------------
    // Permission seed + backfill
    // -------------------------------------------------------------------------

    @Test
    void projectDeletePermissionIsSeededAndCreatorHoldsIt() throws Exception {
        boolean seeded = getProjectRepository().findAvailableStakeholderPermissions().stream()
                .anyMatch(p -> PROJECT_DELETE_KEY.equals(p.getPermissionKey()));
        assertTrue(seeded, "Project[Delete] must be a seeded available permission");

        User admin = getUserRepository().findUserByUsername("admin");
        Project project = createProject(admin, "del-perm-" + System.currentTimeMillis());
        UserStakeholder creator = getProjectRepository()
                .findStakeholderByProjectOrDomainAndUser(project, admin);
        assertTrue(hasPermission(creator, PROJECT_DELETE_KEY),
                "the project creator must automatically hold Project[Delete]");
    }

    @Test
    void backfillGrantsProjectDeleteToExistingEditHolders() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "del-backfill-" + ts);

        // A stakeholder that holds Project[Edit] but NOT Project[Delete], as an
        // owner of a project created before the permission existed would.
        String ownerUsername = "del-owner-" + ts;
        createUser(ownerUsername);
        addUserStakeholder(admin, project, ownerUsername, Set.of(PROJECT_EDIT_KEY));
        User owner = getUserRepository().findUserByUsername(ownerUsername);
        UserStakeholder before = getProjectRepository()
                .findStakeholderByProjectOrDomainAndUser(project, owner);
        assertFalse(hasPermission(before, PROJECT_DELETE_KEY),
                "precondition: the stakeholder must not yet hold Project[Delete]");

        // Re-running the initializer backfills the permission.
        stakeholderPermissionsInitializer.initialize();

        UserStakeholder after = getProjectRepository()
                .findStakeholderByProjectOrDomainAndUser(project, owner);
        assertTrue(hasPermission(after, PROJECT_DELETE_KEY),
                "backfill must grant Project[Delete] to an existing Project[Edit] holder");
    }

    // -------------------------------------------------------------------------
    // Optimistic lock
    // -------------------------------------------------------------------------

    @Test
    void staleVersionIsRejectedAndNothingDeleted() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        String projectName = "del-stale-" + System.currentTimeMillis();
        Project project = createProject(admin, projectName);

        int staleVersion = project.getVersion() + 99;
        assertThrows(EntityLockException.class,
                () -> deleteProject(admin, project, staleVersion));

        // The project is still present.
        assertNotNull(getProjectRepository().findProjectByName(projectName));
    }

    // -------------------------------------------------------------------------
    // Not found
    // -------------------------------------------------------------------------

    @Test
    void deletingANonExistentProjectResolvesToCleanNotFound() {
        // The gateway resolves the project by name before building the command
        // (see ProjectCommandRegistrar's DeleteProject binder). A missing project
        // surfaces there as a clean, mapped not-found (NoSuchProjectException ->
        // 404), never a raw 500.
        String missing = "del-missing-" + System.currentTimeMillis();
        assertThrows(NoSuchProjectException.class,
                () -> getProjectRepository().findProjectByName(missing));
    }

    // -------------------------------------------------------------------------
    // Authorization
    // -------------------------------------------------------------------------

    @Test
    void stakeholderWithoutProjectDeleteCannotDelete() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        String projectName = "del-auth-" + ts;
        Project project = createProject(admin, projectName);

        // A stakeholder with Project[Edit] but not Project[Delete].
        String editorUsername = "del-editor-" + ts;
        createUser(editorUsername);
        addUserStakeholder(admin, project, editorUsername, Set.of(PROJECT_EDIT_KEY));
        User editor = getUserRepository().findUserByUsername(editorUsername);

        assertThrows(AuthorizationException.class,
                () -> deleteProject(editor, project, null));

        // Nothing was deleted.
        assertNotNull(getProjectRepository().findProjectByName(projectName));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Project createProject(User owner, String name) throws Exception {
        EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
        cmd.setEditedBy(owner);
        cmd.setName(name);
        cmd.setText("delete-project integration test");
        cmd.setOrganizationName("DelTestOrg-" + name);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    private void deleteProject(User actor, Project project, Integer expectedVersion)
            throws Exception {
        DeleteProjectCommand cmd = getProjectCommandFactory().newDeleteProjectCommand();
        cmd.setEditedBy(actor);
        cmd.setProject(project);
        cmd.setExpectedVersion(expectedVersion);
        getCommandHandler().execute(cmd);
    }

    private Goal createGoal(User actor, Project project, String name) throws Exception {
        EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
        cmd.setEditedBy(actor);
        cmd.setGoalContainer(project);
        cmd.setName(name);
        cmd.setText("goal");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getGoal();
    }

    private Actor createActor(User actor, Project project, String name) throws Exception {
        EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
        cmd.setEditedBy(actor);
        cmd.setActorContainer(project);
        cmd.setName(name);
        cmd.setText("actor");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getActor();
    }

    private Story createStory(User actor, Project project, String name) throws Exception {
        return createStory(actor, project, name, null);
    }

    private Story createStory(User actor, Project project, String name, String primaryActorName)
            throws Exception {
        EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
        cmd.setEditedBy(actor);
        cmd.setStoryContainer(project);
        cmd.setName(name);
        cmd.setText("story");
        cmd.setStoryTypeName(StoryType.Success.name());
        if (primaryActorName != null) {
            cmd.setPrimaryActorName(primaryActorName);
        }
        cmd = getCommandHandler().execute(cmd);
        return cmd.getStory();
    }

    private void addActorToContainer(User actor, Actor added, ActorContainer container)
            throws Exception {
        AddActorToActorContainerCommand cmd = getProjectCommandFactory()
                .newAddActorToActorContainerCommand();
        cmd.setEditedBy(actor);
        cmd.setActor(added);
        cmd.setActorContainer(container);
        getCommandHandler().execute(cmd);
    }

    private void addGoalToContainer(User actor, Goal added, GoalContainer container)
            throws Exception {
        AddGoalToGoalContainerCommand cmd = getProjectCommandFactory()
                .newAddGoalToGoalContainerCommand();
        cmd.setEditedBy(actor);
        cmd.setGoal(added);
        cmd.setGoalContainer(container);
        getCommandHandler().execute(cmd);
    }

    private GlossaryTerm createGlossaryTerm(User actor, Project project, String name)
            throws Exception {
        EditGlossaryTermCommand cmd = getProjectCommandFactory().newEditGlossaryTermCommand();
        cmd.setEditedBy(actor);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("glossary term");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getGlossaryTerm();
    }

    private NonUserStakeholder createNonUserStakeholder(User actor, Project project, String name)
            throws Exception {
        EditNonUserStakeholderCommand cmd =
                getProjectCommandFactory().newEditNonUserStakeholderCommand();
        cmd.setEditedBy(actor);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("non-user stakeholder");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getStakeholder();
    }

    private void createUser(String username) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(admin);
        cmd.setUsername(username);
        cmd.setPassword("test-pass");
        cmd.setRepassword("test-pass");
        cmd.setName(username);
        cmd.setEmailAddress(username + "@example.com");
        cmd.setPhoneNumber("");
        cmd.setOrganizationName("DelTestOrg");
        cmd.addUserRoleName("ProjectUserRole");
        getCommandHandler().execute(cmd);
    }

    private void addUserStakeholder(User actor, Project project, String username,
            Set<String> permissionKeys) throws Exception {
        EditUserStakeholderCommand cmd =
                getProjectCommandFactory().newEditUserStakeholderCommand();
        cmd.setEditedBy(actor);
        cmd.setProjectOrDomain(project);
        cmd.setUsername(username);
        cmd.setStakeholderPermissions(permissionKeys);
        getCommandHandler().execute(cmd);
    }

    private static boolean hasPermission(UserStakeholder stakeholder, String permissionKey) {
        return stakeholder.getStakeholderPermissions().stream()
                .anyMatch(p -> permissionKey.equals(p.getPermissionKey()));
    }
    private UseCase createUseCase(User actor, Project project, String name,
            String primaryActorName, String... stepNames) throws Exception {
        List<EditScenarioStepCommand> stepCommands = new ArrayList<>();
        for (String stepName : stepNames) {
            EditScenarioStepCommand stepCmd = getProjectCommandFactory()
                    .newEditScenarioStepCommand();
            stepCmd.setEditedBy(actor);
            stepCmd.setProjectOrDomain(project);
            stepCmd.setName(stepName);
            stepCmd.setText("Text for " + stepName);
            stepCmd.setScenarioTypeName(ScenarioType.Primary.name());
            stepCommands.add(stepCmd);
        }
        EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
        cmd.setEditedBy(actor);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("use case");
        cmd.setPrimaryActorName(primaryActorName);
        cmd.setStepCommands(stepCommands);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getUseCase();
    }

    private Scenario createScenario(User actor, Project project, String name, ScenarioType type)
            throws Exception {
        EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
        cmd.setEditedBy(actor);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("scenario");
        cmd.setScenarioTypeName(type.name());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getScenario();
    }

    private void addScenarioToUseCase(User actor, UseCase useCase, Scenario scenario)
            throws Exception {
        AddScenarioToUseCaseCommand cmd = getProjectCommandFactory()
                .newAddScenarioToUseCaseCommand();
        cmd.setEditedBy(actor);
        cmd.setUseCase(useCase);
        cmd.setScenario(scenario);
        getCommandHandler().execute(cmd);
    }

    private Note addNote(User actor, Project project, Annotatable annotatable, String text)
            throws Exception {
        EditNoteCommand cmd = getAnnotationCommandFactory().newEditNoteCommand();
        cmd.setEditedBy(actor);
        cmd.setGroupingObject(project);
        cmd.setAnnotatable(annotatable);
        cmd.setText(text);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getNote();
    }

    private Issue addIssue(User actor, Project project, Annotatable annotatable, String text)
            throws Exception {
        EditIssueCommand cmd = getAnnotationCommandFactory().newEditIssueCommand();
        cmd.setEditedBy(actor);
        cmd.setGroupingObject(project);
        cmd.setAnnotatable(annotatable);
        cmd.setText(text);
        cmd.setMustBeResolved(false);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getIssue();
    }
}
