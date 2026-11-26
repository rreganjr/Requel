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
package com.rreganjr.requel.service.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.gateway.QueryDescriptions;
import com.rreganjr.requel.annotation.impl.IssueImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.StepNames;
import com.rreganjr.requel.project.TextEntity;
import com.rreganjr.requel.service.api.dto.ProjectSearchPageDto;
import com.rreganjr.requel.service.api.dto.ProjectSearchResultDto;

import jakarta.persistence.EntityManager;

/**
 * #504: searches one project's requirements and issues for a piece of text.
 * <p>
 * The name and text of goals, stories, actors, use cases, scenarios, steps, glossary terms and
 * stakeholders, and the text of the project's issues (open and resolved), are matched with SQL
 * {@code LIKE}, ignoring case, the search text taken literally ({@code %} and {@code _} escaped).
 * Positions, arguments and notes are not searched. Name matches come before text matches, then
 * by entity type in {@link #ENTITY_TYPES} order, then by creation (id); that order is stable, so
 * {@code offset} and {@code limit} page through it. Steps and scenarios match on their stored name:
 * a step with an actor role (#468) stores only its action, though its result reads with the actor.
 */
@Service
@Transactional(readOnly = true)
public class ProjectSearchService {

	/** The entity types searched, in result order. */
	public static final List<String> ENTITY_TYPES = QueryDescriptions.SEARCHABLE_ENTITY_TYPES;
	public static final int MIN_QUERY_LENGTH = 2;
	public static final int DEFAULT_LIMIT = 200;
	public static final int MAX_LIMIT = 1000;
	/** About how much of the matched field a snippet shows. */
	static final int SNIPPET_LENGTH = 160;
	private static final int SNIPPET_BEFORE = 60;
	private static final int ISSUE_NAME_LENGTH = 80;
	private static final String ELLIPSIS = "…";

	/** JPQL entity per searched requirement type, and an extra condition for its subtype. */
	private static final Map<String, String[]> ENTITIES = Map.of(
			"Goal", new String[] { "GoalImpl", "" },
			"Story", new String[] { "StoryImpl", "" },
			"Actor", new String[] { "ActorImpl", "" },
			"UseCase", new String[] { "UseCaseImpl", "" },
			"Scenario", new String[] { "ScenarioImpl", "" },
			"Step", new String[] { "StepImpl", " and type(e) = StepImpl" },
			"GlossaryTerm", new String[] { "GlossaryTermImpl", "" });

	private final EntityManager entityManager;

	public ProjectSearchService(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * One page of the matches.
	 *
	 * @param entityType one of {@link #ENTITY_TYPES} (any case), or null/blank for all of them
	 * @param offset     the first result to return; null for 0
	 * @param limit      how many to return, 1 to {@link #MAX_LIMIT}; null for {@link #DEFAULT_LIMIT}
	 * @throws IllegalArgumentException for a search under {@link #MIN_QUERY_LENGTH} characters, an
	 *         unknown type, or an offset or limit out of range, with a message saying which
	 */
	public ProjectSearchPageDto search(Project project, String query, String entityType,
			Integer offset, Integer limit) {
		String needle = validQuery(query);
		String type = validType(entityType);
		int from = offset == null ? 0 : offset;
		int size = limit == null ? DEFAULT_LIMIT : limit;
		if (from < 0) {
			throw new IllegalArgumentException("offset must be 0 or more, not " + from + ".");
		}
		if (size < 1 || size > MAX_LIMIT) {
			throw new IllegalArgumentException(
					"limit must be from 1 to " + MAX_LIMIT + ", not " + size + ".");
		}
		List<Match> matches = new ArrayList<>();
		String pattern = likePattern(needle);
		for (String candidate : ENTITY_TYPES) {
			if (type != null && !type.equals(candidate)) {
				continue;
			}
			switch (candidate) {
			case "Stakeholder" -> matchStakeholders(project, needle, matches);
			case "Issue" -> matchIssues(project, pattern, needle, matches);
			default -> matchRequirements(project, candidate, pattern, needle, matches);
			}
		}
		matches.sort(Comparator.comparingInt((Match m) -> m.nameMatch() ? 0 : 1)
				.thenComparingInt(m -> ENTITY_TYPES.indexOf(m.result().entityType()))
				.thenComparingLong(m -> m.result().id()));
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (Match match : matches) {
			counts.merge(match.result().entityType(), 1, Integer::sum);
		}
		List<ProjectSearchResultDto> page = matches.stream().skip(from).limit(size)
				.map(Match::result).toList();
		return new ProjectSearchPageDto(page, matches.size(), counts, from, size);
	}

	private static String validQuery(String query) {
		String trimmed = query == null ? "" : query.strip();
		if (trimmed.length() < MIN_QUERY_LENGTH) {
			throw new IllegalArgumentException("Search for at least " + MIN_QUERY_LENGTH
					+ " characters.");
		}
		return trimmed;
	}

	private static String validType(String entityType) {
		if (entityType == null || entityType.isBlank()) {
			return null;
		}
		for (String type : ENTITY_TYPES) {
			if (type.equalsIgnoreCase(entityType.strip())) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown entity type '" + entityType
				+ "'; search one of " + String.join(", ", ENTITY_TYPES) + ".");
	}

	/**
	 * The project as the persistence context holds it. The repository can answer a CGLIB proxy,
	 * which Hibernate cannot bind as a query parameter.
	 */
	private ProjectImpl managed(Project project) {
		return entityManager.find(ProjectImpl.class, project.getId());
	}

	/** The search text as a LIKE pattern, lower case, with ! % and _ escaped by '!'. */
	static String likePattern(String needle) {
		String escaped = needle.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%")
				.replace("_", "!_");
		return "%" + escaped + "%";
	}

	@SuppressWarnings("unchecked")
	private void matchRequirements(Project project, String type, String pattern, String needle,
			List<Match> out) {
		String[] entity = ENTITIES.get(type);
		List<Object> found = entityManager.createQuery("select e from " + entity[0]
				+ " e where e.projectOrDomain = :project" + entity[1]
				+ " and (lower(e.name) like :pattern escape '!'"
				+ " or lower(cast(e.text as String)) like :pattern escape '!')")
				.setParameter("project", managed(project)).setParameter("pattern", pattern)
				.getResultList();
		for (Object row : found) {
			ProjectOrDomainEntity e = (ProjectOrDomainEntity) row;
			String text = e instanceof TextEntity te ? te.getText() : null;
			String name = e instanceof Step step ? StepNames.display(step) : e.getName();
			out.add(match(type, e.getId(), name, e.getName(), text, needle, null, null));
		}
	}

	/** Stakeholders are few, and a user stakeholder's name is its user's: matched in memory. */
	private static void matchStakeholders(Project project, String needle, List<Match> out) {
		for (Stakeholder stakeholder : project.getStakeholders()) {
			String name = stakeholder.getDisplayName();
			String text = stakeholder instanceof NonUserStakeholder other ? other.getText() : null;
			if (indexOfIgnoreCase(name, needle) >= 0 || indexOfIgnoreCase(text, needle) >= 0) {
				out.add(match("Stakeholder", stakeholder.getId(), name, name, text, needle, null,
						null));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void matchIssues(Project project, String pattern, String needle, List<Match> out) {
		List<IssueImpl> found = entityManager.createQuery("select i from IssueImpl i"
				+ " where i.groupingObject = :project and lower(cast(i.text as String)) like :pattern escape '!'")
				.setParameter("project", managed(project)).setParameter("pattern", pattern)
				.getResultList();
		for (IssueImpl issue : found) {
			ProjectOrDomainEntity on = firstEntityOf(issue);
			String text = issue.getText();
			Match match = match("Issue", issue.getId(), issueName(text), null, text, needle,
					issue.isResolved(), on);
			out.add(match);
		}
	}

	private static ProjectOrDomainEntity firstEntityOf(IssueImpl issue) {
		ProjectOrDomainEntity first = null;
		for (Annotatable annotatable : issue.getAnnotatables()) {
			Object real = org.hibernate.Hibernate.unproxy(annotatable);
			if (real instanceof ProjectOrDomainEntity entity
					&& (first == null || entity.getId() < first.getId())) {
				first = entity;
			}
		}
		return first;
	}

	private static String issueName(String text) {
		String oneLine = text == null ? "" : text.strip().replaceAll("\\s+", " ");
		return oneLine.length() <= ISSUE_NAME_LENGTH ? oneLine
				: oneLine.substring(0, ISSUE_NAME_LENGTH - 1) + ELLIPSIS;
	}

	private static Match match(String type, Long id, String shownName, String name, String text,
			String needle, Boolean resolved, ProjectOrDomainEntity on) {
		boolean nameMatch = indexOfIgnoreCase(name, needle) >= 0;
		String field = nameMatch ? name : text;
		Snippet snippet = snippet(field, needle);
		String onType = on == null ? null
				: on instanceof Project ? "Project"
						: on.getProjectOrDomainEntityInterface().getSimpleName();
		String onName = on == null ? null
				: on instanceof Step step ? StepNames.display(step) : on.getName();
		return new Match(nameMatch, new ProjectSearchResultDto(type, id, shownName,
				nameMatch ? "name" : "text", snippet.text(), snippet.start(), snippet.length(),
				resolved, onType, on == null ? null : on.getId(), onName));
	}

	/**
	 * About {@link #SNIPPET_LENGTH} characters of the field around the first match, whitespace
	 * runs shown as one space, with an ellipsis where it was cut. A match SQL found but Java can't
	 * place (a database collation that ignores accents) gives the start of the field and -1.
	 */
	static Snippet snippet(String field, String needle) {
		String value = field == null ? "" : field.replaceAll("\\s", " ");
		int at = indexOfIgnoreCase(value, needle);
		if (at < 0) {
			String head = value.length() <= SNIPPET_LENGTH ? value
					: value.substring(0, SNIPPET_LENGTH) + ELLIPSIS;
			return new Snippet(head, -1, 0);
		}
		int start = Math.max(0, at - SNIPPET_BEFORE);
		int end = Math.min(value.length(), Math.max(at + needle.length(), start + SNIPPET_LENGTH));
		String prefix = start > 0 ? ELLIPSIS : "";
		String suffix = end < value.length() ? ELLIPSIS : "";
		return new Snippet(prefix + value.substring(start, end) + suffix,
				prefix.length() + at - start, needle.length());
	}

	static int indexOfIgnoreCase(String haystack, String needle) {
		if (haystack == null) {
			return -1;
		}
		return haystack.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT));
	}

	record Snippet(String text, int start, int length) {
	}

	private record Match(boolean nameMatch, ProjectSearchResultDto result) {
	}
}
