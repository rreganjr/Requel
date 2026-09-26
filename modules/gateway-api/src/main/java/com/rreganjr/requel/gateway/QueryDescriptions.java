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
package com.rreganjr.requel.gateway;

import java.util.List;

/**
 * Caller-facing descriptions of the read surface (issue #296): the MCP read tools and the
 * {@code requel-cli} read commands say the same thing because both take their text from here.
 *
 * <p>The strings are compile-time constants so the CLI can use them in picocli annotations. Keep
 * {@code %} out of them: picocli formats description text. Write for someone reading without the
 * source: what comes back, in what order, and what is left out. Tool names are avoided where the
 * CLI shows the same text; the MCP-only {@link #DRAFT_ANNOTATION} names the write tools.
 *
 * @author ron
 */
public final class QueryDescriptions {

    /** The {@code entityType} values {@code getEntity} and {@code getEntityNeighbors} accept. */
    public static final List<String> READABLE_ENTITY_TYPES = List.of(
            "Goal", "Story", "Actor", "UseCase", "Scenario", "GlossaryTerm");

    /**
     * The {@code entityType} values notes and issues attach to: {@code getAnnotations},
     * {@code draftAnnotation}, and the {@code EditNote}/{@code EditIssue} commands. They are the
     * keys {@code ProjectAnnotatableRegistryConfiguration} registers.
     */
    public static final List<String> ANNOTATABLE_ENTITY_TYPES = List.of(
            "Project", "ProjectTeam", "Goal", "GoalRelation", "UseCase", "Scenario", "Step",
            "Story", "Actor", "GlossaryTerm", "NonUserStakeholder", "UserStakeholder");

    // ---- tools / commands ----------------------------------------------------------------------

    public static final String LIST_PROJECTS = "Lists the projects you can read, sorted by name:"
            + " every project for a system administrator, otherwise the projects you are a"
            + " stakeholder on. Each entry has the project's description, status, organization and"
            + " a count of each kind of entity. Use an entry's name as the project name elsewhere.";

    public static final String GET_PROJECT = "Reads one project's summary: its description,"
            + " status, organization, creator and a count of each kind of entity. A project that"
            + " does not exist is not found, and one you are not a stakeholder on is refused unless you"
            + " are a system administrator.";

    public static final String GET_PROJECT_TREE = "Reads a project's contents as a tree of fixed"
            + " groups (Stakeholders, Goals, Stories, Actors, Use Cases, Glossary, Reports), each"
            + " listing its members by id and name, sorted by name. A member's type is its group"
            + " label, not an entity type. Scenarios and steps are not in the tree; read a use case"
            + " or scenario to see them.";

    public static final String GET_GLOSSARY = "Lists a project's glossary terms, sorted by name,"
            + " with each term's definition and, for an alternate term, its canonical term. Read a"
            + " single term to see its alternate terms and the entities that refer to it.";

    public static final String GET_OPEN_ISSUES = "Lists the unresolved issues on the entities of a"
            + " project, highest severity first (HIGH, MEDIUM, LOW). An issue is unresolved until a"
            + " position is chosen to resolve it, whether or not it must be resolved, and issues"
            + " the assistant raised, such as spelling, are included. Each entry names the entity"
            + " the issue is attached to. Issues on the project itself and on goal relations are"
            + " not listed.";

    public static final String GET_ANNOTATIONS = "Reads the notes and issues attached to one"
            + " entity. Notes come in the order they were created. Issues, resolved ones included,"
            + " come highest severity first and carry their positions and each position's"
            + " arguments.";

    public static final String GET_ENTITY = "Reads one entity's full detail by type and id,"
            + " including its relationships: a goal's relations and their types, a story's or use"
            + " case's primary actor, actors, goals and stories, a use case's scenarios, a"
            + " scenario's steps, and a glossary term's alternate terms and referring entities."
            + " Steps, stakeholders and reports cannot be read this way.";

    public static final String GET_ENTITY_NEIGHBORS = "Lists the entities related to one entity as"
            + " type, id and name references, grouped by relationship: a goal's relations to and"
            + " from other goals and what refers to it; a story's goals and actors; an actor's"
            + " goals, use cases and stories; a use case's goals, actors, stories and scenarios"
            + " (primary first); a scenario's steps; a glossary term's canonical term, alternate"
            + " terms and referring entities. Goal relation types are left out; read the goal to"
            + " see them.";

    public static final String SEARCH_PROJECT_ENTITIES = "Finds the goals, stories, actors, use"
            + " cases, scenarios and glossary terms in a project whose name contains the query,"
            + " ignoring case. Returns type, id and name references, grouped by type and sorted by"
            + " name within each, with no limit on the number of results; an empty query matches"
            + " everything.";

    public static final String GET_PROJECT_CONTEXT = "Reads a whole project in one call: its"
            + " summary, content tree, glossary and open issues, the same data as reading each of"
            + " them separately.";

    public static final String DRAFT_ANNOTATION = "Builds a note or issue for an entity and returns"
            + " it as a draft. Nothing is saved, and neither the entity nor the project is checked."
            + " To save it, call EditNote or EditIssue (write tools, offered when the server allows"
            + " writes) with projectName and the draft's targetRef.entityType, targetRef.entityId,"
            + " text and severity, and for an issue its metadata.mustResolve as mustBeResolved.";

    // ---- properties / parameters ---------------------------------------------------------------

    public static final String PROJECT_NAME = "The project's name, exactly as it is listed.";

    public static final String READABLE_ENTITY_TYPE = "The entity's type, case-sensitive: Goal,"
            + " Story, Actor, UseCase, Scenario or GlossaryTerm.";

    public static final String ANNOTATABLE_ENTITY_TYPE = "The entity's type, case-sensitive:"
            + " Project, ProjectTeam, Goal, GoalRelation, UseCase, Scenario, Step, Story, Actor,"
            + " GlossaryTerm, NonUserStakeholder or UserStakeholder.";

    public static final String ENTITY_ID = "The entity's numeric id, as the project tree, a search"
            + " or another read returns it.";

    public static final String SEARCH_QUERY = "Text to find within entity names, ignoring case.";

    public static final String ANNOTATION_KIND = "NOTE for a remark, ISSUE for a problem that can"
            + " be discussed through positions and resolved.";

    public static final String ANNOTATION_TEXT = "The note's or issue's text.";

    public static final String ANNOTATION_SEVERITY = "Issues only: LOW, MEDIUM or HIGH, ignoring"
            + " case. Left out, a saved issue gets MEDIUM.";

    public static final String ANNOTATION_MUST_RESOLVE = "Issues only: whether the issue must be"
            + " resolved. Defaults to false, as it does when EditIssue creates an issue.";

    private QueryDescriptions() {
    }
}
