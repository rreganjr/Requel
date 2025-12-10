/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration test that exercises the StAX-based streaming import using the
 * large example project stored under doc/samples/Requel.xml.
 */
@RunWith(SpringRunner.class)
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
        assertNotNull("import produced a project", imported);
        org.junit.Assert.assertEquals(projectName, imported.getName());

        org.junit.Assert.assertEquals("expected actor count", 4, imported.getActors().size());
        assertActorAnnotations(imported, "Automated Assistant", 14);
        assertActorAnnotations(imported, "Interactive User", 6);

        org.junit.Assert.assertEquals("expected goal count", 10, imported.getGoals().size());
        assertTrue(imported.getGoals().stream()
                .map(Goal::getName)
                .anyMatch("Collaborative Elicitation of Requirements"::equals));

        org.junit.Assert.assertEquals("expected story count", 6, imported.getStories().size());
        assertTrue(imported.getStories().stream()
                .map(Story::getName)
                .anyMatch("Rich creates a new project"::equals));

        org.junit.Assert.assertEquals("expected use case count", 2, imported.getUseCases().size());
        UseCase createProject = imported.getUseCases().stream()
                .filter(uc -> "A user creates a new project".equals(uc.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("use case UC_1 not imported"));
        org.junit.Assert.assertEquals("primary actor wired", "System Admin",
                createProject.getPrimaryActor().getName());
        Scenario referencedScenario = createProject.getScenario();
        assertNotNull("scenario resolved for use case", referencedScenario);
        org.junit.Assert.assertEquals("test top level scenario", referencedScenario.getName());
        assertFalse("stories linked to use case", createProject.getStories().isEmpty());

        org.junit.Assert.assertEquals("stakeholder count", 4, imported.getStakeholders().size());

        Set<Annotation> annotations = ((ProjectImpl) imported).getAllProjectEntityAnnotations();
        org.junit.Assert.assertEquals("annotation count", 265, annotations.size());
        long lexicalIssueCount = annotations.stream().filter(a -> a instanceof LexicalIssue).count();
        org.junit.Assert.assertEquals("lexical issue count", 145, lexicalIssueCount);

        LexicalIssue underpants = annotations.stream()
                .filter(a -> a instanceof LexicalIssue)
                .map(a -> (LexicalIssue) a)
                .filter(l -> "The phrase \"the underpants\" is a potential glossary term, actor, or domain object/property"
                        .equals(l.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing lexical issue ANN_395"));
        org.junit.Assert.assertEquals("lexical issue retains position refs", 3,
                underpants.getPositions().size());

        Position glossaryPosition = getAnnotationRepository()
                .findPosition(imported, "Add \"the project elements\" to the project glossary.");
        assertNotNull(glossaryPosition);
        org.junit.Assert.assertEquals("position text retained",
                "Add \"the project elements\" to the project glossary.", glossaryPosition.getText());
        assertTrue("position has no arguments", glossaryPosition.getArguments().isEmpty());
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
        assertTrue("importing user added as stakeholder", maybeStakeholder.isPresent());

        UserStakeholder stakeholder = maybeStakeholder.get();
        int granted = stakeholder.getStakeholderPermissions().size();
        int available = getProjectRepository().findAvailableStakeholderPermissions().size();
        org.junit.Assert.assertTrue("available stakeholder permissions should be seeded", available > 0);
        org.junit.Assert.assertEquals("importing user receives full stakeholder permissions", available, granted);
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
        assertTrue("Actor " + actorName + " imported", actor.isPresent());
        org.junit.Assert.assertEquals("annotation count for " + actorName,
                expectedAnnotationCount, actor.get().getAnnotations().size());
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
