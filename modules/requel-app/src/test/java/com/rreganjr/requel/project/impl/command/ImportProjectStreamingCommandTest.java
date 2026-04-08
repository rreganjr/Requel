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
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.impl.ProjectImpl;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import com.rreganjr.requel.user.impl.repository.init.ProjectUserInitializer;
import com.rreganjr.requel.user.command.UserCommandFactory;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.project.ProjectUserRole;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test that exercises the StAX-based streaming import using the
 * large example project stored under doc/samples/Requel.xml.
 */
public class ImportProjectStreamingCommandTest extends AbstractIntegrationTestCase {

    @Autowired
    private ProjectUserInitializer projectUserInitializer;
    @Autowired
    private UserCommandFactory userCommandFactory;

    @Test
    public void streamingImportLoadsDocSample() throws Exception {
        ImportProjectCommand command = (ImportProjectCommand) applicationContext.getBean(
                "importProjectCommand");
        projectUserInitializer.initialize();
        ensureDictionaryLoaded();
        User creator = getUserRepository().findUserByUsername("project");
        ensureAssistantHasProjectRole();
        String projectName = "Streaming Sample " + System.currentTimeMillis();

        command.setEditedBy(creator);
        command.setAnalysisEnabled(false);
        command.setName(projectName);

        Path sampleXml = resolveSampleXml();
        try (InputStream inputStream = Files.newInputStream(sampleXml)) {
            command.setInputStream(inputStream);
            command = getCommandHandler().execute(command);
        }

        Project imported = command.getProject();
        assertNotNull(imported, "import produced a project");
        assertEquals(projectName, imported.getName());

        assertEquals(4, imported.getActors().size(), "expected actor count");
        assertActorAnnotations(imported, "Automated Assistant", 14);
        assertActorAnnotations(imported, "Interactive User", 6);

        assertEquals(10, imported.getGoals().size(), "expected goal count");
        assertTrue(imported.getGoals().stream()
                .map(Goal::getName)
                .anyMatch("Collaborative Elicitation of Requirements"::equals));

        assertEquals(6, imported.getStories().size(), "expected story count");
        assertTrue(imported.getStories().stream()
                .map(Story::getName)
                .anyMatch("Rich creates a new project"::equals));

        assertEquals(2, imported.getUseCases().size(), "expected use case count");
        UseCase createProject = imported.getUseCases().stream()
                .filter(uc -> "A user creates a new project".equals(uc.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("use case UC_1 not imported"));
        assertEquals("System Admin", createProject.getPrimaryActor().getName(),
                "primary actor wired");
        Scenario referencedScenario = createProject.getScenario();
        assertNotNull(referencedScenario, "scenario resolved for use case");
        assertEquals("test top level scenario", referencedScenario.getName());
        assertFalse(createProject.getStories().isEmpty(), "stories linked to use case");

        assertEquals(4, imported.getStakeholders().size(), "stakeholder count");

        Set<Annotation> annotations = ((ProjectImpl) imported).getAllProjectEntityAnnotations();
        assertEquals(265, annotations.size(), "annotation count");
        long lexicalIssueCount = annotations.stream().filter(a -> a instanceof LexicalIssue).count();
        assertEquals(145, lexicalIssueCount, "lexical issue count");

        LexicalIssue underpants = annotations.stream()
                .filter(a -> a instanceof LexicalIssue)
                .map(a -> (LexicalIssue) a)
                .filter(l -> "The phrase \"the underpants\" is a potential glossary term, actor, or domain object/property"
                        .equals(l.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing lexical issue ANN_395"));
        assertEquals(3, underpants.getPositions().size(), "lexical issue retains position refs");

        Position glossaryPosition = getAnnotationRepository()
                .findPosition(imported, "Add \"the project elements\" to the project glossary.");
        assertNotNull(glossaryPosition);
        assertEquals("Add \"the project elements\" to the project glossary.",
                glossaryPosition.getText(), "position text retained");
        assertTrue(glossaryPosition.getArguments().isEmpty(), "position has no arguments");
    }

    /**
     * Ensure the importing user is granted stakeholder membership with permissions on the imported project.
     */
    @Test
    public void importingUserGetsStakeholderPermissions() throws Exception {
        ImportProjectCommand command = (ImportProjectCommand) applicationContext.getBean("importProjectCommand");
        projectUserInitializer.initialize();
        ensureAssistantHasProjectRole();
        User creator = getUserRepository().findUserByUsername("project");

        command.setEditedBy(creator);
        command.setAnalysisEnabled(false);
        command.setName("Import Stakeholder Test " + System.currentTimeMillis());
        try (InputStream inputStream = Files.newInputStream(resolveSampleXml())) {
            command.setInputStream(inputStream);
            command = getCommandHandler().execute(command);
        }

        Project imported = command.getProject();
        Optional<UserStakeholder> maybeStakeholder = imported.getStakeholders().stream()
                .filter(s -> s instanceof UserStakeholder us && us.matchesUser(creator))
                .map(s -> (UserStakeholder) s)
                .findFirst();
        assertTrue(maybeStakeholder.isPresent(), "importing user added as stakeholder");

        UserStakeholder stakeholder = maybeStakeholder.get();
        int granted = stakeholder.getStakeholderPermissions().size();
        int available = getProjectRepository().findAvailableStakeholderPermissions().size();
        assertTrue(available > 0, "available stakeholder permissions should be seeded");
        assertEquals(available, granted, "importing user receives full stakeholder permissions");
    }

    private void ensureAssistantHasProjectRole() throws Exception {
        User assistant = getUserRepository().findUserByUsername("assistant");
        boolean hasRole = assistant.getUserRoles().stream()
                .anyMatch(role -> role instanceof ProjectUserRole);
        if (!hasRole) {
            EditUserCommand cmd = userCommandFactory.newEditUserCommand();
            cmd.setEditedBy(assistant);
            cmd.setUser(assistant);
            cmd.addUserRoleName(ProjectUserRole.getRoleName(ProjectUserRole.class));
            getCommandHandler().execute(cmd);
        }
    }

    private void assertActorAnnotations(Project project, String actorName, int expectedAnnotationCount) {
        Optional<Actor> actor = project.getActors().stream()
                .filter(a -> actorName.equals(a.getName()))
                .findFirst();
        assertTrue(actor.isPresent(), "Actor " + actorName + " imported");
        assertEquals(expectedAnnotationCount, actor.get().getAnnotations().size(),
                "annotation count for " + actorName);
    }

    private Path resolveSampleXml() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 5 && dir != null; depth++, dir = dir.getParent()) {
            Path candidate = dir.resolve("doc/samples/Requel.xml");
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to locate doc/samples/Requel.xml from working directory " +
                Paths.get("").toAbsolutePath());
    }
}
