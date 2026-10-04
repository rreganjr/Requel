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
package com.rreganjr.requel.assistant.core.corpus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.context.provider.GoalSiblingsProvider;
import com.rreganjr.requel.assistant.core.corpus.CorpusCandidateFinder.Member;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.TargetFingerprint;
import com.rreganjr.requel.project.UseCase;

/**
 * Issue #266: the members of a corpus set, read from a loaded project, with the structural links
 * between them. The caller holds the transaction.
 *
 * <p>Linked members are meant to be alike: a use case and its scenario, a scenario and its steps,
 * two steps of one scenario, an entity and the goal, story, actor or glossary term it refers to, a
 * glossary term and its canonical term and alternates (and the alternates of one canonical term
 * with each other), an actor and the glossary term of the same name. The finder
 * doesn't report them as overlaps, but still reports their conflicts.
 */
public final class CorpusMembers {

	/**
	 * The members of a set, the links between them, the glossary tokens, and each member's
	 * {@link TargetFingerprint} as read.
	 */
	public record CorpusSet(List<Member> members, Set<Set<EntityRef>> links,
			Map<String, String> glossaryTokens, Map<EntityRef, String> fingerprints) {

		/** The member with {@code ref}, or null. */
		public Member member(EntityRef ref) {
			for (Member member : members) {
				if (member.ref().equals(ref)) {
					return member;
				}
			}
			return null;
		}

		/** The members' references as {@code Type:id}, for a result's corpus scope. */
		public List<String> scope() {
			List<String> scope = new ArrayList<>(members.size());
			for (Member member : members) {
				scope.add(member.ref().entityType() + ":" + member.ref().entityId());
			}
			return scope;
		}

		public boolean linked(EntityRef a, EntityRef b) {
			return links.contains(Set.of(a, b));
		}
	}

	private CorpusMembers() {
	}

	/** Every goal, story, actor, use case, scenario, step and glossary term of the project. */
	public static CorpusSet project(Project project) {
		Map<EntityRef, Member> members = new LinkedHashMap<>();
		Map<EntityRef, String> fingerprints = new HashMap<>();
		Set<Set<EntityRef>> links = new HashSet<>();
		for (Goal goal : project.getGoals()) {
			EntityRef ref = add(members, "Goal", goal.getId(), goal.getName(), goal.getText());
			fingerprint(fingerprints, ref, goal);
			linkAll(links, ref, goal.getReferers());
		}
		for (Story story : project.getStories()) {
			EntityRef ref = add(members, "Story", story.getId(), story.getName(), story.getText());
			fingerprint(fingerprints, ref, story);
			linkAll(links, ref, story.getReferers());
			link(links, ref, ref(story.getPrimaryActor()));
		}
		for (Actor actor : project.getActors()) {
			EntityRef ref = add(members, "Actor", actor.getId(), actor.getName(), actor.getText());
			fingerprint(fingerprints, ref, actor);
			linkAll(links, ref, actor.getReferers());
		}
		for (UseCase useCase : project.getUseCases()) {
			EntityRef ref = add(members, "UseCase", useCase.getId(), useCase.getName(),
					useCase.getText());
			fingerprint(fingerprints, ref, useCase);
			link(links, ref, ref(useCase.getPrimaryActor()));
			link(links, ref, ref(useCase.getScenario()));
			for (Scenario scenario : useCase.getAdditionalScenarios()) {
				link(links, ref, ref(scenario));
			}
		}
		for (Scenario scenario : project.getScenarios()) {
			EntityRef ref = add(members, "Scenario", scenario.getId(), scenario.getName(),
					scenario.getText());
			fingerprint(fingerprints, ref, scenario);
			List<EntityRef> steps = new ArrayList<>();
			for (Step step : scenario.getSteps()) {
				EntityRef stepRef = step instanceof Scenario nested ? ref(nested)
						: add(members, "Step", step.getId(), step.getName(), step.getText());
				fingerprint(fingerprints, stepRef, step);
				link(links, ref, stepRef);
				steps.add(stepRef);
			}
			for (int i = 0; i < steps.size(); i++) {
				for (int j = i + 1; j < steps.size(); j++) {
					link(links, steps.get(i), steps.get(j));
				}
			}
		}
		Map<String, EntityRef> actorsByName = new LinkedHashMap<>();
		for (Actor actor : project.getActors()) {
			if (actor.getName() != null && actor.getId() != null) {
				actorsByName.put(actor.getName().strip().toLowerCase(Locale.ROOT),
						EntityRef.of("Actor", actor.getId()));
			}
		}
		for (GlossaryTerm term : project.getGlossaryTerms()) {
			EntityRef ref = add(members, "GlossaryTerm", term.getId(), term.getName(),
					term.getText());
			fingerprint(fingerprints, ref, term);
			linkAll(links, ref, term.getReferers());
			link(links, ref, ref(term.getCanonicalTerm()));
			// a term, its canonical term and every alternate of either are one concept
			List<EntityRef> family = new ArrayList<>();
			family.add(ref);
			GlossaryTerm canonical = term.getCanonicalTerm() != null ? term.getCanonicalTerm() : term;
			family.add(ref(canonical));
			for (GlossaryTerm alternate : canonical.getAlternateTerms()) {
				family.add(ref(alternate));
			}
			for (GlossaryTerm alternate : term.getAlternateTerms()) {
				family.add(ref(alternate));
			}
			for (EntityRef other : family) {
				link(links, ref, other);
			}
			if (term.getName() != null) {
				link(links, ref, actorsByName.get(term.getName().strip().toLowerCase(Locale.ROOT)));
			}
		}
		// links only between members
		links.removeIf(pair -> !members.keySet().containsAll(pair));
		fingerprints.keySet().retainAll(members.keySet());
		return new CorpusSet(new ArrayList<>(members.values()), Set.copyOf(links),
				glossaryTokens(project), Map.copyOf(fingerprints));
	}

	/** The kind of set a corpus run analyzes, decided by its root. */
	public enum SetKind {
		/** Every entity of the project. */
		PROJECT,
		/** A goal, the goals reachable through its relations, and what refers to any of them. */
		GOAL,
		/** A use case, its scenarios and their steps, and its primary actor. */
		USE_CASE;

		/** The kind for a root entity, or null when it can't root a set. */
		public static SetKind of(Object root) {
			if (root instanceof Project) {
				return PROJECT;
			}
			if (root instanceof Goal) {
				return GOAL;
			}
			if (root instanceof UseCase) {
				return USE_CASE;
			}
			return null;
		}
	}

	/**
	 * The set rooted at {@code root}: a project, a goal or a use case of {@code project}.
	 *
	 * @throws IllegalArgumentException for any other root
	 */
	public static CorpusSet of(Object root, Project project) {
		SetKind kind = SetKind.of(root);
		if (kind == null) {
			throw new IllegalArgumentException("a corpus set is rooted at a project, a goal or a"
					+ " use case, not " + (root == null ? "null" : root.getClass().getSimpleName()));
		}
		CorpusSet all = project(project);
		if (kind == SetKind.PROJECT) {
			return all;
		}
		Set<EntityRef> refs = kind == SetKind.GOAL ? goalSet((Goal) root)
				: useCaseSet((UseCase) root);
		List<Member> members = new ArrayList<>();
		for (Member member : all.members()) {
			if (refs.contains(member.ref())) {
				members.add(member);
			}
		}
		Set<Set<EntityRef>> links = new HashSet<>();
		for (Set<EntityRef> link : all.links()) {
			if (refs.containsAll(link)) {
				links.add(link);
			}
		}
		Map<EntityRef, String> fingerprints = new HashMap<>(all.fingerprints());
		fingerprints.keySet().retainAll(refs);
		return new CorpusSet(members, Set.copyOf(links), all.glossaryTokens(),
				Map.copyOf(fingerprints));
	}

	private static Set<EntityRef> goalSet(Goal root) {
		Set<EntityRef> refs = new HashSet<>();
		java.util.Deque<Goal> pending = new java.util.ArrayDeque<>();
		Set<Goal> seen = new HashSet<>();
		pending.add(root);
		while (!pending.isEmpty()) {
			Goal goal = pending.poll();
			if (!seen.add(goal)) {
				continue;
			}
			addRef(refs, goal);
			for (com.rreganjr.requel.project.GoalRelation relation : goal.getRelationsFromThisGoal()) {
				pending.add(relation.getToGoal());
			}
			for (com.rreganjr.requel.project.GoalRelation relation : goal.getRelationsToThisGoal()) {
				pending.add(relation.getFromGoal());
			}
			for (Object referer : goal.getReferers()) {
				addRef(refs, referer);
			}
		}
		return refs;
	}

	private static Set<EntityRef> useCaseSet(UseCase root) {
		Set<EntityRef> refs = new HashSet<>();
		addRef(refs, root);
		addRef(refs, root.getPrimaryActor());
		List<Scenario> scenarios = new ArrayList<>();
		if (root.getScenario() != null) {
			scenarios.add(root.getScenario());
		}
		scenarios.addAll(root.getAdditionalScenarios());
		Set<Scenario> seen = new HashSet<>();
		while (!scenarios.isEmpty()) {
			Scenario scenario = scenarios.remove(0);
			if (!seen.add(scenario)) {
				continue;
			}
			addRef(refs, scenario);
			for (Step step : scenario.getSteps()) {
				if (step instanceof Scenario nested) {
					scenarios.add(nested);
				} else {
					refs.add(EntityRef.of("Step", step.getId()));
				}
			}
		}
		return refs;
	}

	private static void addRef(Set<EntityRef> refs, Object entity) {
		EntityRef ref = ref(entity);
		if (ref != null) {
			refs.add(ref);
		}
	}

	/** Each glossary term and alternate name to the token it stands for (#261). */
	public static Map<String, String> glossaryTokens(ProjectOrDomain project) {
		return GoalSiblingsProvider.glossaryTokens(project);
	}

	/** The reference of a project entity, by its domain interface, or null. */
	public static EntityRef ref(Object entity) {
		if (entity instanceof ProjectOrDomainEntity e && e.getId() != null) {
			return EntityRef.of(e.getProjectOrDomainEntityInterface().getSimpleName(), e.getId());
		}
		return null;
	}

	private static void fingerprint(Map<EntityRef, String> fingerprints, EntityRef ref,
			Object entity) {
		String fingerprint = ref == null ? null : TargetFingerprint.of(entity);
		if (fingerprint != null) {
			fingerprints.putIfAbsent(ref, fingerprint);
		}
	}

	private static void linkAll(Set<Set<EntityRef>> links, EntityRef ref,
			Collection<?> referers) {
		if (referers == null) {
			return;
		}
		for (Object referer : referers) {
			link(links, ref, ref(referer));
		}
	}

	private static void link(Set<Set<EntityRef>> links, EntityRef a, EntityRef b) {
		if (a != null && b != null && !a.equals(b)) {
			links.add(Set.of(a, b));
		}
	}

	private static EntityRef add(Map<EntityRef, Member> members, String type, Long id, String name,
			String text) {
		if (id == null) {
			return null;
		}
		EntityRef ref = EntityRef.of(type, id);
		members.putIfAbsent(ref, new Member(ref, name, text));
		return ref;
	}
}
