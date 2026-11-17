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
package com.rreganjr.requel.ideas;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.rreganjr.requel.assistant.ai.IdeaGenerationService;
import com.rreganjr.requel.assistant.ai.IdeaGenerationService.IdeaView;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Draft;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Kind;
import com.rreganjr.requel.assistant.ai.IdeaRequirementsDrafts.Reference;
import com.rreganjr.requel.assistant.ai.IdeaToRequirementsService.Idea;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.service.api.dto.ActorDto;
import com.rreganjr.requel.service.api.dto.GoalDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.StoryDto;
import com.rreganjr.requel.service.api.dto.UseCaseDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;

/**
 * Turns a product manager's idea into a new project with the requirements the manager chose.
 *
 * <ol>
 * <li>{@link #startNewProject}: creates the project, records the idea with the
 * {@code SubmitIdea} command and queues the AI generation of requirement drafts.</li>
 * <li>The manager reviews the drafts (read through {@link IdeaController}).</li>
 * <li>{@link #accept}: creates the chosen drafts as the project's actors, goals, glossary terms,
 * stories and use cases, and links them as the drafts refer to each other.</li>
 * </ol>
 *
 * Every write goes through the {@link CommandGateway}, so authorization, auditing and
 * validation are the ones the rest of the application uses, as the calling user. The bean exists
 * only when {@code requel.ai.enabled=true}, like {@link IdeaGenerationService}.
 */
@Service
@ConditionalOnProperty(name = "requel.ai.enabled", havingValue = "true")
public class IdeaProjectService {

	/** The order entities are created in: what others refer to comes first. */
	private static final List<Kind> CREATION_ORDER = List.of(Kind.ACTOR, Kind.GOAL,
			Kind.GLOSSARY_TERM, Kind.STORY, Kind.USE_CASE);

	/** What starting a project from an idea produced. */
	public record Started(String projectName, String ideaId) {
	}

	/**
	 * What accepting drafts produced.
	 *
	 * @param created how many entities of each kind were created
	 * @param notes what was left out, e.g. a link to a draft that was not chosen
	 */
	public record Accepted(String projectName, Map<String, Integer> created,
			List<String> notes) {
	}

	private final CommandGateway gateway;
	private final IdeaGenerationService ideas;
	private final CurrentUserResolver currentUserResolver;

	@Autowired
	public IdeaProjectService(CommandGateway gateway, IdeaGenerationService ideas,
			CurrentUserResolver currentUserResolver) {
		this.gateway = gateway;
		this.ideas = ideas;
		this.currentUserResolver = currentUserResolver;
	}

	/**
	 * Create the project {@code projectName}, save the idea in it and queue the generation.
	 *
	 * @param ideaText the typed idea followed by any file's text; see {@link #combine}
	 * @throws IllegalArgumentException when the name or the text is missing or too short
	 * @throws GatewayException when a command is refused, e.g. the project name is taken or the
	 *         user may not create projects
	 */
	public Started startNewProject(String projectName, String ideaText, String fileName,
			boolean fromFile) throws GatewayException {
		if (projectName == null || projectName.isBlank()) {
			throw new IllegalArgumentException("A project name is required.");
		}
		String name = projectName.strip();
		String text = ideaText == null ? "" : ideaText.strip();
		if (text.isEmpty()) {
			throw new IllegalArgumentException("An idea needs some text.");
		}
		if (!fromFile && text.length() < 10) {
			throw new IllegalArgumentException("The idea must be at least 10 characters.");
		}
		User user = currentUserResolver.resolve();

		ProjectDto project = (ProjectDto) gateway.execute(new GatewayRequest("EditProject",
				Map.of("name", name, "description", "Created from an idea."))).result();
		Map<String, Object> submit = new HashMap<>();
		submit.put("projectName", name);
		submit.put("text", text);
		gateway.execute(new GatewayRequest("SubmitIdea", submit));
		String ideaId = ideas.submit(project.id(), new UserRef(user.getId(), user.getUsername()),
				new Idea(text, List.of(), List.of(), List.of()));
		return new Started(name, ideaId);
	}

	/** The typed text, then the file's text after a blank line; either may be empty. */
	public static String combine(String typed, String fromFile) {
		String a = typed == null ? "" : typed.strip();
		String b = fromFile == null ? "" : fromFile.strip();
		if (a.isEmpty()) {
			return b;
		}
		return b.isEmpty() ? a : a + "\n\n" + b;
	}

	/**
	 * Create the drafts at the given positions of the idea's draft list as entities of the
	 * project.
	 *
	 * @param selected positions in {@code idea.drafts()}
	 * @throws IllegalStateException when the idea has no drafts yet
	 * @throws IllegalArgumentException when a position is not a draft or nothing is chosen
	 * @throws GatewayException when a command is refused; entities created before it stay
	 */
	public Accepted accept(String projectName, IdeaView idea, Set<Integer> selected)
			throws GatewayException {
		if (!"DRAFTED".equals(idea.status())) {
			throw new IllegalStateException("The idea has no drafts to accept (it is "
					+ idea.status() + ").");
		}
		if (selected == null || selected.isEmpty()) {
			throw new IllegalArgumentException("Choose at least one requirement.");
		}
		List<Draft> drafts = idea.drafts();
		for (Integer position : selected) {
			if (position == null || position < 0 || position >= drafts.size()) {
				throw new IllegalArgumentException("No draft at position " + position + ".");
			}
		}

		Map<String, Long> ids = new HashMap<>();
		Map<String, Integer> created = new LinkedHashMap<>();
		List<String> notes = new ArrayList<>();
		Map<Kind, List<Draft>> chosen = new EnumMap<>(Kind.class);
		for (int i = 0; i < drafts.size(); i++) {
			if (selected.contains(i)) {
				chosen.computeIfAbsent(drafts.get(i).kind(), k -> new ArrayList<>()).add(drafts.get(i));
			}
		}

		for (Kind kind : CREATION_ORDER) {
			for (Draft draft : chosen.getOrDefault(kind, List.of())) {
				Long id = create(projectName, draft, ids, notes);
				if (id != null) {
					ids.put(key(kind, draft.name()), id);
				}
				created.merge(kind.label(), 1, Integer::sum);
			}
		}
		return new Accepted(projectName, created, notes);
	}

	/** Create one entity, then its links to entities created before it. */
	private Long create(String projectName, Draft draft, Map<String, Long> ids,
			List<String> notes) throws GatewayException {
		Map<String, Object> input = new HashMap<>();
		input.put("projectName", projectName);
		input.put("name", draft.name());
		String primaryActor = primaryActor(draft, ids, notes);
		switch (draft.kind()) {
		case ACTOR: {
			input.put("description", draft.text());
			ActorDto actor = (ActorDto) gateway.execute(new GatewayRequest("EditActor", input))
					.result();
			return actor.id();
		}
		case GOAL: {
			input.put("text", draft.text());
			GoalDto goal = (GoalDto) gateway.execute(new GatewayRequest("EditGoal", input))
					.result();
			return goal.id();
		}
		case GLOSSARY_TERM: {
			input.put("text", draft.text());
			gateway.execute(new GatewayRequest("EditGlossaryTerm", input));
			return null;
		}
		case STORY: {
			input.put("text", draft.text());
			// a story needs a type and the draft has none: Success is the ordinary case
			input.put("storyTypeName", "Success");
			if (primaryActor != null) {
				input.put("primaryActorName", primaryActor);
			}
			StoryDto story = (StoryDto) gateway.execute(new GatewayRequest("EditStory", input))
					.result();
			linkGoals(projectName, "Story", story.id(), draft, ids, notes);
			return story.id();
		}
		case USE_CASE: {
			input.put("text", draft.text());
			if (primaryActor != null) {
				input.put("primaryActorName", primaryActor);
			}
			UseCaseDto useCase = (UseCaseDto) gateway
					.execute(new GatewayRequest("EditUseCase", input)).result();
			linkGoals(projectName, "UseCase", useCase.id(), draft, ids, notes);
			for (Reference ref : draft.references()) {
				if (ref.kind() == Kind.STORY) {
					Long storyId = ids.get(key(Kind.STORY, ref.name()));
					if (storyId == null) {
						notes.add("\"" + draft.name() + "\" refers to the story \"" + ref.name()
								+ "\", which was not created, so it was not linked.");
						continue;
					}
					gateway.execute(new GatewayRequest("AddStoryToStoryContainer",
							Map.of("projectName", projectName, "storyContainerId", useCase.id(),
									"storyId", storyId, "containerType", "UseCase")));
				}
			}
			return useCase.id();
		}
		default:
			return null;
		}
	}

	/** The name of the draft's primary actor if that actor was created, else null (and a note). */
	private static String primaryActor(Draft draft, Map<String, Long> ids, List<String> notes) {
		if (draft.kind() != Kind.STORY && draft.kind() != Kind.USE_CASE) {
			return null;
		}
		for (Reference ref : draft.references()) {
			if (ref.kind() == Kind.ACTOR) {
				if (ids.containsKey(key(Kind.ACTOR, ref.name()))) {
					return ref.name();
				}
				notes.add("\"" + draft.name() + "\" has the primary actor \"" + ref.name()
						+ "\", which was not created, so it has none.");
				return null;
			}
		}
		return null;
	}

	private void linkGoals(String projectName, String containerType, Long containerId,
			Draft draft, Map<String, Long> ids, List<String> notes) throws GatewayException {
		for (Reference ref : draft.references()) {
			if (ref.kind() != Kind.GOAL) {
				continue;
			}
			Long goalId = ids.get(key(Kind.GOAL, ref.name()));
			if (goalId == null) {
				notes.add("\"" + draft.name() + "\" serves the goal \"" + ref.name()
						+ "\", which was not created, so it was not linked.");
				continue;
			}
			gateway.execute(new GatewayRequest("AddGoalToGoalContainer",
					Map.of("projectName", projectName, "goalContainerId", containerId,
							"goalId", goalId, "containerType", containerType)));
		}
	}

	private static String key(Kind kind, String name) {
		return kind.name() + ":" + name.toLowerCase(java.util.Locale.ROOT);
	}
}
