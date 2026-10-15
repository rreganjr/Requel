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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.project.command.AddActorToActorContainerCommand;
import com.rreganjr.requel.project.command.AddGoalToGoalContainerCommand;
import com.rreganjr.requel.project.command.AddStoryToStoryContainerCommand;
import com.rreganjr.requel.project.command.DeleteStoryCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.command.RemoveActorFromActorContainerCommand;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.project.command.RemoveStoryFromStoryContainerCommand;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * #323: {@code em.refresh(entity)} used to drag in every active project of every user its eager
 * graph reached - each entity's {@code createdBy} user, eagerly with its roles, and
 * {@code ProjectUserRole.activeProjects} eagerly left-joined on top, one chain per user
 * reference, multiplying to ~N^6 rows for a user on N projects. Two properties pin the fix:
 * <ul>
 * <li>the refresh graph is the same size however many projects the users in it are on, and never
 * initializes {@code activeProjects} (measured on a private session, so nothing else running in
 * the context can skew it);</li>
 * <li>each command that refreshes after a native join-table write - the seven in
 * {@code project-jpa} plus one annotation command - still does its job and never loads
 * {@code activeProjects} (measured with SessionFactory statistics).</li>
 * </ul>
 * See {@code doc/work/2.0/323-refresh-graph-plan.md}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RefreshGraphIT extends AbstractIntegrationTestCase {

    private static final String ACTIVE_PROJECTS_ROLE = ProjectUserRole.class.getName() + ".activeProjects";
    /** Projects added between the narrow and wide measurements. */
    private static final int EXTRA_PROJECTS = 12;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;
    private boolean statisticsWereEnabled;

    @BeforeAll
    void setUp() throws Exception {
        initializeBaselineData();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statisticsWereEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
    }

    @AfterAll
    void tearDown() {
        statistics.setStatisticsEnabled(statisticsWereEnabled);
    }

    // -------------------------------------------------------------------------
    // The refresh graph does not scale with active projects
    // -------------------------------------------------------------------------

    @Test
    void refreshGraphDoesNotGrowWithTheCreatorsActiveProjects() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "rg-graph-" + ts);
        Story story = createStory(admin, project, "rg-graph-story-" + ts, null);
        Actor actor = createActor(admin, project, "rg-graph-actor-" + ts);
        Goal goal = createGoal(admin, project, "rg-graph-goal-" + ts);
        Note note = addNote(admin, project, goal, "rg-graph-note " + ts);

        Map<String, Integer> narrow = measureRefreshGraphs(story, actor, goal, note);

        // Put the creator on EXTRA_PROJECTS more projects. Before #323 every one of them (and
        // everything under it) joined into each refresh below.
        for (int i = 0; i < EXTRA_PROJECTS; i++) {
            createProject(admin, "rg-graph-extra-" + ts + "-" + i);
        }
        assertTrue(getProjectRepository().findActiveProjects(admin).size() > EXTRA_PROJECTS,
                "precondition: the creator is now on more than " + EXTRA_PROJECTS + " projects");

        Map<String, Integer> wide = measureRefreshGraphs(story, actor, goal, note);

        assertEquals(narrow, wide,
                "the entities a refresh loads must not depend on how many projects its users are on");
    }

    /**
     * Refresh each entity on a private session and return the number of entities that session
     * ended up holding, keyed by entity type. Also asserts that the creator's
     * {@code activeProjects} was left uninitialized.
     */
    private Map<String, Integer> measureRefreshGraphs(Story story, Actor actor, Goal goal, Note note) {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        sizes.put("Story", refreshGraphSize(StoryImpl.class, story.getId()));
        sizes.put("Actor", refreshGraphSize(ActorImpl.class, actor.getId()));
        sizes.put("Goal", refreshGraphSize(GoalImpl.class, goal.getId()));
        sizes.put("Note", refreshGraphSize(AbstractAnnotation.class, note.getId()));
        return sizes;
    }

    private int refreshGraphSize(Class<?> entityType, Long id) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            Object entity = em.find(entityType, id);
            assertNotNull(entity, entityType.getSimpleName() + " " + id + " must exist");
            em.refresh(entity);

            com.rreganjr.platform.identity.User creator = entity instanceof AbstractAnnotation a
                    ? a.getCreatedBy()
                    : ((com.rreganjr.requel.project.impl.AbstractProjectOrDomainEntity) entity).getCreatedBy();
            ProjectUserRole role = creator.getRoleForType(ProjectUserRole.class);
            assertNotNull(role, "fixture: the creator holds a ProjectUserRole");
            assertFalse(Hibernate.isInitialized(role.getActiveProjects()),
                    "refreshing a " + entityType.getSimpleName()
                            + " must not initialize its creator's activeProjects");

            return em.unwrap(Session.class).getStatistics().getEntityCount();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    // -------------------------------------------------------------------------
    // Each refreshing command still works and never loads activeProjects
    // -------------------------------------------------------------------------

    @Test
    void storyAddAndRemoveNeverLoadActiveProjects() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "rg-story-" + ts);
        UseCase container = createUseCase(admin, project, "rg-story-uc-" + ts,
                "rg-story-uc-actor-" + ts, "rg story step " + ts);
        Story story = createStory(admin, project, "rg-story-" + ts, null);

        withoutLoadingActiveProjects("AddStoryToStoryContainer", () -> {
            AddStoryToStoryContainerCommand cmd = getProjectCommandFactory()
                    .newAddStoryToStoryContainerCommand();
            cmd.setEditedBy(admin);
            cmd.setStory(story);
            cmd.setStoryContainer(container);
            getCommandHandler().execute(cmd);
        });
        assertEquals(1, joinRows("story_storycontainers", "story_id", "storycontainer",
                story.getId(), container.getId()), "the story is linked to the use case");

        withoutLoadingActiveProjects("RemoveStoryFromStoryContainer", () -> {
            RemoveStoryFromStoryContainerCommand cmd = getProjectCommandFactory()
                    .newRemoveStoryFromStoryContainerCommand();
            cmd.setEditedBy(admin);
            cmd.setStory(story);
            cmd.setStoryContainer(container);
            getCommandHandler().execute(cmd);
        });
        assertEquals(0, joinRows("story_storycontainers", "story_id", "storycontainer",
                story.getId(), container.getId()), "the story is unlinked from the use case");
    }

    @Test
    void actorAddAndRemoveNeverLoadActiveProjects() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "rg-actor-" + ts);
        UseCase container = createUseCase(admin, project, "rg-actor-uc-" + ts,
                "rg-actor-uc-actor-" + ts, "rg actor step " + ts);
        Actor actor = createActor(admin, project, "rg-actor-" + ts);

        withoutLoadingActiveProjects("AddActorToActorContainer", () -> {
            AddActorToActorContainerCommand cmd = getProjectCommandFactory()
                    .newAddActorToActorContainerCommand();
            cmd.setEditedBy(admin);
            cmd.setActor(actor);
            cmd.setActorContainer(container);
            getCommandHandler().execute(cmd);
        });
        assertEquals(1, joinRows("actor_actorcontainers", "actor_id", "actorcontainer",
                actor.getId(), container.getId()), "the actor is linked to the use case");

        withoutLoadingActiveProjects("RemoveActorFromActorContainer", () -> {
            RemoveActorFromActorContainerCommand cmd = getProjectCommandFactory()
                    .newRemoveActorFromActorContainerCommand();
            cmd.setEditedBy(admin);
            cmd.setActor(actor);
            cmd.setActorContainer(container);
            getCommandHandler().execute(cmd);
        });
        assertEquals(0, joinRows("actor_actorcontainers", "actor_id", "actorcontainer",
                actor.getId(), container.getId()), "the actor is unlinked from the use case");
    }

    @Test
    void goalAddAndRemoveNeverLoadActiveProjects() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "rg-goal-" + ts);
        UseCase container = createUseCase(admin, project, "rg-goal-uc-" + ts,
                "rg-goal-uc-actor-" + ts, "rg goal step " + ts);
        Goal goal = createGoal(admin, project, "rg-goal-" + ts);

        withoutLoadingActiveProjects("AddGoalToGoalContainer", () -> {
            AddGoalToGoalContainerCommand cmd = getProjectCommandFactory()
                    .newAddGoalToGoalContainerCommand();
            cmd.setEditedBy(admin);
            cmd.setGoal(goal);
            cmd.setGoalContainer(container);
            getCommandHandler().execute(cmd);
        });
        assertEquals(1, joinRows("goals_goalcontainers", "goal_id", "goalcontainer",
                goal.getId(), container.getId()), "the goal is linked to the use case");

        withoutLoadingActiveProjects("RemoveGoalFromGoalContainer", () -> {
            RemoveGoalFromGoalContainerCommand cmd = getProjectCommandFactory()
                    .newRemoveGoalFromGoalContainerCommand();
            cmd.setEditedBy(admin);
            cmd.setGoal(goal);
            cmd.setGoalContainer(container);
            getCommandHandler().execute(cmd);
        });
        assertEquals(0, joinRows("goals_goalcontainers", "goal_id", "goalcontainer",
                goal.getId(), container.getId()), "the goal is unlinked from the use case");
    }

    @Test
    void deleteStoryWithPrimaryActorNeverLoadsActiveProjects() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "rg-delstory-" + ts);
        // The actor has to exist: before #325 an unknown name silently left the story with no
        // primary actor, so this test never had the actor its name promises.
        Actor actor = createActor(admin, project, "rg-delstory-actor-" + ts);
        Story story = createStory(admin, project, "rg-delstory-" + ts, actor.getName());
        assertNotNull(story.getPrimaryActor(), "fixture: the story has a primary actor");

        withoutLoadingActiveProjects("DeleteStory", () -> {
            DeleteStoryCommand cmd = getProjectCommandFactory().newDeleteStoryCommand();
            cmd.setEditedBy(admin);
            cmd.setStory(story);
            getCommandHandler().execute(cmd);
        });
        assertThrows(NoSuchEntityException.class,
                () -> getProjectRepository().findById(Story.class, story.getId()));
    }

    @Test
    void removeAnnotationFromAnnotatableNeverLoadsActiveProjects() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        Project project = createProject(admin, "rg-note-" + ts);
        Goal goal = createGoal(admin, project, "rg-note-goal-" + ts);
        Note note = addNote(admin, project, goal, "rg-note " + ts);

        withoutLoadingActiveProjects("RemoveAnnotationFromAnnotatable", () -> {
            RemoveAnnotationFromAnnotatableCommand cmd = getAnnotationCommandFactory()
                    .newRemoveAnnotationFromAnnotatableCommand();
            cmd.setEditedBy(admin);
            cmd.setAnnotatable(goal);
            cmd.setAnnotation(note);
            getCommandHandler().execute(cmd);
        });
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM annotation_annotatable WHERE annotation_id = ? "
                        + "AND annotatable_type = 'Goal' AND annotatable_id = ?",
                Integer.class, note.getId(), goal.getId()),
                "the note is unlinked from the goal");
    }

    // -------------------------------------------------------------------------
    // activeProjects is LAZY: the readers and writers still work
    // -------------------------------------------------------------------------

    @Test
    void findActiveProjectsListsAProjectUsersProjectsInOrder() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        long ts = System.currentTimeMillis();
        String username = "rg-member-" + ts;
        createUser(username);
        // Created in reverse name order so the sort is actually exercised.
        Project second = createProject(admin, "rg-member-b-" + ts);
        Project first = createProject(admin, "rg-member-a-" + ts);

        // EditUserStakeholder adds to the (now lazy) activeProjects on a managed user.
        addUserStakeholder(second, username);
        addUserStakeholder(first, username);

        User member = getUserRepository().findUserByUsername(username);
        List<Project> projects = new ArrayList<>(getProjectRepository().findActiveProjects(member));
        assertEquals(2, projects.size(), "the member is on exactly the two projects");
        assertEquals(first.getId(), projects.get(0).getId(), "natural (name) order");
        assertEquals(second.getId(), projects.get(1).getId(), "natural (name) order");
    }

    @Test
    void editProjectWithADetachedCreatorRecordsTheActiveProject() throws Exception {
        // findUserByUsername runs in its own transaction, so admin is detached here - the case a
        // lazy activeProjects would throw on if EditProject touched it without re-reading the user.
        User admin = getUserRepository().findUserByUsername("admin");
        Project project = createProject(admin, "rg-detached-" + System.currentTimeMillis());

        Set<Project> active = getProjectRepository().findActiveProjects(admin);
        assertTrue(active.stream().anyMatch(p -> p.getId().equals(project.getId())),
                "the creator's active projects include the new project");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface CommandRun {
        void run() throws Exception;
    }

    private void withoutLoadingActiveProjects(String command, CommandRun run) throws Exception {
        long before = activeProjectsLoads();
        run.run();
        assertEquals(before, activeProjectsLoads(),
                command + " must not load any user's activeProjects");
    }

    private long activeProjectsLoads() {
        return statistics.getCollectionStatistics(ACTIVE_PROJECTS_ROLE).getLoadCount();
    }

    /**
     * Rows linking {@code entityId} to {@code containerId} in a {@code @ManyToAny} join table.
     * Container ids collide across tables, so the use-case discriminator is part of the match.
     */
    private int joinRows(String table, String entityColumn, String containerPrefix,
            Long entityId, Long containerId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE "
                + entityColumn + " = ? AND " + containerPrefix + "_id = ? AND "
                + containerPrefix + "_type = ?", Integer.class, entityId, containerId,
                UseCase.class.getName());
    }

    private Project createProject(User owner, String name) throws Exception {
        EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
        cmd.setEditedBy(owner);
        cmd.setName(name);
        cmd.setText("refresh-graph integration test");
        cmd.setOrganizationName("RefreshGraphOrg-" + name);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
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

    private Actor createActor(User actor, Project project, String name) throws Exception {
        EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
        cmd.setEditedBy(actor);
        cmd.setActorContainer(project);
        cmd.setName(name);
        cmd.setText("actor");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getActor();
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

    private UseCase createUseCase(User actor, Project project, String name,
            String primaryActorName, String stepName) throws Exception {
        EditScenarioStepCommand stepCmd = getProjectCommandFactory().newEditScenarioStepCommand();
        stepCmd.setEditedBy(actor);
        stepCmd.setProjectOrDomain(project);
        stepCmd.setName(stepName);
        stepCmd.setText("Text for " + stepName);
        stepCmd.setScenarioTypeName(ScenarioType.Primary.name());
        List<EditScenarioStepCommand> stepCommands = new ArrayList<>();
        stepCommands.add(stepCmd);

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

    private Note addNote(User actor, Project project, Goal goal, String text) throws Exception {
        EditNoteCommand cmd = getAnnotationCommandFactory().newEditNoteCommand();
        cmd.setEditedBy(actor);
        cmd.setGroupingObject(project);
        cmd.setAnnotatable(goal);
        cmd.setText(text);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getNote();
    }

    private void createUser(String username) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(admin);
        cmd.setUsername(username);
        cmd.setPassword("pw");
        cmd.setRepassword("pw");
        cmd.setName(username);
        cmd.setEmailAddress(username + "@example.com");
        cmd.setPhoneNumber("");
        cmd.setOrganizationName("RefreshGraphOrg");
        cmd.addUserRoleName("ProjectUserRole");
        getCommandHandler().execute(cmd);
    }

    private void addUserStakeholder(Project project, String username) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUserStakeholderCommand cmd = getProjectCommandFactory().newEditUserStakeholderCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setUsername(username);
        cmd.setStakeholderPermissions(Set.of());
        getCommandHandler().execute(cmd);
    }
}
