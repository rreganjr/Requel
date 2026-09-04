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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.EntityLockException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.project.command.DeleteProjectCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
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
        EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
        cmd.setEditedBy(actor);
        cmd.setStoryContainer(project);
        cmd.setName(name);
        cmd.setText("story");
        cmd.setStoryTypeName(StoryType.Success.name());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getStory();
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
}
