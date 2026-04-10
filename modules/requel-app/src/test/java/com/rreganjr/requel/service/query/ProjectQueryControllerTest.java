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
package com.rreganjr.requel.service.query;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Web-layer tests for {@link ProjectQueryController}.
 *
 * Uses {@code @SpringBootTest(webEnvironment=MOCK)} for the same reason as
 * {@code CommandControllerTest}: the application's XML-imported component scan
 * pulls in JPA-dependent beans that {@code @WebMvcTest} cannot satisfy.
 *
 * All repository and service collaborators are mocked. The controller's
 * domain-to-DTO mapping logic is the primary thing under test.
 *
 * The controller has many endpoints that share the same structure:
 *   findProjectByName → requireProjectAccess → map entities → sort → 200
 * Access control errors (404 / 403) are tested once on getProject and
 * confirmed to apply uniformly via the shared requireProjectAccess helper.
 * Entity-specific shape (DTO fields, sort order) is verified for goals,
 * actors, stories, use cases, and scenarios as representatives of the pattern.
 *
 * Scenarios covered:
 * - listProjects: project user sees their active projects sorted by name
 * - listProjects: user with no relevant role sees empty list
 * - getProject: found → 200 with ProjectDto fields populated
 * - getProject: not found → 404
 * - getProject: user is not a stakeholder → 403
 * - listGoals: 200 with goals sorted by name
 * - getGoal: found → 200 with detail DTO
 * - getGoal: id not in project's goals → 404
 * - listActors: 200 with actors sorted by name
 * - getActor: found → 200 with actor name in response
 * - listStories: 200 with stories sorted by name
 * - listUseCases: 200 with use cases sorted by name
 * - listScenarios: 200 with scenarios sorted by name
 * - listStakeholders: 200 with stakeholder list
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class ProjectQueryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ProjectRepository projectRepository;
    @MockBean ProjectCommandFactory projectCommandFactory;
    @MockBean CurrentUserResolver currentUserResolver;

    private User user;
    private Project project;
    private UserStakeholder stakeholder;

    /**
     * Default setup: a project-user who is a stakeholder on "TestProject".
     * Individual tests override parts of this as needed.
     */
    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(currentUserResolver.resolve()).thenReturn(user);
        when(user.hasRole(SystemAdminUserRole.class)).thenReturn(false);
        when(user.hasRole(ProjectUserRole.class)).thenReturn(true);

        project = stubProject("TestProject", 1L);

        stakeholder = mock(UserStakeholder.class);
        when(stakeholder.matchesUser(user)).thenReturn(true);
        when(project.getStakeholders()).thenReturn(Set.of(stakeholder));

        when(projectRepository.findProjectByName("TestProject")).thenReturn(project);
    }

    // -------------------------------------------------------------------------
    // listProjects
    // -------------------------------------------------------------------------

    @Test
    void listProjectsReturnsProjectUserActiveProjectsSortedByName() throws Exception {
        ProjectUserRole role = mock(ProjectUserRole.class);
        when(user.getRoleForType(ProjectUserRole.class)).thenReturn(role);

        Project alpha = stubProject("Alpha", 10L);
        Project zeta  = stubProject("Zeta",  20L);
        Project beta  = stubProject("Beta",  30L);
        when(role.getActiveProjects()).thenReturn(Set.of(zeta, alpha, beta));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[1].name").value("Beta"))
                .andExpect(jsonPath("$[2].name").value("Zeta"));
    }

    @Test
    void listProjectsReturnsEmptyListWhenUserHasNoRelevantRole() throws Exception {
        when(user.hasRole(ProjectUserRole.class)).thenReturn(false);

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // getProject
    // -------------------------------------------------------------------------

    @Test
    void getProjectReturnsProjectDtoWhenFound() throws Exception {
        when(project.getName()).thenReturn("TestProject");
        when(project.getText()).thenReturn("A test project");
        when(project.getGoals()).thenReturn(Set.of(mock(Goal.class), mock(Goal.class)));
        when(project.getActors()).thenReturn(Set.of(mock(Actor.class)));

        mockMvc.perform(get("/api/projects/TestProject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("TestProject"))
                .andExpect(jsonPath("$.description").value("A test project"))
                .andExpect(jsonPath("$.goalCount").value(2))
                .andExpect(jsonPath("$.actorCount").value(1));
    }

    @Test
    void getProjectReturns404WhenNotFound() throws Exception {
        when(projectRepository.findProjectByName("Missing"))
                .thenThrow(NoSuchProjectException.forName("Missing"));

        mockMvc.perform(get("/api/projects/Missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProjectReturns403WhenUserIsNotStakeholder() throws Exception {
        when(stakeholder.matchesUser(user)).thenReturn(false);

        mockMvc.perform(get("/api/projects/TestProject"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Goals
    // -------------------------------------------------------------------------

    @Test
    void listGoalsReturnsSortedGoals() throws Exception {
        Goal g1 = stubGoal(10L, "Zebra goal");
        Goal g2 = stubGoal(20L, "Alpha goal");
        when(project.getGoals()).thenReturn(Set.of(g1, g2));

        mockMvc.perform(get("/api/projects/TestProject/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha goal"))
                .andExpect(jsonPath("$[1].name").value("Zebra goal"));
    }

    @Test
    void getGoalReturnsDetailDto() throws Exception {
        Goal goal = stubGoal(42L, "My Goal");
        when(goal.getRelationsFromThisGoal()).thenReturn(Collections.emptySet());
        when(goal.getRelationsToThisGoal()).thenReturn(Collections.emptySet());
        when(goal.getReferers()).thenReturn(Collections.emptySet());
        when(project.getGoals()).thenReturn(Set.of(goal));

        mockMvc.perform(get("/api/projects/TestProject/goals/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("My Goal"));
    }

    @Test
    void getGoalReturns404WhenIdNotInProject() throws Exception {
        when(project.getGoals()).thenReturn(Collections.emptySet());

        mockMvc.perform(get("/api/projects/TestProject/goals/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Actors
    // -------------------------------------------------------------------------

    @Test
    void listActorsReturnsSortedActors() throws Exception {
        Actor a1 = stubActor(1L, "Zara");
        Actor a2 = stubActor(2L, "Alice");
        when(project.getActors()).thenReturn(Set.of(a1, a2));

        mockMvc.perform(get("/api/projects/TestProject/actors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[1].name").value("Zara"));
    }

    @Test
    void getActorReturnsDetailDto() throws Exception {
        Actor actor = stubActor(7L, "Bob");
        when(actor.getGoals()).thenReturn(Collections.emptySet());
        when(actor.getReferers()).thenReturn(Collections.emptySet());
        when(project.getActors()).thenReturn(Set.of(actor));

        mockMvc.perform(get("/api/projects/TestProject/actors/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Bob"));
    }

    // -------------------------------------------------------------------------
    // Stories
    // -------------------------------------------------------------------------

    @Test
    void listStoriesReturnsSortedStories() throws Exception {
        Story s1 = stubStory(1L, "Z story");
        Story s2 = stubStory(2L, "A story");
        when(project.getStories()).thenReturn(Set.of(s1, s2));

        mockMvc.perform(get("/api/projects/TestProject/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("A story"))
                .andExpect(jsonPath("$[1].name").value("Z story"));
    }

    // -------------------------------------------------------------------------
    // Use Cases
    // -------------------------------------------------------------------------

    @Test
    void listUseCasesReturnsSortedUseCases() throws Exception {
        UseCase uc1 = stubUseCase(1L, "Z use case");
        UseCase uc2 = stubUseCase(2L, "A use case");
        when(project.getUseCases()).thenReturn(Set.of(uc1, uc2));

        mockMvc.perform(get("/api/projects/TestProject/use-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("A use case"))
                .andExpect(jsonPath("$[1].name").value("Z use case"));
    }

    // -------------------------------------------------------------------------
    // Scenarios
    // -------------------------------------------------------------------------

    @Test
    void listScenariosReturnsSortedScenarios() throws Exception {
        Scenario sc1 = stubScenario(1L, "Z scenario");
        Scenario sc2 = stubScenario(2L, "A scenario");
        when(project.getScenarios()).thenReturn(Set.of(sc1, sc2));

        mockMvc.perform(get("/api/projects/TestProject/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("A scenario"))
                .andExpect(jsonPath("$[1].name").value("Z scenario"));
    }

    // -------------------------------------------------------------------------
    // Stakeholders
    // -------------------------------------------------------------------------

    @Test
    void listStakeholdersReturnsStakeholderList() throws Exception {
        UserStakeholder s1 = mock(UserStakeholder.class);
        when(s1.getId()).thenReturn(1L);
        when(s1.getVersion()).thenReturn(0);
        when(s1.getDisplayName()).thenReturn("Alice");
        when(s1.isUserStakeholder()).thenReturn(true);
        when(s1.matchesUser(user)).thenReturn(true);
        when(s1.getStakeholderPermissions()).thenReturn(Collections.emptySet());

        when(project.getStakeholders()).thenReturn(Set.of(s1));

        mockMvc.perform(get("/api/projects/TestProject/stakeholders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    // -------------------------------------------------------------------------
    // Stub helpers
    // -------------------------------------------------------------------------

    private Project stubProject(String name, Long id) {
        Project p = mock(Project.class);
        when(p.getId()).thenReturn(id);
        when(p.getVersion()).thenReturn(0);
        when(p.getName()).thenReturn(name);
        when(p.getText()).thenReturn(null);
        when(p.getOrganization()).thenReturn(null);
        when(p.getCreatedBy()).thenReturn(null);
        when(p.getStatus()).thenReturn("active");
        when(p.getStakeholders()).thenReturn(Collections.emptySet());
        when(p.getGoals()).thenReturn(Collections.emptySet());
        when(p.getStories()).thenReturn(Collections.emptySet());
        when(p.getActors()).thenReturn(Collections.emptySet());
        when(p.getUseCases()).thenReturn(Collections.emptySet());
        when(p.getScenarios()).thenReturn(Collections.emptySet());
        when(p.getGlossaryTerms()).thenReturn(Collections.emptySortedSet());
        when(p.getReportGenerators()).thenReturn(Collections.emptySet());
        return p;
    }

    private Goal stubGoal(Long id, String name) {
        Goal g = mock(Goal.class);
        when(g.getId()).thenReturn(id);
        when(g.getVersion()).thenReturn(0);
        when(g.getName()).thenReturn(name);
        when(g.getText()).thenReturn(null);
        when(g.getCreatedBy()).thenReturn(null);
        return g;
    }

    private Actor stubActor(Long id, String name) {
        Actor a = mock(Actor.class);
        when(a.getId()).thenReturn(id);
        when(a.getVersion()).thenReturn(0);
        when(a.getName()).thenReturn(name);
        when(a.getText()).thenReturn(null);
        when(a.getCreatedBy()).thenReturn(null);
        return a;
    }

    private Story stubStory(Long id, String name) {
        Story s = mock(Story.class);
        when(s.getId()).thenReturn(id);
        when(s.getVersion()).thenReturn(0);
        when(s.getName()).thenReturn(name);
        when(s.getText()).thenReturn(null);
        when(s.getCreatedBy()).thenReturn(null);
        when(s.getStoryType()).thenReturn(StoryType.Success);
        when(s.getPrimaryActor()).thenReturn(null);
        return s;
    }

    private UseCase stubUseCase(Long id, String name) {
        UseCase uc = mock(UseCase.class);
        when(uc.getId()).thenReturn(id);
        when(uc.getVersion()).thenReturn(0);
        when(uc.getName()).thenReturn(name);
        when(uc.getText()).thenReturn(null);
        when(uc.getCreatedBy()).thenReturn(null);
        when(uc.getPrimaryActor()).thenReturn(null);
        when(uc.getScenario()).thenReturn(null);
        return uc;
    }

    private Scenario stubScenario(Long id, String name) {
        Scenario sc = mock(Scenario.class);
        when(sc.getId()).thenReturn(id);
        when(sc.getVersion()).thenReturn(0);
        when(sc.getName()).thenReturn(name);
        when(sc.getText()).thenReturn(null);
        when(sc.getCreatedBy()).thenReturn(null);
        when(sc.getType()).thenReturn(null);
        return sc;
    }
}
