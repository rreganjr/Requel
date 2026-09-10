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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.project.command.AddScenarioToUseCaseCommand;
import com.rreganjr.requel.project.command.DeleteGoalCommand;
import com.rreganjr.requel.project.command.DeleteScenarioCommand;
import com.rreganjr.requel.project.command.DeleteUseCaseCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.SetPrimaryScenarioOnUseCaseCommand;
import com.rreganjr.requel.user.User;

/**
 * The #247 delete-cascade edge cases that a single-threaded H2 run never reaches on
 * its own: an annotation linked to an entity <em>after</em> the delete loaded it
 * (the assistant race), a scenario that is another use case's additional scenario,
 * and a primary scenario shared by two use cases. See
 * {@code doc/247-delete-project-cascade-hardening-plan.md}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeleteCascadeIT extends AbstractIntegrationTestCase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void setUp() throws Exception {
        initializeBaselineData();
    }

    /**
     * The sweep ({@code RemoveAllAnnotationsFromAnnotatableCommand}) exists for links
     * the per-annotation loop cannot see: rows in {@code annotation_annotatable} that
     * are not mirrored in the entity's own {@code goals_annotations} collection. That is
     * exactly what a background assistant leaves when it links a finding between the
     * delete's load and its flush, so manufacture that state: a note shared with another
     * goal (must survive, unlinked from the deleted goal only), and a note and an issue
     * whose only link is the doomed goal (must be deleted with it).
     */
    @Test
    void deleteGoalSweepsLinksTheEntityCollectionDoesNotSee() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "sweep-" + ts);
        Goal doomed = createGoal(admin, project, "sweep-doomed-" + ts);
        Goal survivor = createGoal(admin, project, "sweep-survivor-" + ts);

        Long sharedNoteId = addNote(admin, project, survivor, "shared note " + ts).getId();
        Long lateNoteId = addNote(admin, project, doomed, "late note " + ts).getId();
        Long lateIssueId = addIssue(admin, project, doomed, "late issue " + ts).getId();

        // Late link: the shared note also points at the doomed goal, but only in the
        // @ManyToAny table - the goal's own collection never saw it.
        jdbcTemplate.update("INSERT INTO annotation_annotatable "
                + "(annotation_id, annotatable_type, annotatable_id) VALUES (?, 'Goal', ?)",
                sharedNoteId, doomed.getId());
        // Same for the goal's own note and issue: drop the collection side so the
        // per-annotation loop sees nothing and only the sweep can find them.
        jdbcTemplate.update("DELETE FROM goals_annotations WHERE annotations_id IN (?, ?)",
                lateNoteId, lateIssueId);

        deleteGoal(admin, doomed);

        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Goal.class, doomed.getId()));
        assertEquals(0, countLinks(doomed.getId()),
                "no annotation_annotatable row may still point at the deleted goal");

        Annotation sharedNote = getAnnotationRepository().findAnnotationById(sharedNoteId);
        assertNotNull(sharedNote, "a note still annotating another goal must survive");
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM annotation_annotatable WHERE annotation_id = ?",
                Integer.class, sharedNoteId), "the shared note keeps only its surviving link");
        assertNull(getAnnotationRepository().findAnnotationById(lateNoteId),
                "a note whose only link was the deleted goal is deleted (DeleteNote path)");
        assertNull(getAnnotationRepository().findAnnotationById(lateIssueId),
                "an issue whose only link was the deleted goal is deleted (DeleteIssue path)");

        assertNotNull(getProjectRepository().findById(Goal.class, survivor.getId()));
    }

    /**
     * A project-level scenario attached to a use case as an additional scenario lives in
     * {@code usecase_scenarios} (owned by the use case, no inverse mapping) with an FK to
     * the scenario row. DeleteScenario must detach it from every use case first.
     */
    @Test
    void deleteScenarioDetachesItFromUseCasesAdditionalScenarios() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "scn-detach-" + ts);
        UseCase useCase = createUseCase(admin, project, "scn-detach-uc-" + ts,
                "scn-detach-actor-" + ts, "step one " + ts);
        Scenario alternative = createScenario(admin, project, "scn-detach-alt-" + ts,
                ScenarioType.Alternative);
        addScenarioToUseCase(admin, useCase, alternative);
        assertTrue(getProjectRepository().findById(UseCase.class, useCase.getId())
                .getAdditionalScenarios().contains(alternative),
                "precondition: the scenario is one of the use case's additional scenarios");

        deleteScenario(admin, alternative);

        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Scenario.class, alternative.getId()));
        UseCase reloaded = getProjectRepository().findById(UseCase.class, useCase.getId());
        assertFalse(reloaded.getAdditionalScenarios().contains(alternative),
                "the use case no longer lists the deleted scenario");
        assertNotNull(reloaded.getScenario(), "the use case's primary scenario is untouched");
    }

    /**
     * Two use cases can share one primary scenario ({@code SetPrimaryScenarioOnUseCase}).
     * Deleting one of them must delete only what it alone owned; the shared primary
     * scenario stays for the other use case.
     */
    @Test
    void deleteUseCaseSparesAPrimaryScenarioSharedWithAnotherUseCase() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "uc-shared-" + ts);
        UseCase keeper = createUseCase(admin, project, "uc-keeper-" + ts,
                "uc-shared-actor-" + ts, "keeper step " + ts);
        UseCase doomed = createUseCase(admin, project, "uc-doomed-" + ts,
                "uc-shared-actor-" + ts, "doomed step " + ts);
        Long sharedScenarioId = keeper.getScenario().getId();
        Long doomedOwnScenarioId = doomed.getScenario().getId();

        SetPrimaryScenarioOnUseCaseCommand share = getProjectCommandFactory()
                .newSetPrimaryScenarioOnUseCaseCommand();
        share.setEditedBy(admin);
        share.setUseCase(doomed);
        share.setScenario(keeper.getScenario());
        getCommandHandler().execute(share);
        assertEquals(sharedScenarioId,
                getProjectRepository().findById(UseCase.class, doomed.getId()).getScenario().getId(),
                "precondition: both use cases now have the same primary scenario");

        deleteUseCase(admin, doomed);

        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(UseCase.class, doomed.getId()));
        UseCase reloadedKeeper = getProjectRepository().findById(UseCase.class, keeper.getId());
        assertEquals(sharedScenarioId, reloadedKeeper.getScenario().getId(),
                "the shared primary scenario survives for the other use case");
        assertNotNull(getProjectRepository().findById(Scenario.class, sharedScenarioId));
        // The scenario the deleted use case used to own before it switched primaries is
        // no longer referenced by any use case; it is a project-level scenario and stays.
        assertNotNull(getProjectRepository().findById(Scenario.class, doomedOwnScenarioId));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int countLinks(Long annotatableId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM annotation_annotatable WHERE annotatable_id = ?",
                Integer.class, annotatableId);
    }

    private Project createProject(User owner, String name) throws Exception {
        EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
        cmd.setEditedBy(owner);
        cmd.setName(name);
        cmd.setText("delete-cascade integration test");
        cmd.setOrganizationName("DelCascadeOrg-" + name);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
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

    private void deleteGoal(User actor, Goal goal) throws Exception {
        DeleteGoalCommand cmd = getProjectCommandFactory().newDeleteGoalCommand();
        cmd.setEditedBy(actor);
        cmd.setGoal(goal);
        getCommandHandler().execute(cmd);
    }

    private void deleteScenario(User actor, Scenario scenario) throws Exception {
        DeleteScenarioCommand cmd = getProjectCommandFactory().newDeleteScenarioCommand();
        cmd.setEditedBy(actor);
        cmd.setScenario(scenario);
        getCommandHandler().execute(cmd);
    }

    private void deleteUseCase(User actor, UseCase useCase) throws Exception {
        DeleteUseCaseCommand cmd = getProjectCommandFactory().newDeleteUseCaseCommand();
        cmd.setEditedBy(actor);
        cmd.setUseCase(useCase);
        getCommandHandler().execute(cmd);
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
