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
package com.rreganjr.requel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.StakeholderPermissionRules;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.DeleteGoalCommand;
import com.rreganjr.requel.project.command.DeleteUseCaseCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditGoalRelationCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #75: stakeholder permission coherence.
 * <ul>
 * <li>A delete's steps - detaching the deleted entity, deleting what only it owned - are
 * authorized by the delete's own permission, and only as its steps.</li>
 * <li>Granting a permission grants what it implies (UseCase[Edit] → Scenario[Edit],
 * Actor[Edit]).</li>
 * <li>Granting or removing a permission needs that permission and its Grant.</li>
 * <li>Creating or importing a project needs {@code createProjects}; {@code /projectxml} is
 * gone.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StakeholderPermissionCoherenceIT extends AbstractIntegrationTestCase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long ts;
    private User owner;
    private Project project;

    @BeforeAll
    void setUpFixture() throws Exception {
        initializeBaselineData();
        ts = System.currentTimeMillis();
        owner = getUserRepository().findUserByUsername("admin");
        EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
        projectCmd.setEditedBy(owner);
        projectCmd.setName("coherence-" + ts);
        projectCmd.setText("#75");
        projectCmd.setOrganizationName("CoherenceOrg-" + ts);
        project = getCommandHandler().execute(projectCmd).getProject();
    }

    // ---- granting -----------------------------------------------------------------------

    @Test
    void stakeholderEditAloneCanChangeTheTeamButGrantNothingNotEvenToItself() throws Exception {
        User steward = user("steward");
        User member = user("member-a");
        setPermissions(owner, steward, keys(edit(Stakeholder.class)), null);
        setPermissions(owner, member, Set.of(), null);

        assertRefused(() -> setPermissions(steward, member, keys(edit(Goal.class)), null));
        assertRefused(() -> setPermissions(steward, steward,
                keys(edit(Stakeholder.class), edit(Goal.class)), null));

        setPermissions(steward, member, Set.of(), "Reviewers");
        assertThat(stakeholder(member).getTeam().getName()).isEqualTo("Reviewers");
    }

    @Test
    void grantingAndRemovingNeedThePermissionAndItsGrant() throws Exception {
        User lead = user("lead");
        User member = user("member-b");
        setPermissions(owner, lead, keys(edit(Stakeholder.class), edit(Goal.class),
                grant(Goal.class)), null);
        setPermissions(owner, member, Set.of(), null);

        setPermissions(lead, member, keys(edit(Goal.class)), null);
        assertThat(held(member)).containsExactly(edit(Goal.class));

        assertRefused(() -> setPermissions(lead, member,
                keys(edit(Goal.class), delete(Goal.class)), null));
        assertRefused(() -> setPermissions(lead, member,
                keys(edit(Goal.class), edit(com.rreganjr.requel.project.Story.class)), null));

        setPermissions(lead, member, Set.of(), null);
        assertThat(held(member)).isEmpty();
    }

    @Test
    void useCaseEditBringsScenarioAndActorEditWhichStayWhenItIsRemoved() throws Exception {
        User member = user("member-c");
        setPermissions(owner, member, keys(edit(UseCase.class)), null);
        assertThat(held(member)).containsExactlyInAnyOrder(edit(UseCase.class),
                edit(Scenario.class), edit(Actor.class));

        // an implied permission can't be removed while what implies it is held
        setPermissions(owner, member, keys(edit(UseCase.class), edit(Actor.class)), null);
        assertThat(held(member)).contains(edit(Scenario.class));

        // and stays, editable, when it is removed
        setPermissions(owner, member, keys(edit(Scenario.class), edit(Actor.class)), null);
        assertThat(held(member)).containsExactlyInAnyOrder(edit(Scenario.class),
                edit(Actor.class));
    }

    @Test
    void aUseCaseEditorCanCreateAUseCase() throws Exception {
        User writer = user("writer");
        setPermissions(owner, writer, keys(edit(UseCase.class)), null);

        UseCase useCase = createUseCase(writer, "uc-" + ts, "Librarian-" + ts);

        assertThat(useCase.getScenario()).isNotNull();
        assertThat(useCase.getPrimaryActor().getName()).isEqualTo("Librarian-" + ts);
    }

    // ---- cascades -------------------------------------------------------------------------

    @Test
    void aGoalDeleterDeletesTheGoalItsRelationsAndTheNotesOnlyOnIt() throws Exception {
        User deleter = user("goal-deleter");
        setPermissions(owner, deleter, keys(delete(Goal.class)), null);
        Goal doomed = createGoal(owner, "doomed-" + ts);
        Goal other = createGoal(owner, "other-" + ts);
        relate(owner, doomed, other);
        Long noteId = addNote(owner, doomed, "only here " + ts).getId();

        // the steps are not the deleter's to run on their own
        assertRefused(() -> {
            RemoveGoalFromGoalContainerCommand detach = getProjectCommandFactory()
                    .newRemoveGoalFromGoalContainerCommand();
            detach.setEditedBy(deleter);
            detach.setGoal(doomed);
            detach.setGoalContainer(project);
            getCommandHandler().execute(detach);
        });
        assertRefused(() -> {
            DeleteNoteCommand deleteNote = getAnnotationCommandFactory().newDeleteNoteCommand();
            deleteNote.setEditedBy(deleter);
            deleteNote.setNote((Note) getAnnotationRepository().findAnnotationById(noteId));
            getCommandHandler().execute(deleteNote);
        });

        DeleteGoalCommand delete = getProjectCommandFactory().newDeleteGoalCommand();
        delete.setEditedBy(deleter);
        delete.setGoal(doomed);
        getCommandHandler().execute(delete);

        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Goal.class, doomed.getId()));
        assertThat(getAnnotationRepository().findAnnotationById(noteId)).isNull();
        assertThat(getProjectRepository().findById(Goal.class, other.getId())).isNotNull();
    }

    @Test
    void aUseCaseDeleterDeletesTheScenarioItOwned() throws Exception {
        User deleter = user("uc-deleter");
        setPermissions(owner, deleter, keys(delete(UseCase.class)), null);
        UseCase doomed = createUseCase(owner, "doomed-uc-" + ts, "Clerk-" + ts);
        Long scenarioId = doomed.getScenario().getId();

        DeleteUseCaseCommand delete = getProjectCommandFactory().newDeleteUseCaseCommand();
        delete.setEditedBy(deleter);
        delete.setUseCase(doomed);
        getCommandHandler().execute(delete);

        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Scenario.class, scenarioId));
    }

    // ---- creating projects, /projectxml, the rules read ------------------------------------

    @Test
    void creatingOrImportingAProjectNeedsCreateProjects() throws Exception {
        User plain = user("no-create");
        assertRefused(() -> {
            EditProjectCommand create = getProjectCommandFactory().newEditProjectCommand();
            create.setEditedBy(plain);
            create.setName("not-allowed-" + ts);
            create.setOrganizationName("Nope-" + ts);
            getCommandHandler().execute(create);
        });
        assertRefused(() -> {
            ImportProjectCommand importCmd = getProjectCommandFactory().newImportProjectCommand();
            importCmd.setEditedBy(plain);
            importCmd.setName("not-imported-" + ts);
            importCmd.setInputStream(new ByteArrayInputStream(new byte[0]));
            getCommandHandler().execute(importCmd);
        });
        assertThrows(Exception.class,
                () -> getProjectRepository().findProjectByName("not-allowed-" + ts));
    }

    @Test
    void projectXmlNoLongerExportsAnything() throws Exception {
        String body = mockMvc.perform(get("/projectxml").param("project", project.getName()))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(project.getName());
    }

    @Test
    void thePermissionRulesAreReadable() throws Exception {
        String token = login("admin", "admin");
        mockMvc.perform(get("/api/projects/stakeholder-permission-rules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.implied[0].granted").value(edit(UseCase.class)))
                .andExpect(jsonPath("$.ownedDeletes").isNotEmpty())
                .andExpect(jsonPath("$.grantKeys['"
                        + StakeholderPermissionRules.key(
                                com.rreganjr.requel.project.AssistantDefinition.class,
                                StakeholderPermissionType.Edit)
                        + "']").value(grant(Project.class)));
    }

    // ---- helpers ------------------------------------------------------------------------

    private static String edit(Class<?> type) {
        return StakeholderPermissionRules.key(type, StakeholderPermissionType.Edit);
    }

    private static String delete(Class<?> type) {
        return StakeholderPermissionRules.key(type, StakeholderPermissionType.Delete);
    }

    private static String grant(Class<?> type) {
        return StakeholderPermissionRules.key(type, StakeholderPermissionType.Grant);
    }

    private static Set<String> keys(String... keys) {
        return new HashSet<>(Set.of(keys));
    }

    private User user(String name) throws Exception {
        String username = name + "-" + ts;
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(getUserRepository().findUserByUsername("admin"));
        cmd.setUsername(username);
        cmd.setPassword("pw-" + name);
        cmd.setRepassword("pw-" + name);
        cmd.setName(username);
        cmd.setEmailAddress(username + "@example.com");
        cmd.setPhoneNumber("");
        cmd.setOrganizationName("CoherenceOrg-" + ts);
        cmd.addUserRoleName("ProjectUserRole");
        getCommandHandler().execute(cmd);
        return getUserRepository().findUserByUsername(username);
    }

    private UserStakeholder stakeholder(User user) throws Exception {
        return getProjectRepository().findStakeholderByProjectOrDomainAndUser(
                getProjectRepository().findProjectByName(project.getName()), user);
    }

    private Set<String> held(User user) throws Exception {
        return stakeholder(user).getStakeholderPermissions().stream()
                .map(StakeholderPermission::getPermissionKey).collect(Collectors.toSet());
    }

    private void setPermissions(User editor, User target, Set<String> permissions, String team)
            throws Exception {
        EditUserStakeholderCommand cmd = getProjectCommandFactory().newEditUserStakeholderCommand();
        cmd.setEditedBy(editor);
        cmd.setProjectOrDomain(getProjectRepository().findProjectByName(project.getName()));
        cmd.setUsername(target.getUsername());
        try {
            cmd.setStakeholder(stakeholder(target));
        } catch (NoSuchEntityException e) {
            // a new stakeholder
        }
        cmd.setStakeholderPermissions(permissions);
        cmd.setTeamName(team);
        getCommandHandler().execute(cmd);
    }

    private static void assertRefused(Executable action) {
        try {
            action.execute();
        } catch (Throwable t) {
            for (Throwable cause = t; cause != null; cause = cause.getCause()) {
                if (cause instanceof AuthorizationException) {
                    return;
                }
            }
            fail("expected an AuthorizationException, got " + t, t);
        }
        fail("expected an AuthorizationException, but the command ran");
    }

    private Goal createGoal(User actor, String name) throws Exception {
        EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
        cmd.setEditedBy(actor);
        cmd.setGoalContainer(getProjectRepository().findProjectByName(project.getName()));
        cmd.setName(name);
        cmd.setText("goal");
        return getCommandHandler().execute(cmd).getGoal();
    }

    private void relate(User actor, Goal from, Goal to) throws Exception {
        EditGoalRelationCommand cmd = getProjectCommandFactory().newEditGoalRelationCommand();
        cmd.setEditedBy(actor);
        cmd.setProjectOrDomain(getProjectRepository().findProjectByName(project.getName()));
        cmd.setFromGoal(from.getName());
        cmd.setToGoal(to.getName());
        cmd.setRelationType("Supports");
        getCommandHandler().execute(cmd);
    }

    private Note addNote(User actor, Goal goal, String text) throws Exception {
        EditNoteCommand cmd = getAnnotationCommandFactory().newEditNoteCommand();
        cmd.setEditedBy(actor);
        cmd.setGroupingObject(getProjectRepository().findProjectByName(project.getName()));
        cmd.setAnnotatable(goal);
        cmd.setText(text);
        return getCommandHandler().execute(cmd).getNote();
    }

    private UseCase createUseCase(User actor, String name, String actorName) throws Exception {
        Project current = getProjectRepository().findProjectByName(project.getName());
        EditScenarioStepCommand step = getProjectCommandFactory().newEditScenarioStepCommand();
        step.setEditedBy(actor);
        step.setProjectOrDomain(current);
        step.setName("step for " + name);
        step.setText("step");
        step.setScenarioTypeName(ScenarioType.Primary.name());
        EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
        cmd.setEditedBy(actor);
        cmd.setProjectOrDomain(current);
        cmd.setName(name);
        cmd.setText("use case");
        cmd.setPrimaryActorName(actorName);
        cmd.setStepCommands(java.util.List.of(step));
        return getCommandHandler().execute(cmd).getUseCase();
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
