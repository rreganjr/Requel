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

    /**
     * #270: what {@code stale} and {@code source} on a note or issue mean, shared by the reads
     * that return them.
     */
    public static final String STALE_ANNOTATIONS = " Each note and issue carries its source"
            + " (ASSISTANT:<id> when an assistant raised it, otherwise empty) and a stale flag:"
            + " stale is true when an assistant raised it against text that has since changed, or"
            + " a later analysis no longer reports it, so it may no longer apply; weigh or skip"
            + " stale entries.";

    public static final String GET_OPEN_ISSUES = "Lists the unresolved issues on the entities of a"
            + " project, highest severity first (HIGH, MEDIUM, LOW). An issue is unresolved until a"
            + " position is chosen to resolve it, whether or not it must be resolved, and issues"
            + " the assistant raised, such as spelling, are included. Each entry names the entity"
            + " the issue is attached to. Issues on the project itself and on goal relations are"
            + " not listed. Within a severity, stale issues come after the others."
            + STALE_ANNOTATIONS;

    public static final String GET_ANNOTATIONS = "Reads the notes and issues attached to one"
            + " entity. Notes come in the order they were created, stale ones last. Issues, resolved ones included,"
            + " come highest severity first, stale ones after the others within a severity, and"
            + " carry their positions and each position's arguments." + STALE_ANNOTATIONS;

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

    public static final String GET_PROJECT_CONTEXT = "Reads a project's summary, content tree"
            + " (names only), glossary and open issues in one call, the same data as reading each"
            + " of them separately. For the text of every entity, scenarios with their steps, and"
            + " all annotations, read the project's content instead." + STALE_ANNOTATIONS;

    /** #274: the project content read. */
    public static final String GET_PROJECT_CONTENT = "Reads a project's whole content in one call:"
            + " the summary with its entity counts, then every stakeholder, goal, story, actor,"
            + " use case, scenario, step and glossary term with its name, text, type, tags and"
            + " annotations, each list sorted by id. Entities refer to one another by id (a goal's"
            + " relations, a story's goals and actors, a use case's primary actor, scenarios,"
            + " goals, actors and stories, a glossary term's canonical term). A scenario lists its"
            + " steps in order as Step or Scenario references; a step shared by several scenarios"
            + " has the same id in each and is listed once under steps. A user stakeholder's"
            + " account details are left out. Sources and references are not included; list"
            + " them separately. A project over the configured character cap is refused with a"
            + " message giving each section's size rather than cut short; the result also reports"
            + " the characters it counted and the cap. A large result may still be truncated by"
            + " the client, so on a large project read with annotations open or none."
            + STALE_ANNOTATIONS;

    /** #274: the {@code annotations} parameter of the project content read. */
    public static final String CONTENT_ANNOTATIONS = "Which annotations to include, ignoring case:"
            + " all (the default) for every note and issue with its positions and arguments, open"
            + " for notes and unresolved issues only, none for no annotations.";

    /** #274: the values {@link #CONTENT_ANNOTATIONS} accepts. */
    public static final List<String> CONTENT_ANNOTATION_MODES = List.of("none", "open", "all");

    public static final String DRAFT_ANNOTATION = "Builds a note or issue for an entity and returns"
            + " it as a draft. Nothing is saved, and neither the entity nor the project is checked."
            + " To save it, call EditNote or EditIssue (write tools, offered when the server allows"
            + " writes) with projectName and the draft's targetRef.entityType, targetRef.entityId,"
            + " text and severity, and for an issue its metadata.mustResolve as mustBeResolved.";

    // ---- provenance (#272): the only reads that return a source or its locator ----------------

    /** The entity types a source link can be recorded on. */
    public static final List<String> PROVENANCE_ENTITY_TYPES = List.of(
            "Goal", "Story", "Actor", "UseCase", "Scenario", "Step", "GlossaryTerm",
            "NonUserStakeholder");

    public static final String GET_SOURCE = "Reads one external source recorded in a project — a"
            + " ticket, a guide, a review the project's entities were built from — by system and"
            + " externalId (exact, case-sensitive). Returns null when the project has none. Its"
            + " contentHash is the version last recorded: compare it with the hash of the source as"
            + " it is now to tell whether the source changed, without reading any entity.";

    public static final String FIND_ENTITIES_BY_SOURCE = "Lists the entities built from an external"
            + " source, each with the fragment it came from (an acceptance criterion, a page),"
            + " notInLatestSource (the fragment was not part of the latest ingest — it may have been"
            + " removed upstream) and editedSinceIngest (it changed in Requel since). Pass fragment"
            + " to narrow to one fragment. Returns null when the project has no such source.";

    public static final String GET_ENTITY_SOURCES = "Lists the external sources one entity was built"
            + " from, and which fragment of each — the answer to \"says who?\" for a requirement —"
            + " and the documents it cites (relation CITES).";

    // ---- references and precedence (#273) -----------------------------------------------------

    public static final String LIST_SOURCES = "Lists every source and reference recorded in a"
            + " project — tickets, guides, reviews, runbooks, matrices — whether or not an entity"
            + " was built from it, each with its kind, note, how many entities were derived from"
            + " it and cite it, and which sources it defers to and outranks directly. authority"
            + " lists every defers-to edge: where the two disagree the superior wins, and both"
            + " remain current.";

    public static final String COMPARE_SOURCES = "Says which of two recorded sources wins where they"
            + " disagree: A (the first), B (the second) or NONE when no chain of defers-to edges"
            + " joins them. Precedence is transitive; chain lists the sources from the loser up to"
            + " the winner.";

    public static final String OTHER_SOURCE_SYSTEM = "The second source's system, ignoring case.";

    public static final String OTHER_SOURCE_EXTERNAL_ID = "The second source's external id, exactly"
            + " as recorded.";

    public static final String SOURCE_SYSTEM = "The source family, e.g. jira, github or doc,"
            + " ignoring case.";

    public static final String SOURCE_EXTERNAL_ID = "The source's own identifier, e.g. CON-3685,"
            + " exactly as recorded.";

    public static final String SOURCE_FRAGMENT = "Optional: one fragment of the source, e.g. AC-4,"
            + " exactly as recorded.";

    public static final String PROVENANCE_ENTITY_TYPE = "The entity's type, case-sensitive: Goal,"
            + " Story, Actor, UseCase, Scenario, Step, GlossaryTerm or NonUserStakeholder.";

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
