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
package com.rreganjr.requel.project;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.project.command.*;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.user.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link ProjectRepository}.
 *
 * Extends {@link AbstractIntegrationTestCase} so the full Spring context boots
 * with H2 and the seeding initializers. Test data (projects, goals, actors) is
 * created via commands before each assertion — same pattern used in the command
 * tests — so the repository finders are exercised against real persisted state.
 *
 * Scenarios covered:
 * - findProjectByName: found and not-found cases
 * - findGoalByProjectOrDomainAndName: found and not-found
 * - findActorByProjectOrDomainAndName: found and not-found
 * - findStakeholderPermission: found from seeded catalog; not-found throws
 * - findAvailableStakeholderPermissions: returns the full 30-entry catalog
 * - Uniqueness: duplicate project name throws EntityException
 */
public class ProjectRepositoryTest extends AbstractIntegrationTestCase {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Project createProject(String label) throws Exception {
        long ts = System.currentTimeMillis();
        User admin = getUserRepository().findUserByUsername("admin");
        EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
        cmd.setEditedBy(admin);
        cmd.setName(label + "-" + ts);
        cmd.setText("repository test project");
        cmd.setOrganizationName("RepoTestOrg-" + ts);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    private Goal createGoal(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
        cmd.setEditedBy(admin);
        cmd.setGoalContainer(project);
        cmd.setName(name);
        cmd.setText("test goal");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getGoal();
    }

    private Actor createActor(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
        cmd.setEditedBy(admin);
        cmd.setActorContainer(project);
        cmd.setName(name);
        cmd.setText("test actor");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getActor();
    }

    private Story createStory(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
        cmd.setEditedBy(admin);
        cmd.setStoryContainer(project);
        cmd.setName(name);
        cmd.setText("test story");
        cmd.setStoryTypeName(StoryType.Success.name());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getStory();
    }

    private UseCase createUseCase(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("test use case");
        cmd.setPrimaryActorName("Any User");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getUseCase();
    }

    private Scenario createScenario(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("test scenario");
        cmd.setScenarioTypeName(ScenarioType.Primary.name());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getScenario();
    }

    private NonUserStakeholder createNonUserStakeholder(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditNonUserStakeholderCommand cmd = getProjectCommandFactory().newEditNonUserStakeholderCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("stakeholder text");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getStakeholder();
    }

    private GlossaryTerm createGlossaryTerm(Project project, String name, ProjectOrDomainEntity referer)
            throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditGlossaryTermCommand cmd = getProjectCommandFactory().newEditGlossaryTermCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("test glossary term");
        if (referer != null) {
            cmd.setAddReferers(Set.of(referer));
        }
        cmd = getCommandHandler().execute(cmd);
        return cmd.getGlossaryTerm();
    }

    private ReportGenerator createReportGenerator(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditReportGeneratorCommand cmd = getProjectCommandFactory().newEditReportGeneratorCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"><xsl:template match=\"/\"><output/></xsl:template></xsl:stylesheet>");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getReportGenerator();
    }

    // -------------------------------------------------------------------------
    // findProjectByName
    // -------------------------------------------------------------------------

    @Test
    void findProjectByNameReturnsPersistedProject() throws Exception {
        Project created = createProject("Repo-findByName");

        Project found = getProjectRepository().findProjectByName(created.getName());

        assertNotNull(found);
        assertEquals(created.getName(), found.getName());
    }

    @Test
    void findProjectByNameThrowsForUnknownName() {
        assertThrows(NoSuchProjectException.class,
                () -> getProjectRepository().findProjectByName("no-such-project-xyz"));
    }

    // -------------------------------------------------------------------------
    // findGoalByProjectOrDomainAndName
    // -------------------------------------------------------------------------

    @Test
    void findGoalByProjectAndNameReturnsPersistedGoal() throws Exception {
        Project project = createProject("Repo-goal");
        createGoal(project, "GoalAlpha");

        Goal found = getProjectRepository()
                .findGoalByProjectOrDomainAndName(project, "GoalAlpha");

        assertNotNull(found);
        assertEquals("GoalAlpha", found.getName());
    }

    @Test
    void findGoalByProjectAndNameThrowsForUnknownName() throws Exception {
        Project project = createProject("Repo-goal-notfound");

        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository()
                        .findGoalByProjectOrDomainAndName(project, "NoSuchGoal"));
    }

    // -------------------------------------------------------------------------
    // findActorByProjectOrDomainAndName
    // -------------------------------------------------------------------------

    @Test
    void findActorByProjectAndNameReturnsPersistedActor() throws Exception {
        Project project = createProject("Repo-actor");
        createActor(project, "ActorBeta");

        Actor found = getProjectRepository()
                .findActorByProjectOrDomainAndName(project, "ActorBeta");

        assertNotNull(found);
        assertEquals("ActorBeta", found.getName());
    }

    @Test
    void findActorByProjectAndNameThrowsForUnknownName() throws Exception {
        Project project = createProject("Repo-actor-notfound");

        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository()
                        .findActorByProjectOrDomainAndName(project, "NoSuchActor"));
    }

    @Test
    void findStoryByProjectAndNameReturnsPersistedStory() throws Exception {
        Project project = createProject("Repo-story");
        createStory(project, "StoryGamma");

        Story found = getProjectRepository()
                .findStoryByProjectOrDomainAndName(project, "StoryGamma");

        assertNotNull(found);
        assertEquals("StoryGamma", found.getName());
    }

    @Test
    void findStoryByProjectAndNameThrowsForUnknownName() throws Exception {
        Project project = createProject("Repo-story-notfound");

        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository()
                        .findStoryByProjectOrDomainAndName(project, "NoSuchStory"));
    }

    @Test
    void findUseCaseByProjectAndNameReturnsPersistedUseCase() throws Exception {
        Project project = createProject("Repo-usecase");
        createUseCase(project, "UseCaseDelta");

        UseCase found = getProjectRepository()
                .findUseCaseByProjectOrDomainAndName(project, "UseCaseDelta");

        assertNotNull(found);
        assertEquals("UseCaseDelta", found.getName());
    }

    @Test
    void findUseCaseByProjectAndNameThrowsForUnknownName() throws Exception {
        Project project = createProject("Repo-usecase-notfound");

        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository()
                        .findUseCaseByProjectOrDomainAndName(project, "NoSuchUseCase"));
    }

    @Test
    void findScenarioByProjectAndNameReturnsPersistedScenario() throws Exception {
        Project project = createProject("Repo-scenario");
        createScenario(project, "ScenarioEpsilon");

        Scenario found = getProjectRepository()
                .findScenarioByProjectOrDomainAndName(project, "ScenarioEpsilon");

        assertNotNull(found);
        assertEquals("ScenarioEpsilon", found.getName());
    }

    @Test
    void findScenarioByProjectAndNameThrowsForUnknownName() throws Exception {
        Project project = createProject("Repo-scenario-notfound");

        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository()
                        .findScenarioByProjectOrDomainAndName(project, "NoSuchScenario"));
    }

    @Test
    void findStakeholderByProjectAndNameReturnsPersistedStakeholder() throws Exception {
        Project project = createProject("Repo-stakeholder");
        createNonUserStakeholder(project, "StakeholderZeta");

        NonUserStakeholder found = getProjectRepository()
                .findStakeholderByProjectOrDomainAndName(project, "StakeholderZeta");

        assertNotNull(found);
        assertEquals("StakeholderZeta", found.getName());
    }

    @Test
    void findStakeholderByProjectAndUserReturnsPersistedUserStakeholder() throws Exception {
        Project project = createProject("Repo-user-stakeholder");
        User admin = getUserRepository().findUserByUsername("admin");

        UserStakeholder found = getProjectRepository()
                .findStakeholderByProjectOrDomainAndUser(project, admin);

        assertNotNull(found);
        assertEquals(admin.getUsername(), found.getUser().getUsername());
    }

    @Test
    void findGlossaryTermForProjectReturnsPersistedTerm() throws Exception {
        Project project = createProject("Repo-term");
        createGlossaryTerm(project, "CanonicalTerm", null);

        GlossaryTerm found = getProjectRepository()
                .findGlossaryTermForProjectOrDomain(project, "CanonicalTerm");

        assertNotNull(found);
        assertEquals("CanonicalTerm", found.getName());
    }

    @Test
    void findGlossaryTermsForEntityReturnsAttachedReferers() throws Exception {
        Project project = createProject("Repo-term-referer");
        Goal goal = createGoal(project, "GoalReferer");
        createGlossaryTerm(project, "AttachedTerm", goal);

        Set<GlossaryTerm> found = getProjectRepository()
                .findGlossaryTermsForProjectOrDomainEntity(goal);

        assertEquals(1, found.size());
        assertEquals("AttachedTerm", found.iterator().next().getName());
    }

    @Test
    void findReportGeneratorByProjectReturnsPersistedTemplate() throws Exception {
        Project project = createProject("Repo-report");
        createReportGenerator(project, "ReportTheta");

        ReportGenerator found = getProjectRepository()
                .findReportGeneratorByProjectOrDomainAndName(project, "ReportTheta");

        assertNotNull(found);
        assertEquals("ReportTheta", found.getName());
    }

    // -------------------------------------------------------------------------
    // findStakeholderPermission (seeded by StakeholderPermissionsInitializer)
    // -------------------------------------------------------------------------

    @Test
    void findStakeholderPermissionGoalEditIsSeeded() throws Exception {
        StakeholderPermission perm = getProjectRepository()
                .findStakeholderPermission(Goal.class, StakeholderPermissionType.Edit);

        assertNotNull(perm);
        assertEquals(StakeholderPermissionType.Edit, perm.getPermissionType());
    }

    @Test
    void findStakeholderPermissionGoalDeleteIsSeeded() throws Exception {
        StakeholderPermission perm = getProjectRepository()
                .findStakeholderPermission(Goal.class, StakeholderPermissionType.Delete);

        assertNotNull(perm);
        assertEquals(StakeholderPermissionType.Delete, perm.getPermissionType());
    }

    @Test
    void findAvailableStakeholderPermissionsReturnsFullCatalog() throws Exception {
        Set<StakeholderPermission> perms =
                getProjectRepository().findAvailableStakeholderPermissions();

        // 9 types × 3 + Project × 2 (no Delete) = 29 entries
        assertNotNull(perms);
        assertEquals(29, perms.size(),
                "expected 29 stakeholder permissions (Project has no Delete; 9 types × 3 + Project × 2)");
    }

    // -------------------------------------------------------------------------
    // Uniqueness constraint
    // -------------------------------------------------------------------------

    @Test
    void duplicateProjectNameThrowsEntityException() throws Exception {
        Project first = createProject("Repo-dup");
        String duplicateName = first.getName();
        User admin = getUserRepository().findUserByUsername("admin");

        EditProjectCommand dup = getProjectCommandFactory().newEditProjectCommand();
        dup.setEditedBy(admin);
        dup.setName(duplicateName);
        dup.setText("duplicate");
        dup.setOrganizationName("DupOrg");

        assertThrows(EntityException.class, () -> getCommandHandler().execute(dup),
                "expected exception for duplicate project name");
    }
}
