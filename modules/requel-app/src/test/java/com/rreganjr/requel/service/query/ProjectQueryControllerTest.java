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

import java.util.List;
import java.util.Collections;
import java.util.SortedSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.impl.IssueImpl;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.ExportProjectCommand;
import com.rreganjr.requel.project.command.GenerateReportCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Web-layer tests for {@link ProjectQueryController}.
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
class ProjectQueryControllerTest {

    private MockMvc mockMvc;

    private ProjectRepository projectRepository;
    private ProjectCommandFactory projectCommandFactory;
    private CommandHandler commandHandler;
    private CurrentUserResolver currentUserResolver;
    private jakarta.persistence.EntityManager entityManager;

    private User user;
    private Project project;
    private UserStakeholder stakeholder;

    /**
     * Default setup: a project-user who is a stakeholder on "TestProject".
     * Individual tests override parts of this as needed.
     */
    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        projectCommandFactory = mock(ProjectCommandFactory.class);
        commandHandler = mock(CommandHandler.class);
        currentUserResolver = mock(CurrentUserResolver.class);
        entityManager = mock(jakarta.persistence.EntityManager.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectQueryController(
                projectRepository, projectCommandFactory, commandHandler, currentUserResolver,
                entityManager)).build();

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

    @Test
    void getProjectReportsCanDeleteWhenStakeholderHoldsProjectDelete() throws Exception {
        when(stakeholder.getStakeholderPermissions()).thenReturn(
                Set.of(stubStakeholderPermission(Project.class, StakeholderPermissionType.Delete)));

        mockMvc.perform(get("/api/projects/TestProject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canDelete").value(true));
    }

    @Test
    void getProjectReportsCanDeleteFalseWithoutProjectDelete() throws Exception {
        when(stakeholder.getStakeholderPermissions()).thenReturn(
                Set.of(stubStakeholderPermission(Project.class, StakeholderPermissionType.Edit)));

        mockMvc.perform(get("/api/projects/TestProject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canDelete").value(false));
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
    // toContainerDetailDto — polymorphic container → detail DTO (issue #178 §4.1)
    // Entity interfaces are mutually exclusive, so arm order can't misclassify; these pin the
    // mapping (and the project → null decision) so a future refactor can't silently change it.
    // -------------------------------------------------------------------------

    @Test
    void toContainerDetailDtoMapsUseCaseToUseCaseDetail() {
        UseCase useCase = stubUseCase(40L, "Delta use case");
        when(useCase.getGoals()).thenReturn(Collections.emptySet());
        when(useCase.getActors()).thenReturn(Collections.emptySet());
        when(useCase.getStories()).thenReturn(Collections.emptySet());
        when(useCase.getAdditionalScenarios()).thenReturn(Collections.emptySet());

        Object dto = ProjectQueryController.toContainerDetailDto(useCase);

        assertInstanceOf(com.rreganjr.requel.service.api.dto.UseCaseDto.class, dto);
        assertEquals(40L, ((com.rreganjr.requel.service.api.dto.UseCaseDto) dto).id().longValue());
    }

    @Test
    void toContainerDetailDtoMapsStoryToStoryDetail() {
        Story story = stubStory(41L, "A story");
        when(story.getStoryType()).thenReturn(StoryType.Success);
        when(story.getGoals()).thenReturn(Collections.emptySet());
        when(story.getActors()).thenReturn(Collections.emptySet());

        Object dto = ProjectQueryController.toContainerDetailDto(story);

        assertInstanceOf(com.rreganjr.requel.service.api.dto.StoryDto.class, dto);
        assertEquals(41L, ((com.rreganjr.requel.service.api.dto.StoryDto) dto).id().longValue());
    }

    @Test
    void toContainerDetailDtoMapsActorToActorDetail() {
        Actor actor = stubActor(42L, "Bob");
        when(actor.getGoals()).thenReturn(Collections.emptySet());
        when(actor.getReferers()).thenReturn(Collections.emptySet());

        Object dto = ProjectQueryController.toContainerDetailDto(actor);

        assertInstanceOf(com.rreganjr.requel.service.api.dto.ActorDto.class, dto);
        assertEquals(42L, ((com.rreganjr.requel.service.api.dto.ActorDto) dto).id().longValue());
    }

    @Test
    void toContainerDetailDtoMapsStakeholderToStakeholderDetail() {
        UserStakeholder stakeholder = stubUserStakeholder(43L, "Alice");
        when(stakeholder.getGoals()).thenReturn(Collections.emptySet());

        Object dto = ProjectQueryController.toContainerDetailDto(stakeholder);

        assertInstanceOf(com.rreganjr.requel.service.api.dto.StakeholderDto.class, dto);
        assertEquals(43L, ((com.rreganjr.requel.service.api.dto.StakeholderDto) dto).id().longValue());
    }

    @Test
    void toContainerDetailDtoReturnsNullForProjectContainer() {
        // The project itself is a GoalContainer/StoryContainer/ActorContainer, but no component
        // subscribes to a targeted Project:<id> channel — the sidebar uses the Project:0 broadcast.
        assertNull(ProjectQueryController.toContainerDetailDto(
                mock(com.rreganjr.requel.project.ProjectOrDomain.class)));
    }

    @Test
    void toContainerDetailDtoReturnsNullForUnknownAndNullContainer() {
        assertNull(ProjectQueryController.toContainerDetailDto(new Object()));
        assertNull(ProjectQueryController.toContainerDetailDto(null));
    }

    // -------------------------------------------------------------------------
    // referencedBy mapping (issue #24) — reverse-association labels for the
    // "Referenced By" sections. These pin the referer → EntityReferenceDto mapping
    // for each container kind so the row always carries a resolvable name.
    // -------------------------------------------------------------------------

    @Test
    void toGoalDetailDtoResolvesUserStakeholderReferrerName() {
        // A UserStakeholder has no entered name; toEntityReference(GoalContainer) must use
        // getDisplayName() (falls back to the linked user), else the row shows a Type with no Name.
        Goal goal = stubGoal(50L, "Reduce churn");
        UserStakeholder ref = stubUserStakeholder(9L, "Dr. Smith");
        doReturn(UserStakeholder.class).when(ref).getProjectOrDomainEntityInterface();
        when(goal.getReferers()).thenReturn(Set.<com.rreganjr.requel.project.GoalContainer>of(ref));

        com.rreganjr.requel.service.api.dto.GoalDto dto = ProjectQueryController.toGoalDetailDto(goal);

        assertEquals(1, dto.referencedBy().size());
        assertEquals("UserStakeholder", dto.referencedBy().get(0).entityType());
        assertEquals("Dr. Smith", dto.referencedBy().get(0).name());
    }

    @Test
    void toStoryDetailDtoIncludesUseCaseReferrer() {
        Story story = stubStory(60L, "Login");
        when(story.getGoals()).thenReturn(Collections.emptySet());
        when(story.getActors()).thenReturn(Collections.emptySet());
        UseCase uc = stubUseCase(61L, "Checkout");
        when(story.getReferers()).thenReturn(Set.<com.rreganjr.requel.project.StoryContainer>of(uc));

        com.rreganjr.requel.service.api.dto.StoryDto dto = ProjectQueryController.toStoryDetailDto(story);

        assertEquals(1, dto.referencedBy().size());
        assertEquals("UseCase", dto.referencedBy().get(0).entityType());
        assertEquals("Checkout", dto.referencedBy().get(0).name());
    }

    @Test
    void toStoryDetailDtoMapsProjectReferrerToProjectType() {
        // The project itself is a StoryContainer (project-level stories), taking the
        // ProjectOrDomain arm of toEntityReference rather than the entity arm.
        Story story = stubStory(62L, "Signup");
        when(story.getGoals()).thenReturn(Collections.emptySet());
        when(story.getActors()).thenReturn(Collections.emptySet());
        Project owning = mock(Project.class);
        when(owning.getId()).thenReturn(1L);
        when(owning.getName()).thenReturn("TestProject");
        when(story.getReferers()).thenReturn(Set.<com.rreganjr.requel.project.StoryContainer>of(owning));

        com.rreganjr.requel.service.api.dto.StoryDto dto = ProjectQueryController.toStoryDetailDto(story);

        assertEquals(1, dto.referencedBy().size());
        assertEquals("Project", dto.referencedBy().get(0).entityType());
        assertEquals("TestProject", dto.referencedBy().get(0).name());
    }

    @Test
    void toScenarioDetailDtoIncludesUseCaseThatAddsIt() {
        // Scenario has no back-reference to its use cases; the referer list is found by scanning
        // the project's use cases for one whose additionalScenarios contains this scenario.
        Scenario scenario = stubScenario(70L, "Alt flow");
        Project owning = mock(Project.class);
        when(scenario.getProjectOrDomain()).thenReturn(owning);
        UseCase uc = stubUseCase(71L, "Checkout");
        when(uc.getAdditionalScenarios()).thenReturn(Set.of(scenario));
        when(owning.getUseCases()).thenReturn(Set.of(uc));

        com.rreganjr.requel.service.api.dto.ScenarioDto dto = ProjectQueryController.toScenarioDetailDto(scenario);

        assertEquals(1, dto.referencedBy().size());
        assertEquals("UseCase", dto.referencedBy().get(0).entityType());
        assertEquals("Checkout", dto.referencedBy().get(0).name());
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
        UserStakeholder s1 = stubUserStakeholder(1L, "Alice");

        when(project.getStakeholders()).thenReturn(Set.of(s1));

        mockMvc.perform(get("/api/projects/TestProject/stakeholders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    void listAvailablePermissionsReturnsCatalog() throws Exception {
        StakeholderPermission permission = stubStakeholderPermission(Goal.class, StakeholderPermissionType.Edit);
        when(projectRepository.findAvailableStakeholderPermissions()).thenReturn(Set.of(permission));

        mockMvc.perform(get("/api/projects/stakeholder-permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].entityType").value("Goal"))
                .andExpect(jsonPath("$[0].permissionType").value("Edit"));
    }

    @Test
    void getMyPermissionsReturnsStakeholderPermissions() throws Exception {
        ProjectUserRole role = mock(ProjectUserRole.class);
        StakeholderPermission permission = stubStakeholderPermission(Goal.class, StakeholderPermissionType.Edit);
        when(user.getRoleForType(ProjectUserRole.class)).thenReturn(role);
        when(role.canCreateProjects()).thenReturn(true);
        when(stakeholder.getStakeholderPermissions()).thenReturn(Set.of(permission));

        mockMvc.perform(get("/api/projects/TestProject/my-permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isStakeholder").value(true))
                .andExpect(jsonPath("$.canCreateProjects").value(true))
                .andExpect(jsonPath("$.permissions.Goal[0]").value("Edit"));
    }

    @Test
    void getProjectTreeReturnsGroupedNodes() throws Exception {
        UserStakeholder treeStakeholder = stubUserStakeholder(5L, "Zoe");
        Goal goal = stubGoal(10L, "Alpha goal");
        Story story = stubStory(20L, "Beta story");
        Actor actor = stubActor(30L, "Gamma actor");
        UseCase useCase = stubUseCase(40L, "Delta use case");
        GlossaryTerm term = stubGlossaryTerm(50L, "Epsilon term");
        ReportGenerator report = stubReportGenerator(60L, "Zeta report");

        when(project.getStakeholders()).thenReturn(Set.of(treeStakeholder));
        when(project.getGoals()).thenReturn(Set.of(goal));
        when(project.getStories()).thenReturn(Set.of(story));
        when(project.getActors()).thenReturn(Set.of(actor));
        when(project.getUseCases()).thenReturn(Set.of(useCase));
        when(project.getGlossaryTerms()).thenReturn(sortedSetOf(term));
        when(project.getReportGenerators()).thenReturn(Set.of(report));

        mockMvc.perform(get("/api/projects/TestProject/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].name").value("Stakeholders"))
                .andExpect(jsonPath("$[0].children[0].name").value("Zoe"))
                .andExpect(jsonPath("$[5].name").value("Glossary"))
                .andExpect(jsonPath("$[5].children[0].name").value("Epsilon term"))
                .andExpect(jsonPath("$[6].children[0].name").value("Zeta report"));
    }

    @Test
    void exportProjectStreamsXmlAttachment() throws Exception {
        ExportProjectCommand command = mock(ExportProjectCommand.class);
        when(projectCommandFactory.newExportProjectCommand()).thenReturn(command);

        mockMvc.perform(get("/api/projects/TestProject/export"))
                .andExpect(status().isOk());

        verify(command).setProject(project);
        verify(command).setOutputStream(any());
        verify(commandHandler).execute(command);
    }

    @Test
    void getStakeholderReturnsDetailDto() throws Exception {
        UserStakeholder s1 = stubUserStakeholder(1L, "Alice");
        Goal goal = stubGoal(11L, "Tracked goal");
        when(s1.getGoals()).thenReturn(Set.of(goal));
        when(project.getStakeholders()).thenReturn(Set.of(s1));

        mockMvc.perform(get("/api/projects/TestProject/stakeholders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.goals[0].name").value("Tracked goal"));
    }

    @Test
    void getStoryReturnsDetailDto() throws Exception {
        Story story = stubStory(7L, "My Story");
        Goal goal = stubGoal(70L, "Goal Ref");
        Actor actor = stubActor(71L, "Actor Ref");
        when(story.getGoals()).thenReturn(Set.of(goal));
        when(story.getActors()).thenReturn(Set.of(actor));
        when(project.getStories()).thenReturn(Set.of(story));

        mockMvc.perform(get("/api/projects/TestProject/stories/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.goals[0].name").value("Goal Ref"))
                .andExpect(jsonPath("$.actors[0].name").value("Actor Ref"));
    }

    @Test
    void getScenarioReturnsDetailDto() throws Exception {
        Scenario scenario = stubScenario(8L, "Main Scenario");
        Step step = stubStep(80L, "First Step");
        Scenario nestedScenario = stubScenario(81L, "Nested Scenario");
        when(scenario.getType()).thenReturn(ScenarioType.Primary);
        when(scenario.getSteps()).thenReturn(List.of(step, nestedScenario));
        when(project.getScenarios()).thenReturn(Set.of(scenario));

        mockMvc.perform(get("/api/projects/TestProject/scenarios/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.steps.length()").value(2))
                .andExpect(jsonPath("$.steps[0].name").value("First Step"))
                .andExpect(jsonPath("$.steps[1].isScenario").value(true))
                .andExpect(jsonPath("$.steps[1].scenarioId").value(81));
    }

    @Test
    void getUseCaseReturnsDetailDto() throws Exception {
        UseCase useCase = stubUseCase(9L, "My Use Case");
        Goal goal = stubGoal(90L, "Goal A");
        Actor actor = stubActor(91L, "Actor A");
        Story story = stubStory(92L, "Story A");
        Scenario primaryScenario = stubScenario(93L, "Primary Scenario");
        Scenario alternateScenario = stubScenario(94L, "Alternate Scenario");
        Step step = stubStep(95L, "Primary Step");
        when(primaryScenario.getSteps()).thenReturn(List.of(step));
        when(useCase.getScenario()).thenReturn(primaryScenario);
        when(useCase.getGoals()).thenReturn(Set.of(goal));
        when(useCase.getActors()).thenReturn(Set.of(actor));
        when(useCase.getStories()).thenReturn(Set.of(story));
        when(useCase.getAdditionalScenarios()).thenReturn(Set.of(alternateScenario));
        when(project.getUseCases()).thenReturn(Set.of(useCase));

        mockMvc.perform(get("/api/projects/TestProject/use-cases/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.scenarioName").value("Primary Scenario"))
                .andExpect(jsonPath("$.scenarioStepCount").value(1))
                .andExpect(jsonPath("$.goals[0].name").value("Goal A"))
                .andExpect(jsonPath("$.additionalScenarios[0].name").value("Alternate Scenario"));
    }

    @Test
    void listTermsReturnsSortedTerms() throws Exception {
        GlossaryTerm term1 = stubGlossaryTerm(1L, "Zeta term");
        GlossaryTerm term2 = stubGlossaryTerm(2L, "Alpha term");
        when(project.getGlossaryTerms()).thenReturn(sortedSetOf(term1, term2));

        mockMvc.perform(get("/api/projects/TestProject/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha term"))
                .andExpect(jsonPath("$[1].name").value("Zeta term"));
    }

    @Test
    void getTermReturnsDetailDto() throws Exception {
        GlossaryTerm term = stubGlossaryTerm(12L, "Canonical");
        GlossaryTerm alternate = stubGlossaryTerm(13L, "Alternate");
        Story referer = stubStory(14L, "Referer Story");
        when(term.getAlternateTerms()).thenReturn(Set.of(alternate));
        when(term.getReferers()).thenReturn(Set.of(referer));
        when(project.getGlossaryTerms()).thenReturn(sortedSetOf(term));

        mockMvc.perform(get("/api/projects/TestProject/terms/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Canonical"))
                .andExpect(jsonPath("$.alternateTerms[0].name").value("Alternate"))
                .andExpect(jsonPath("$.referers[0].name").value("Referer Story"));
    }

    @Test
    void listReportsReturnsSortedSummaries() throws Exception {
        ReportGenerator report1 = stubReportGenerator(1L, "Zeta report");
        ReportGenerator report2 = stubReportGenerator(2L, "Alpha report");
        when(project.getReportGenerators()).thenReturn(Set.of(report1, report2));

        mockMvc.perform(get("/api/projects/TestProject/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha report"))
                .andExpect(jsonPath("$[1].name").value("Zeta report"));
    }

    @Test
    void getReportReturnsDetailDto() throws Exception {
        ReportGenerator report = stubReportGenerator(15L, "HTML Spec");
        when(report.getText()).thenReturn("<xsl:stylesheet/>");
        when(project.getReportGenerators()).thenReturn(Set.of(report));

        mockMvc.perform(get("/api/projects/TestProject/reports/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("HTML Spec"))
                .andExpect(jsonPath("$.text").value("<xsl:stylesheet/>"));
    }

    @Test
    void runReportStreamsHtmlAttachment() throws Exception {
        ReportGenerator report = stubReportGenerator(16L, "HTML Spec");
        GenerateReportCommand command = mock(GenerateReportCommand.class);
        when(project.getReportGenerators()).thenReturn(Set.of(report));
        when(projectCommandFactory.newGenerateReportCommand()).thenReturn(command);

        mockMvc.perform(get("/api/projects/TestProject/reports/16/run"))
                .andExpect(status().isOk());

        verify(command).setReportGenerator(report);
        verify(command).setOutputStream(any());
        verify(commandHandler).execute(command);
    }

    @Test
    void getOpenIssuesReturnsUnresolvedIssues() throws Exception {
        Goal goal = stubGoal(21L, "Goal Entity");
        Story story = stubStory(22L, "Story Entity");
        doReturn(Goal.class).when(goal).getProjectOrDomainEntityInterface();
        doReturn(Story.class).when(story).getProjectOrDomainEntityInterface();
        Annotation goalIssue = new IssueImpl(project, "Goal issue", true, user);
        Annotation storyIssue = new IssueImpl(project, "Story issue", false, user);
        when(goal.getAnnotations()).thenReturn(Set.of(goalIssue));
        when(story.getAnnotations()).thenReturn(Set.of(storyIssue));
        when(project.getProjectEntities()).thenReturn(Set.of(goal, story));

        mockMvc.perform(get("/api/projects/TestProject/open-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].entityType").value("Goal"))
                .andExpect(jsonPath("$[0].issueText").value("Goal issue"))
                .andExpect(jsonPath("$[1].entityType").value("Story"));
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
        doReturn(Goal.class).when(g).getProjectOrDomainEntityInterface();
        return g;
    }

    private Actor stubActor(Long id, String name) {
        Actor a = mock(Actor.class);
        when(a.getId()).thenReturn(id);
        when(a.getVersion()).thenReturn(0);
        when(a.getName()).thenReturn(name);
        when(a.getText()).thenReturn(null);
        when(a.getCreatedBy()).thenReturn(null);
        doReturn(Actor.class).when(a).getProjectOrDomainEntityInterface();
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
        doReturn(Story.class).when(s).getProjectOrDomainEntityInterface();
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
        doReturn(UseCase.class).when(uc).getProjectOrDomainEntityInterface();
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
        doReturn(Scenario.class).when(sc).getProjectOrDomainEntityInterface();
        return sc;
    }

    private Step stubStep(Long id, String name) {
        Step step = mock(Step.class);
        when(step.getId()).thenReturn(id);
        when(step.getVersion()).thenReturn(0);
        when(step.getName()).thenReturn(name);
        when(step.getText()).thenReturn(null);
        when(step.getType()).thenReturn(ScenarioType.Primary);
        doReturn(Step.class).when(step).getProjectOrDomainEntityInterface();
        return step;
    }

    private UserStakeholder stubUserStakeholder(Long id, String name) {
        UserStakeholder s1 = mock(UserStakeholder.class);
        when(s1.getId()).thenReturn(id);
        when(s1.getVersion()).thenReturn(0);
        when(s1.getDisplayName()).thenReturn(name);
        when(s1.getDisplayUsername()).thenReturn(name.toLowerCase());
        when(s1.getDisplayEmailAddress()).thenReturn(name.toLowerCase() + "@example.com");
        when(s1.getDisplayPhoneNumber()).thenReturn("");
        when(s1.isUserStakeholder()).thenReturn(true);
        when(s1.matchesUser(user)).thenReturn(true);
        when(s1.getStakeholderPermissions()).thenReturn(Collections.emptySet());
        when(s1.getGoals()).thenReturn(Collections.emptySet());
        return s1;
    }

    private GlossaryTerm stubGlossaryTerm(Long id, String name) {
        GlossaryTerm term = mock(GlossaryTerm.class);
        when(term.getId()).thenReturn(id);
        when(term.getVersion()).thenReturn(0);
        when(term.getName()).thenReturn(name);
        when(term.getText()).thenReturn("definition for " + name);
        when(term.getCreatedBy()).thenReturn(null);
        when(term.getCanonicalTerm()).thenReturn(null);
        when(term.getAlternateTerms()).thenReturn(Collections.emptySet());
        when(term.getReferers()).thenReturn(Collections.emptySet());
        doReturn(GlossaryTerm.class).when(term).getProjectOrDomainEntityInterface();
        return term;
    }

    private ReportGenerator stubReportGenerator(Long id, String name) {
        ReportGenerator report = mock(ReportGenerator.class);
        when(report.getId()).thenReturn(id);
        when(report.getVersion()).thenReturn(0);
        when(report.getName()).thenReturn(name);
        when(report.getText()).thenReturn("<xsl:stylesheet/>");
        when(report.getCreatedBy()).thenReturn(null);
        doReturn(ReportGenerator.class).when(report).getProjectOrDomainEntityInterface();
        return report;
    }

    private StakeholderPermission stubStakeholderPermission(Class<?> entityType,
            StakeholderPermissionType permissionType) {
        StakeholderPermission permission = mock(StakeholderPermission.class);
        doReturn(entityType).when(permission).getEntityType();
        when(permission.getPermissionType()).thenReturn(permissionType);
        when(permission.getPermissionKey()).thenReturn(entityType.getSimpleName() + ":" + permissionType.name());
        return permission;
    }

    @SafeVarargs
    private final <T> SortedSet<T> sortedSetOf(T... values) {
        SortedSet<T> result = new TreeSet<>((left, right) -> {
            if (left == right) {
                return 0;
            }
            return Integer.compare(System.identityHashCode(left), System.identityHashCode(right));
        });
        result.addAll(List.of(values));
        return result;
    }
}
