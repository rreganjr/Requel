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
package com.rreganjr.requel.service.gateway;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.ActorDto;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GlossaryTermDto;
import com.rreganjr.requel.service.api.dto.GoalDto;
import com.rreganjr.requel.service.api.dto.GoalRelationDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;
import com.rreganjr.requel.service.api.dto.ScenarioDto;
import com.rreganjr.requel.service.api.dto.StepDto;
import com.rreganjr.requel.service.api.dto.StoryDto;
import com.rreganjr.requel.service.api.dto.UseCaseDto;
import com.rreganjr.requel.service.query.AnnotationQueryController;
import com.rreganjr.requel.service.query.ProjectQueryController;

/**
 * In-process {@link QueryGateway}: the authorized, DTO-shaped read side of the gateway, backed by
 * the same query controllers the REST API uses. This is the canonical home for the read contract
 * the MCP server (and a future REST-backed client) consume — the mirror of
 * {@code InProcessCommandGateway} on the write side. Relocated from the mcp-server module so reads
 * and writes share one gateway-api contract (issue #69 Slice 4).
 */
@Component
public class InProcessQueryGateway implements QueryGateway {

	private final ProjectQueryController projectQueryController;
	private final AnnotationQueryController annotationQueryController;

	public InProcessQueryGateway(ProjectQueryController projectQueryController,
			AnnotationQueryController annotationQueryController) {
		this.projectQueryController = projectQueryController;
		this.annotationQueryController = annotationQueryController;
	}

	@Override
	public List<ProjectDto> listProjects() {
		return projectQueryController.listProjects();
	}

	@Override
	public ProjectDto getProject(String projectName) {
		return unwrap(projectQueryController.getProject(projectName), ProjectDto.class);
	}

	@Override
	public List<ProjectTreeNodeDto> getProjectTree(String projectName) {
		ResponseEntity<List<ProjectTreeNodeDto>> response = projectQueryController.getProjectTree(
				projectName);
		return unwrap(response);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<GlossaryTermDto> getGlossaryTerms(String projectName) {
		return (List<GlossaryTermDto>) unwrap(projectQueryController.listTerms(projectName));
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<OpenIssueDto> getOpenIssues(String projectName) {
		return (List<OpenIssueDto>) unwrap(projectQueryController.getOpenIssues(projectName));
	}

	@Override
	public AnnotationsDto getAnnotations(String projectName, String entityType, long entityId) {
		return unwrap(annotationQueryController.getAnnotations(projectName, entityType, entityId));
	}

	@Override
	public Object getEntity(String projectName, String entityType, long entityId) {
		ResponseEntity<?> response = switch (entityType) {
			case "Goal" -> projectQueryController.getGoal(projectName, entityId);
			case "Story" -> projectQueryController.getStory(projectName, entityId);
			case "Actor" -> projectQueryController.getActor(projectName, entityId);
			case "UseCase" -> projectQueryController.getUseCase(projectName, entityId);
			case "Scenario" -> projectQueryController.getScenario(projectName, entityId);
			case "GlossaryTerm" -> projectQueryController.getTerm(projectName, entityId);
			default -> throw new IllegalArgumentException("Unsupported entity type: " + entityType);
		};
		return unwrap(response);
	}

	@Override
	public Map<String, List<EntityReferenceDto>> getEntityNeighbors(String projectName,
			String entityType, long entityId) {
		Object entity = getEntity(projectName, entityType, entityId);
		Map<String, List<EntityReferenceDto>> neighbors = new LinkedHashMap<>();
		if (entity instanceof GoalDto goal) {
			neighbors.put("relationsFromThisGoal", goalRelationRefs(goal.relationsFromThisGoal()));
			neighbors.put("relationsToThisGoal", goalRelationRefs(goal.relationsToThisGoal()));
			neighbors.put("referencedBy", nullToEmpty(goal.referencedBy()));
		} else if (entity instanceof StoryDto story) {
			neighbors.put("goals", nullToEmpty(story.goals()));
			neighbors.put("actors", nullToEmpty(story.actors()));
		} else if (entity instanceof ActorDto actor) {
			neighbors.put("goals", nullToEmpty(actor.goals()));
			neighbors.put("referencedByUseCases", nullToEmpty(actor.referencedByUseCases()));
			neighbors.put("referencedByStories", nullToEmpty(actor.referencedByStories()));
		} else if (entity instanceof UseCaseDto useCase) {
			neighbors.put("goals", refs("Goal", useCase.goals(), GoalDto::id, GoalDto::name));
			neighbors.put("actors", refs("Actor", useCase.actors(), ActorDto::id, ActorDto::name));
			neighbors.put("stories", refs("Story", useCase.stories(), StoryDto::id, StoryDto::name));
			List<EntityReferenceDto> scenarios = new ArrayList<>();
			if (useCase.scenarioId() != null) {
				scenarios.add(new EntityReferenceDto("Scenario", useCase.scenarioId(),
						useCase.scenarioName()));
			}
			scenarios.addAll(refs("Scenario", useCase.additionalScenarios(), ScenarioDto::id,
					ScenarioDto::name));
			neighbors.put("scenarios", scenarios);
		} else if (entity instanceof ScenarioDto scenario) {
			neighbors.put("steps", refs("Step", scenario.steps(), StepDto::id, StepDto::name));
		} else if (entity instanceof GlossaryTermDto term) {
			if (term.canonicalTermId() != null) {
				neighbors.put("canonicalTerm", List.of(new EntityReferenceDto("GlossaryTerm",
						term.canonicalTermId(), term.canonicalTermName())));
			}
			neighbors.put("alternateTerms", nullToEmpty(term.alternateTerms()));
			neighbors.put("referers", nullToEmpty(term.referers()));
		}
		return neighbors;
	}

	@Override
	public List<EntityReferenceDto> searchProjectEntities(String projectName, String query) {
		String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
		List<EntityReferenceDto> matches = new ArrayList<>();
		addMatches(matches, "Goal", projectQueryController.listGoals(projectName),
				GoalDto::id, GoalDto::name, needle);
		addMatches(matches, "Story", projectQueryController.listStories(projectName),
				StoryDto::id, StoryDto::name, needle);
		addMatches(matches, "Actor", projectQueryController.listActors(projectName),
				ActorDto::id, ActorDto::name, needle);
		addMatches(matches, "UseCase", projectQueryController.listUseCases(projectName),
				UseCaseDto::id, UseCaseDto::name, needle);
		addMatches(matches, "Scenario", projectQueryController.listScenarios(projectName),
				ScenarioDto::id, ScenarioDto::name, needle);
		addMatches(matches, "GlossaryTerm", projectQueryController.listTerms(projectName),
				GlossaryTermDto::id, GlossaryTermDto::name, needle);
		return matches;
	}

	@Override
	public Map<String, Object> getProjectContext(String projectName) {
		Map<String, Object> context = new LinkedHashMap<>();
		context.put("project", getProject(projectName));
		context.put("tree", getProjectTree(projectName));
		context.put("glossary", getGlossaryTerms(projectName));
		context.put("openIssues", getOpenIssues(projectName));
		return context;
	}

	@SuppressWarnings("unchecked")
	private <T> void addMatches(List<EntityReferenceDto> out, String entityType,
			ResponseEntity<?> response, Function<T, Long> idFn, Function<T, String> nameFn,
			String needle) {
		List<T> items = (List<T>) unwrap(response);
		for (T item : items) {
			String name = nameFn.apply(item);
			if (name != null && name.toLowerCase(Locale.ROOT).contains(needle)) {
				out.add(new EntityReferenceDto(entityType, idFn.apply(item), name));
			}
		}
	}

	private static List<EntityReferenceDto> nullToEmpty(List<EntityReferenceDto> refs) {
		return refs == null ? List.of() : refs;
	}

	private static List<EntityReferenceDto> goalRelationRefs(List<GoalRelationDto> relations) {
		if (relations == null) {
			return List.of();
		}
		List<EntityReferenceDto> refs = new ArrayList<>(relations.size());
		for (GoalRelationDto relation : relations) {
			refs.add(new EntityReferenceDto("Goal", relation.goalId(), relation.goalName()));
		}
		return refs;
	}

	private static <T> List<EntityReferenceDto> refs(String entityType, List<T> items,
			Function<T, Long> idFn, Function<T, String> nameFn) {
		if (items == null) {
			return List.of();
		}
		List<EntityReferenceDto> refs = new ArrayList<>(items.size());
		for (T item : items) {
			refs.add(new EntityReferenceDto(entityType, idFn.apply(item), nameFn.apply(item)));
		}
		return refs;
	}

	private <T> T unwrap(ResponseEntity<?> response, Class<T> bodyType) {
		Object body = unwrap(response);
		if (!bodyType.isInstance(body)) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Unexpected query response body: " + body);
		}
		return bodyType.cast(body);
	}

	private <T> T unwrap(ResponseEntity<T> response) {
		if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
			return response.getBody();
		}
		throw new ResponseStatusException(HttpStatus.valueOf(response.getStatusCode().value()));
	}
}
