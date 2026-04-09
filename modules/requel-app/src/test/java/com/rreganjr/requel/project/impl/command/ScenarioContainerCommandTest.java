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
package com.rreganjr.requel.project.impl.command;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.AddScenarioToUseCaseCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.RemoveScenarioFromUseCaseCommand;
import com.rreganjr.requel.project.command.SetPrimaryScenarioOnUseCaseCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for scenario-container relationship commands:
 * {@link AddScenarioToUseCaseCommand}, {@link RemoveScenarioFromUseCaseCommand},
 * and {@link SetPrimaryScenarioOnUseCaseCommand}.
 *
 * Use cases have two scenario relationships:
 * - {@code getScenario()} — the primary scenario, auto-created with the use case
 * - {@code getAdditionalScenarios()} — additional/alternative scenario flows
 *
 * {@code AddScenarioToUseCase} and {@code RemoveScenarioFromUseCase} operate on
 * the additional-scenarios collection. {@code SetPrimaryScenarioOnUseCase} swaps
 * the primary scenario reference.
 */
public class ScenarioContainerCommandTest extends AbstractIntegrationTestCase {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Project createProject(String label) throws Exception {
        long ts = System.currentTimeMillis();
        User admin = getUserRepository().findUserByUsername("admin");
        EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
        cmd.setEditedBy(admin);
        cmd.setName(label + "-" + ts);
        cmd.setText("test project for " + label);
        cmd.setOrganizationName("ScenarioContainerTestOrg-" + ts);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    private Actor createActor(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
        cmd.setEditedBy(admin);
        cmd.setActorContainer(project);
        cmd.setName(name);
        cmd.setText("An actor for scenario container tests.");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getActor();
    }

    private UseCase createUseCase(Project project, Actor primaryActor, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("A use case for scenario container tests.");
        cmd.setPrimaryActorName(primaryActor.getName());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getUseCase();
    }

    private Scenario createScenario(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("A scenario for container tests.");
        cmd.setScenarioTypeName(ScenarioType.Primary.name());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getScenario();
    }

    // -------------------------------------------------------------------------
    // AddScenarioToUseCase / RemoveScenarioFromUseCase (additional scenarios)
    // -------------------------------------------------------------------------

    @Test
    public void addScenarioToUseCase() throws Exception {
        Project project = createProject("ScenarioContainer-add");
        Actor actor = createActor(project, "Any User");
        UseCase useCase = createUseCase(project, actor, "Log in to system");
        Scenario additional = createScenario(project, "Login with SSO");

        assertTrue(useCase.getAdditionalScenarios().isEmpty(),
                "use case should have no additional scenarios before the command");

        AddScenarioToUseCaseCommand cmd =
                getProjectCommandFactory().newAddScenarioToUseCaseCommand();
        User admin = getUserRepository().findUserByUsername("admin");
        cmd.setEditedBy(admin);
        cmd.setUseCase(useCase);
        cmd.setScenario(additional);
        cmd = getCommandHandler().execute(cmd);

        UseCase updated = cmd.getUseCase();
        assertTrue(updated.getAdditionalScenarios().stream()
                        .anyMatch(s -> s.getName().equals("Login with SSO")),
                "additional scenario should appear in use case after add");
    }

    @Test
    public void removeScenarioFromUseCase() throws Exception {
        Project project = createProject("ScenarioContainer-remove");
        Actor actor = createActor(project, "Any User");
        UseCase useCase = createUseCase(project, actor, "Manage session");
        Scenario additional = createScenario(project, "Session timeout");

        // First add the scenario
        User admin = getUserRepository().findUserByUsername("admin");
        AddScenarioToUseCaseCommand addCmd =
                getProjectCommandFactory().newAddScenarioToUseCaseCommand();
        addCmd.setEditedBy(admin);
        addCmd.setUseCase(useCase);
        addCmd.setScenario(additional);
        addCmd = getCommandHandler().execute(addCmd);
        UseCase withScenario = addCmd.getUseCase();
        assertFalse(withScenario.getAdditionalScenarios().isEmpty(),
                "additional scenario should be present after add");

        // Now remove it
        RemoveScenarioFromUseCaseCommand removeCmd =
                getProjectCommandFactory().newRemoveScenarioFromUseCaseCommand();
        removeCmd.setEditedBy(admin);
        removeCmd.setUseCase(withScenario);
        removeCmd.setScenario(additional);
        removeCmd = getCommandHandler().execute(removeCmd);

        UseCase withoutScenario = removeCmd.getUseCase();
        assertTrue(withoutScenario.getAdditionalScenarios().isEmpty(),
                "additional scenario should be absent after remove");
    }

    // -------------------------------------------------------------------------
    // SetPrimaryScenarioOnUseCase
    // -------------------------------------------------------------------------

    @Test
    public void setPrimaryScenarioOnUseCase() throws Exception {
        Project project = createProject("ScenarioContainer-setPrimary");
        Actor actor = createActor(project, "Any User");
        UseCase useCase = createUseCase(project, actor, "Create account");

        // Use case auto-creates a primary scenario on creation
        Scenario originalPrimary = useCase.getScenario();
        assertNotNull(originalPrimary, "use case should have an auto-created primary scenario");

        // Create a new standalone scenario to swap in
        Scenario newPrimary = createScenario(project, "Create account via invitation");

        SetPrimaryScenarioOnUseCaseCommand cmd =
                getProjectCommandFactory().newSetPrimaryScenarioOnUseCaseCommand();
        User admin = getUserRepository().findUserByUsername("admin");
        cmd.setEditedBy(admin);
        cmd.setUseCase(useCase);
        cmd.setScenario(newPrimary);
        cmd = getCommandHandler().execute(cmd);

        UseCase updated = cmd.getUseCase();
        assertNotNull(updated.getScenario(), "use case should still have a primary scenario after swap");
        assertEquals("Create account via invitation", updated.getScenario().getName(),
                "primary scenario should have been replaced with the new one");
    }
}
