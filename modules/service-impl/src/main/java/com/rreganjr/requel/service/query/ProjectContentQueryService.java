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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.spi.AnnotationFreshness;
import com.rreganjr.requel.gateway.ProjectContentTooLargeException;
import com.rreganjr.requel.gateway.ProjectContentTooLargeException.SectionSize;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.service.api.dto.ActorContentDto;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.GlossaryTermContentDto;
import com.rreganjr.requel.service.api.dto.GoalContentDto;
import com.rreganjr.requel.service.api.dto.GoalRelationContentDto;
import com.rreganjr.requel.service.api.dto.IssueDto;
import com.rreganjr.requel.service.api.dto.NoteDto;
import com.rreganjr.requel.service.api.dto.ProjectContentDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ScenarioContentDto;
import com.rreganjr.requel.service.api.dto.StakeholderContentDto;
import com.rreganjr.requel.service.api.dto.StepContentDto;
import com.rreganjr.requel.service.api.dto.StepRefDto;
import com.rreganjr.requel.service.api.dto.StoryContentDto;
import com.rreganjr.requel.service.api.dto.UseCaseContentDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.service.command.AnnotationCommandRegistrar;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.TagToken;
import com.rreganjr.requel.tagging.spi.TaggableTypeRegistry;

/**
 * Issue #274: the project content read — a project's whole normative content in one call, for
 * gateway callers (MCP, CLI, REST). Entities refer to one another by id, every list is sorted by
 * id, and nothing is truncated: a project over the character cap is refused with
 * {@link ProjectContentTooLargeException}, naming each section's size.
 * <p>
 * Provenance and references are never read here (#272 P7): the service does not touch the
 * provenance tables, so the read cannot carry a source or its locator.
 * <p>
 * Scenarios are walked recursively from the project's scenarios, as the XML export's
 * {@code getAllScenariosAndSteps} does, so a sub-scenario used as a step and a step shared by
 * several scenarios each appear once.
 */
@Service
@Transactional(readOnly = true)
public class ProjectContentQueryService {

	/** The annotations a read carries. */
	public enum AnnotationMode {
		/** None: every entity's annotations are empty. */
		NONE,
		/** Notes and unresolved issues. */
		OPEN,
		/** Every note and issue, resolved ones included. */
		ALL;

		/** @throws IllegalArgumentException for anything but none, open or all (ignoring case) */
		public static AnnotationMode parse(String value) {
			if (value == null || value.isBlank()) {
				return ALL;
			}
			try {
				return valueOf(value.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Unsupported annotations value: " + value
						+ " (expected none, open or all)");
			}
		}
	}

	/** The characters a read may carry before it is refused; 0 or less for no cap. */
	public static final String MAX_CHARACTERS_PROPERTY = "requel.gateway.content.max-characters";

	private final ProjectRepository projectRepository;
	private final CurrentUserResolver currentUserResolver;
	private final ProjectQueryController projectQueryController;
	private final ObjectProvider<AnnotationFreshness> annotationFreshness;
	private final ObjectProvider<TagRepository> tagRepository;
	private final ObjectProvider<TaggableTypeRegistry> taggableTypeRegistry;
	private final int maxCharacters;

	public ProjectContentQueryService(ProjectRepository projectRepository,
			CurrentUserResolver currentUserResolver, ProjectQueryController projectQueryController,
			ObjectProvider<AnnotationFreshness> annotationFreshness,
			ObjectProvider<TagRepository> tagRepository,
			ObjectProvider<TaggableTypeRegistry> taggableTypeRegistry,
			@Value("${" + MAX_CHARACTERS_PROPERTY + ":400000}") int maxCharacters) {
		this.projectRepository = projectRepository;
		this.currentUserResolver = currentUserResolver;
		this.projectQueryController = projectQueryController;
		this.annotationFreshness = annotationFreshness;
		this.tagRepository = tagRepository;
		this.taggableTypeRegistry = taggableTypeRegistry;
		this.maxCharacters = Math.max(0, maxCharacters);
	}

	/**
	 * @param annotations none, open or all, ignoring case; null means all
	 * @throws com.rreganjr.requel.project.exception.NoSuchProjectException when there is no such
	 *                                                                      project
	 * @throws AuthorizationException          when the caller cannot read the project
	 * @throws IllegalArgumentException        for an unsupported annotations value
	 * @throws ProjectContentTooLargeException when the content is over the cap
	 */
	public ProjectContentDto read(String projectName, String annotations) {
		AnnotationMode mode = AnnotationMode.parse(annotations);
		Project project = projectRepository.findProjectByName(projectName);
		if (!ProjectReadAccess.canRead(project, currentUserResolver.resolve())) {
			throw new AuthorizationException("You do not have access to this project.");
		}
		return build(project, mode, true);
	}

	/**
	 * Issue #275: the same read for an in-process caller that has already checked access (the
	 * report run computing the project version). No character cap: a version must exist for any
	 * project that can be rendered.
	 */
	public ProjectContentDto readUncapped(Project project, AnnotationMode mode) {
		return build(project, mode, false);
	}

	private ProjectContentDto build(Project project, AnnotationMode mode, boolean enforceCap) {
		List<Scenario> scenarios = new ArrayList<>();
		List<Step> steps = new ArrayList<>();
		collectScenariosAndSteps(project, scenarios, steps);

		Build build = new Build(mode, staleness(project, scenarios, steps));

		List<StakeholderContentDto> stakeholders = sorted(project.getStakeholders()).stream()
				.map(s -> build.stakeholder(s)).toList();
		List<GoalContentDto> goals = sorted(project.getGoals()).stream()
				.map(g -> build.goal(g)).toList();
		List<StoryContentDto> stories = sorted(project.getStories()).stream()
				.map(s -> build.story(s)).toList();
		List<ActorContentDto> actors = sorted(project.getActors()).stream()
				.map(a -> build.actor(a)).toList();
		List<UseCaseContentDto> useCases = sorted(project.getUseCases()).stream()
				.map(u -> build.useCase(u)).toList();
		List<ScenarioContentDto> scenarioDtos = sorted(scenarios).stream()
				.map(s -> build.scenario(s)).toList();
		List<StepContentDto> stepDtos = sorted(steps).stream()
				.map(s -> build.step(s)).toList();
		List<GlossaryTermContentDto> glossary = sorted(project.getGlossaryTerms()).stream()
				.map(t -> build.term(t)).toList();

		int characters = build.totalCharacters();
		if (enforceCap && maxCharacters > 0 && characters > maxCharacters) {
			throw new ProjectContentTooLargeException(project.getName(), characters,
					maxCharacters, build.sections());
		}
		ProjectDto summary = projectQueryController.toProjectDto(project);
		return new ProjectContentDto(summary, mode.name(), characters, maxCharacters,
				stakeholders, goals, stories, actors, useCases, scenarioDtos, stepDtos, glossary);
	}

	/**
	 * Every scenario reachable from the project's scenarios, and every plain step they use, each
	 * once. A scenario used as a step is a scenario, not a step.
	 */
	static void collectScenariosAndSteps(Project project, List<Scenario> scenarios,
			List<Step> steps) {
		Set<Scenario> seenScenarios = new LinkedHashSet<>();
		Set<Step> seenSteps = new LinkedHashSet<>();
		Deque<Scenario> toExamine = new ArrayDeque<>(project.getScenarios());
		while (!toExamine.isEmpty()) {
			Scenario scenario = toExamine.pop();
			if (!seenScenarios.add(scenario)) {
				continue;
			}
			for (Step step : scenario.getSteps()) {
				if (step instanceof Scenario sub) {
					toExamine.add(sub);
				} else {
					seenSteps.add(step);
				}
			}
		}
		scenarios.addAll(seenScenarios);
		steps.addAll(seenSteps);
	}

	private AnnotationFreshness.StaleAnnotations staleness(Project project,
			List<Scenario> scenarios, List<Step> steps) {
		AnnotationFreshness freshness = annotationFreshness.getIfAvailable(
				() -> AnnotationFreshness.NONE);
		List<Annotatable> all = new ArrayList<>();
		all.addAll(project.getStakeholders());
		all.addAll(project.getGoals());
		all.addAll(project.getStories());
		all.addAll(project.getActors());
		all.addAll(project.getUseCases());
		all.addAll(scenarios);
		all.addAll(steps);
		all.addAll(project.getGlossaryTerms());
		return freshness.staleAnnotations(all);
	}

	private static <T extends ProjectOrDomainEntity> List<T> sorted(Collection<T> entities) {
		List<T> list = new ArrayList<>(entities);
		list.sort(Comparator.comparing(ProjectOrDomainEntity::getId,
				Comparator.nullsLast(Comparator.naturalOrder())));
		return list;
	}

	private static <T> List<Long> ids(Collection<T> items, Function<T, Long> idFn) {
		if (items == null) {
			return List.of();
		}
		return items.stream().map(idFn).filter(Objects::nonNull).sorted().toList();
	}

	private static Long idOf(ProjectOrDomainEntity entity) {
		return entity != null ? entity.getId() : null;
	}

	private static String nameOf(Enum<?> value) {
		return value != null ? value.name() : null;
	}

	/** One read's DTO building, with its annotation mode, staleness and character tallies. */
	private final class Build {

		private final AnnotationMode mode;
		private final AnnotationFreshness.StaleAnnotations stale;
		private final Map<String, int[]> tallies = new LinkedHashMap<>();

		Build(AnnotationMode mode, AnnotationFreshness.StaleAnnotations stale) {
			this.mode = mode;
			this.stale = stale;
			for (String section : List.of("stakeholders", "goals", "stories", "actors", "useCases",
					"scenarios", "steps", "glossary", "annotations")) {
				tallies.put(section, new int[2]);
			}
		}

		StakeholderContentDto stakeholder(Stakeholder s) {
			String text = s instanceof NonUserStakeholder nus ? nus.getText() : null;
			count("stakeholders", s.getDisplayName(), text);
			return new StakeholderContentDto(s.getId(), s.getVersion(), s.getDisplayName(),
					s.isUserStakeholder() ? "user" : "non-user", text,
					ids(s.getGoals(), Goal::getId), tags(s), annotations(s));
		}

		GoalContentDto goal(Goal g) {
			count("goals", g.getName(), g.getText());
			List<GoalRelationContentDto> relations = g.getRelationsFromThisGoal().stream()
					.map((GoalRelation r) -> new GoalRelationContentDto(nameOf(r.getRelationType()),
							idOf(r.getToGoal())))
					.sorted(Comparator.comparing(GoalRelationContentDto::goalId,
							Comparator.nullsLast(Comparator.naturalOrder()))
							.thenComparing(GoalRelationContentDto::relationType,
									Comparator.nullsLast(Comparator.naturalOrder())))
					.toList();
			return new GoalContentDto(g.getId(), g.getVersion(), g.getName(), g.getText(),
					relations, tags(g), annotations(g));
		}

		StoryContentDto story(Story s) {
			count("stories", s.getName(), s.getText());
			return new StoryContentDto(s.getId(), s.getVersion(), s.getName(), s.getText(),
					nameOf(s.getStoryType()), ids(s.getGoals(), Goal::getId),
					ids(s.getActors(), Actor::getId), tags(s), annotations(s));
		}

		ActorContentDto actor(Actor a) {
			count("actors", a.getName(), a.getText());
			return new ActorContentDto(a.getId(), a.getVersion(), a.getName(), a.getText(),
					ids(a.getGoals(), Goal::getId), tags(a), annotations(a));
		}

		UseCaseContentDto useCase(UseCase u) {
			count("useCases", u.getName(), u.getText());
			return new UseCaseContentDto(u.getId(), u.getVersion(), u.getName(), u.getText(),
					idOf(u.getPrimaryActor()), idOf(u.getScenario()),
					ids(u.getAdditionalScenarios(), Scenario::getId), ids(u.getGoals(), Goal::getId),
					ids(u.getActors(), Actor::getId), ids(u.getStories(), Story::getId), tags(u),
					annotations(u));
		}

		ScenarioContentDto scenario(Scenario s) {
			count("scenarios", s.getName(), s.getText());
			List<StepRefDto> stepRefs = s.getSteps().stream()
					.map(step -> new StepRefDto(step instanceof Scenario ? "Scenario" : "Step",
							step.getId()))
					.toList();
			return new ScenarioContentDto(s.getId(), s.getVersion(), s.getName(), s.getText(),
					nameOf(s.getType()), stepRefs, tags(s), annotations(s));
		}

		StepContentDto step(Step s) {
			count("steps", s.getName(), s.getText());
			return new StepContentDto(s.getId(), s.getVersion(), s.getName(), s.getText(),
					nameOf(s.getType()), tags(s), annotations(s));
		}

		GlossaryTermContentDto term(GlossaryTerm t) {
			count("glossary", t.getName(), t.getText());
			return new GlossaryTermContentDto(t.getId(), t.getVersion(), t.getName(), t.getText(),
					idOf(t.getCanonicalTerm()), tags(t), annotations(t));
		}

		private AnnotationsDto annotations(Annotatable annotatable) {
			if (mode == AnnotationMode.NONE) {
				return new AnnotationsDto(List.of(), List.of());
			}
			List<NoteDto> notes = new ArrayList<>();
			List<IssueDto> issues = new ArrayList<>();
			for (Annotation annotation : annotatable.getAnnotations()) {
				if (annotation instanceof Note note) {
					NoteDto dto = AnnotationCommandRegistrar.toNoteDto(note,
							stale.isStale(annotatable, note));
					countAnnotation(dto.text());
					notes.add(dto);
				} else if (annotation instanceof Issue issue) {
					if (mode == AnnotationMode.OPEN && issue.getResolvedByPosition() != null) {
						continue;
					}
					IssueDto dto = AnnotationCommandRegistrar.toIssueDto(issue,
							stale.isStale(annotatable, issue));
					countAnnotation(dto.text());
					if (dto.positions() != null) {
						dto.positions().forEach(p -> {
							add("annotations", 0, p.text());
							if (p.arguments() != null) {
								p.arguments().forEach(a -> add("annotations", 0, a.text()));
							}
						});
					}
					issues.add(dto);
				}
			}
			notes.sort(Comparator.comparing(NoteDto::id,
					Comparator.nullsLast(Comparator.naturalOrder())));
			issues.sort(Comparator.comparing(IssueDto::id,
					Comparator.nullsLast(Comparator.naturalOrder())));
			return new AnnotationsDto(notes, issues);
		}

		/** Sorted {@code category:value} tokens; empty without the tagging module. */
		private List<String> tags(ProjectOrDomainEntity entity) {
			TagRepository tags = tagRepository.getIfAvailable();
			TaggableTypeRegistry registry = taggableTypeRegistry.getIfAvailable();
			if (tags == null || registry == null || entity.getId() == null) {
				return List.of();
			}
			String discriminator = registry.resolveDiscriminator(ClassUtils.getUserClass(entity))
					.orElse(null);
			if (discriminator == null) {
				return List.of();
			}
			List<Tag> found = tags.findTagsOnEntity(discriminator, entity.getId());
			if (found == null || found.isEmpty()) {
				return List.of();
			}
			return found.stream()
					.map(t -> new TagToken(t.getCategory(), t.getValue(), null).toToken())
					.sorted()
					.toList();
		}

		private void count(String section, String name, String text) {
			add(section, 1, name);
			add(section, 0, text);
		}

		private void countAnnotation(String text) {
			add("annotations", 1, text);
		}

		private void add(String section, int entities, String text) {
			int[] tally = tallies.get(section);
			tally[0] += entities;
			tally[1] += text != null ? text.length() : 0;
		}

		int totalCharacters() {
			return tallies.values().stream().mapToInt(t -> t[1]).sum();
		}

		Map<String, SectionSize> sections() {
			Map<String, SectionSize> sizes = new LinkedHashMap<>();
			tallies.forEach((section, t) -> sizes.put(section, new SectionSize(t[0], t[1])));
			return sizes;
		}
	}
}
