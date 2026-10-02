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
package com.rreganjr.requel.assistant.core.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.core.context.provider.ActorReferencesProvider;
import com.rreganjr.requel.assistant.core.context.provider.GlossaryRelatedProvider;
import com.rreganjr.requel.assistant.core.context.provider.GoalRelationsProvider;
import com.rreganjr.requel.assistant.core.context.provider.GoalSiblingsProvider;
import com.rreganjr.requel.assistant.core.context.provider.GoalStakeholdersProvider;
import com.rreganjr.requel.assistant.core.context.provider.ScenarioUseCasesProvider;
import com.rreganjr.requel.assistant.core.context.provider.StepSequenceProvider;
import com.rreganjr.requel.assistant.core.context.provider.ProjectNamesProvider;
import com.rreganjr.requel.assistant.core.context.provider.StoryActorsProvider;
import com.rreganjr.requel.assistant.core.context.provider.UseCaseScenariosProvider;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ActorContainer;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.GoalRelationType;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectTeam;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.user.User;

/** Issue #261: each built-in provider, and how the builder composes and budgets them. */
class ContextProvidersTest {

	private final ContextPackSizeLimits limits = new ContextPackSizeLimits();
	private final EntityContextPackBuilder builder = builder(limits);
	private final Project project = mock(Project.class);

	private EntityContextPackBuilder builder(ContextPackSizeLimits sizeLimits) {
		EntityContextPackBuilder b = new EntityContextPackBuilder(new NoOpRedactionPolicy(),
				sizeLimits, Clock.fixed(Instant.parse("2026-10-02T00:00:00Z"), ZoneOffset.UTC));
		b.setProviders(new ContextProviderRegistry(List.of(new GoalRelationsProvider(),
				new GoalSiblingsProvider(), new GoalStakeholdersProvider(),
				new UseCaseScenariosProvider(), new ScenarioUseCasesProvider(),
				new StepSequenceProvider(), new StoryActorsProvider(),
				new ActorReferencesProvider(), new GlossaryRelatedProvider(),
				new ProjectNamesProvider())));
		return b;
	}

	// ---- composition -------------------------------------------------------------------

	@Test
	void anEntityOnlyPackSerialisesWithoutAContextField() throws Exception {
		Goal goal = goal(1L, "Fast search", "Search is fast");

		EntityContextPack pack = builder.build(goal);

		assertThat(pack.context()).isEmpty();
		assertThat(com.fasterxml.jackson.databind.json.JsonMapper.builder()
				.disable(com.fasterxml.jackson.databind.MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES)
				.build().writeValueAsString(pack))
				.doesNotContain("\"context\"");
	}

	@Test
	void anUnknownProviderIsRefused() {
		Goal goal = goal(1L, "G", "t");

		assertThatThrownBy(() -> builder.build(goal, spec("no-such-provider")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no-such");
	}

	@Test
	void aProviderForAnotherTypeAddsNothing() {
		Goal goal = goal(1L, "G", "t");

		EntityContextPack pack = builder.build(goal, spec("usecase-scenarios", "story-actors"));

		assertThat(pack.context()).isEmpty();
	}

	@Test
	void aProviderOverItsShareIsTrimmedFromTheTailAndNoted() {
		Goal target = goal(1L, "Target", "t");
		List<Goal> all = new ArrayList<>(List.of(target));
		for (long i = 2; i <= 40; i++) {
			all.add(goal(i, "Goal " + i, "x".repeat(200)));
		}
		Set<Goal> goals = new LinkedHashSet<>(all);
		when(project.getGoals()).thenReturn(goals);

		EntityContextPack pack = builder.build(target,
				new PackSpec(List.of("entity", "goal-siblings"), Map.of("goal-siblings", 1000), 0));

		ContextSection section = pack.context().get(0);
		assertThat(section.truncated()).isTrue();
		assertThat(section.available()).isEqualTo(39);
		assertThat(section.shown()).isLessThan(39).isEqualTo(section.entities().size());
		assertThat(pack.metadata().truncationNotes())
				.contains("goal-siblings: " + section.shown() + " of 39 shown");
	}

	@Test
	void aProviderGetsNothingOnceThePackIsFull() {
		Goal target = goal(1L, "Target", "y".repeat(500));
		Goal other = goal(2L, "Other", "t");
		Set<Goal> goals = new LinkedHashSet<>(List.of(target, other));
		when(project.getGoals()).thenReturn(goals);

		EntityContextPack pack = builder.build(target,
				new PackSpec(List.of("entity", "goal-siblings"), Map.of(), 400));

		assertThat(pack.context()).isEmpty();
		assertThat(pack.metadata().truncationNotes())
				.contains("goal-siblings: skipped, the pack's character budget is used up");
	}

	@Test
	void dropsTheLastSectionFirstAndNotesIt() {
		Goal target = goal(1L, "Target", "t");
		Goal other = goal(2L, "Other", "t");
		Set<Goal> goals = new LinkedHashSet<>(List.of(target, other));
		when(project.getGoals()).thenReturn(goals);
		relate(target, other, GoalRelationType.Refines);

		EntityContextPack pack = builder.build(target, spec("goal-relations", "goal-siblings"));
		EntityContextPack trimmed = pack.withoutLastSection();

		assertThat(pack.context()).extracting(ContextSection::providerId)
				.containsExactly("goal-relations", "goal-siblings");
		assertThat(trimmed.context()).extracting(ContextSection::providerId)
				.containsExactly("goal-relations");
		assertThat(trimmed.metadata().truncationNotes())
				.contains("goal-siblings: dropped to fit the input cap");
	}

	@Test
	void providerTextIsRedacted() {
		EntityContextPackBuilder redacting = new EntityContextPackBuilder(
				(path, value, notes) -> {
					if (value != null && value.contains("@")) {
						notes.add(path + ": EMAIL x1");
						return value.replaceAll("\\S+@\\S+", "[EMAIL]");
					}
					return value;
				}, limits, Clock.systemUTC());
		redacting.setProviders(new ContextProviderRegistry(List.of(new GoalRelationsProvider())));
		Goal target = goal(1L, "Target", "t");
		Goal other = goal(2L, "Other", "mail ops@example.com");
		relate(target, other, GoalRelationType.Supports);

		EntityContextPack pack = redacting.build(target, spec("goal-relations"));

		assertThat(pack.context().get(0).entities().get(0).text()).isEqualTo("mail [EMAIL]");
		assertThat(pack.metadata().redactedFields())
				.contains("context.goal-relations.Goal[2].text: EMAIL x1");
	}

	// ---- providers ---------------------------------------------------------------------

	@Test
	void goalRelationsReadFromTheReviewedGoal() {
		Goal target = goal(1L, "Target", "t");
		Goal parent = goal(2L, "Parent", "p");
		Goal child = goal(3L, "Child", "c");
		Goal rival = goal(4L, "Rival", "r");
		relate(target, parent, GoalRelationType.Refines);
		relate(child, target, GoalRelationType.Refines);
		// symmetric, recorded both ways: shown once
		relate(target, rival, GoalRelationType.Conflicts);
		relate(rival, target, GoalRelationType.Conflicts);

		List<RelatedEntity> entities = section(target, "goal-relations").entities();

		assertThat(entities).extracting(e -> e.relation() + " " + e.name())
				.containsExactly("Conflicts Rival", "Refined by Child", "Refines Parent");
	}

	@Test
	void siblingsRankRelatedThenSameStakeholderThenSimilar() {
		Goal target = goal(1L, "Search returns results quickly", "Search is fast for users");
		Goal related = goal(2L, "Zebra", "unrelated words");
		Goal shared = goal(3L, "Yak", "other words");
		Goal similar = goal(4L, "Quick search results", "Users get search results fast");
		Goal unrelated = goal(5L, "Alpha", "Invoices are emailed");
		Set<Goal> goals = new LinkedHashSet<>(List.of(target, related, shared, similar, unrelated));
		when(project.getGoals()).thenReturn(goals);
		relate(target, related, GoalRelationType.Supports);
		NonUserStakeholder regulator = nonUser(9L, "Regulator", "sets rules");
		hold(regulator, target, shared);

		List<RelatedEntity> entities = section(target, "goal-siblings").entities();

		assertThat(entities).extracting(RelatedEntity::name)
				.containsExactly("Zebra", "Yak", "Quick search results", "Alpha");
		assertThat(entities).extracting(RelatedEntity::relation).containsExactly("related goal",
				"same stakeholder", "sibling goal", "sibling goal");
	}

	@Test
	void stakeholdersShowUsersByRoleOnly() {
		Goal target = goal(1L, "Target", "t");
		Goal other = goal(2L, "Other goal", "o");
		NonUserStakeholder regulator = nonUser(9L, "Regulator", "sets the rules");
		UserStakeholder chris = mock(UserStakeholder.class);
		when(chris.getId()).thenReturn(10L);
		when(chris.getName()).thenReturn("Chris Smith");
		doReturn(UserStakeholder.class).when(chris).getProjectOrDomainEntityInterface();
		User user = mock(User.class);
		when(user.getUsername()).thenReturn("csmith");
		when(chris.getUser()).thenReturn(user);
		ProjectTeam team = mock(ProjectTeam.class);
		when(team.getName()).thenReturn("Leadership");
		when(chris.getTeam()).thenReturn(team);
		hold(regulator, target, other);
		hold(chris, target);

		List<RelatedEntity> entities = section(target, "goal-stakeholders").entities();

		assertThat(entities).extracting(RelatedEntity::name)
				.containsExactly("Regulator", "user-1 (team Leadership)");
		assertThat(entities.get(0).text()).isEqualTo("sets the rules");
		assertThat(entities.get(0).children()).extracting(e -> e.relation() + " " + e.name())
				.containsExactly("also holds Other goal");
		assertThat(entities.toString()).doesNotContain("Chris").doesNotContain("csmith");
	}

	@Test
	void useCaseScenariosListStepsInOrderAndMarkThePrimary() {
		UseCase useCase = entity(UseCase.class, 1L, "Check out", "buy things");
		Step one = entity(Step.class, 11L, "Pick", "pick items");
		Step two = entity(Step.class, 12L, "Pay", "pay");
		Scenario primary = entity(Scenario.class, 10L, "Main", "main flow");
		when(primary.getSteps()).thenReturn(List.of(one, two));
		Scenario alternate = entity(Scenario.class, 20L, "Card declined", "alt");
		when(useCase.getScenario()).thenReturn(primary);
		when(useCase.getAdditionalScenarios()).thenReturn(Set.of(alternate));

		List<RelatedEntity> entities = section(useCase, "usecase-scenarios").entities();

		assertThat(entities).extracting(e -> e.relation() + " " + e.name())
				.containsExactly("primary scenario Main", "additional scenario Card declined");
		assertThat(entities.get(0).children()).extracting(e -> e.relation() + " " + e.name())
				.containsExactly("step 1 of 2 Pick", "step 2 of 2 Pay");
	}

	@Test
	void scenarioUseCasesCarryThePrimaryActor() {
		Scenario scenario = entity(Scenario.class, 10L, "Main", "flow");
		UseCase useCase = entity(UseCase.class, 1L, "Check out", "buy");
		Actor shopper = entity(Actor.class, 5L, "Shopper", "buys");
		when(useCase.getPrimaryActor()).thenReturn(shopper);
		when(scenario.getUsingUseCases()).thenReturn(Set.of(useCase));

		List<RelatedEntity> entities = section(scenario, "scenario-usecases").entities();

		assertThat(entities.get(0).name()).isEqualTo("Check out");
		assertThat(entities.get(0).children()).extracting(e -> e.relation() + " " + e.name())
				.containsExactly("primary actor Shopper");
	}

	@Test
	void stepSequenceShowsTheNeighbours() {
		Step one = entity(Step.class, 11L, "Pick", "pick");
		Step two = entity(Step.class, 12L, "Pay", "pay");
		Step three = entity(Step.class, 13L, "Ship", "ship");
		Scenario scenario = entity(Scenario.class, 10L, "Main", "flow");
		when(scenario.getSteps()).thenReturn(List.of(one, two, three));
		UseCase useCase = entity(UseCase.class, 1L, "Check out", "buy");
		when(scenario.getUsingUseCases()).thenReturn(Set.of(useCase));
		when(two.getUsingScenarios()).thenReturn(Set.of(scenario));

		List<RelatedEntity> entities = section(two, "step-sequence").entities();

		assertThat(entities.get(0).relation()).isEqualTo("step 2 of 3 in scenario");
		assertThat(entities.get(0).children()).extracting(e -> e.relation() + " " + e.name())
				.containsExactly("previous step Pick", "next step Ship", "use case Check out");
	}

	@Test
	void storyActorsMarkThePrimary() {
		Story story = entity(Story.class, 1L, "Story", "s");
		Actor primary = entity(Actor.class, 5L, "Shopper", "buys");
		Actor clerk = entity(Actor.class, 6L, "Clerk", "helps");
		when(story.getPrimaryActor()).thenReturn(primary);
		when(story.getActors()).thenReturn(Set.of(primary, clerk));

		assertThat(section(story, "story-actors").entities())
				.extracting(e -> e.relation() + " " + e.name())
				.containsExactly("primary actor Shopper", "actor Clerk");
	}

	/** #263: a primary actor is not one of the container's actors, so it has no referer. */
	@Test
	void actorReferencesIncludeUseCasesWhereItIsOnlyThePrimaryActor() {
		Actor actor = entity(Actor.class, 5L, "Treasurer", "approves purchases");
		when(actor.getProjectOrDomain()).thenReturn(project);
		when(actor.getReferers()).thenReturn(new LinkedHashSet<>());
		when(actor.getGoals()).thenReturn(Set.of());
		UseCase useCase = entity(UseCase.class, 1L, "Approve a purchase", "approve");
		when(useCase.getPrimaryActor()).thenReturn(actor);
		UseCase other = entity(UseCase.class, 2L, "Check out", "buy");
		when(project.getProjectEntities()).thenReturn(new LinkedHashSet<>(List.of(actor, useCase,
				other)));

		assertThat(section(actor, "actor-references").entities())
				.extracting(e -> e.relation() + " " + e.name())
				.containsExactly("primary actor of use case Approve a purchase");
	}

	@Test
	void actorReferencesListUseCasesStoriesAndGoals() {
		Actor actor = entity(Actor.class, 5L, "Shopper", "buys");
		UseCase useCase = entity(UseCase.class, 1L, "Check out", "buy");
		when(useCase.getPrimaryActor()).thenReturn(actor);
		Story story = entity(Story.class, 2L, "Browsing", "look");
		Goal goal = goal(3L, "Find things", "f");
		LinkedHashSet<ActorContainer> referers = new LinkedHashSet<>(List.of(story, useCase));
		when(actor.getReferers()).thenReturn(referers);
		when(actor.getGoals()).thenReturn(Set.of(goal));

		assertThat(section(actor, "actor-references").entities())
				.extracting(e -> e.relation() + " " + e.name())
				.containsExactly("primary actor of use case Check out",
						"actor in story Browsing", "actor's goal Find things");
	}

	@Test
	void glossaryRelatedListsCanonicalAlternatesThenSimilarTerms() {
		com.rreganjr.requel.project.GlossaryTerm loan = entity(
				com.rreganjr.requel.project.GlossaryTerm.class, 1L, "Loan", "A tool a member has borrowed");
		com.rreganjr.requel.project.GlossaryTerm canonical = entity(
				com.rreganjr.requel.project.GlossaryTerm.class, 2L, "Borrowing", "Taking a tool home");
		com.rreganjr.requel.project.GlossaryTerm alternate = entity(
				com.rreganjr.requel.project.GlossaryTerm.class, 3L, "Lending", "Same as loan");
		com.rreganjr.requel.project.GlossaryTerm similar = entity(
				com.rreganjr.requel.project.GlossaryTerm.class, 4L, "Overdue tool",
				"A borrowed tool a member has kept too long");
		com.rreganjr.requel.project.GlossaryTerm unrelated = entity(
				com.rreganjr.requel.project.GlossaryTerm.class, 5L, "Opening hours", "When we are open");
		when(loan.getCanonicalTerm()).thenReturn(canonical);
		when(loan.getAlternateTerms()).thenReturn(Set.of(alternate));
		when(loan.getProjectOrDomain()).thenReturn(project);
		java.util.SortedSet<com.rreganjr.requel.project.GlossaryTerm> terms = new java.util.TreeSet<>(
				java.util.Comparator.comparing(com.rreganjr.requel.project.GlossaryTerm::getId));
		terms.addAll(List.of(loan, canonical, alternate, similar, unrelated));
		when(project.getGlossaryTerms()).thenReturn(terms);

		assertThat(section(loan, "glossary-related").entities())
				.extracting(e -> e.relation() + " " + e.name())
				.containsExactly("canonical term Borrowing", "alternate name Lending",
						"other term Overdue tool", "other term Opening hours");
	}

	/** #263: actor and glossary names (no text) so extraction knows what the project has. */
	@Test
	void projectNamesListsActorsThenGlossaryTermsByNameWithoutText() {
		Goal target = goal(1L, "Members love the library", "enjoy it");
		Actor member = entity(Actor.class, 2L, "Member", "Someone with a current membership");
		Actor treasurer = entity(Actor.class, 3L, "Dana Whitfield", "Treasurer");
		com.rreganjr.requel.project.GlossaryTerm loan = entity(
				com.rreganjr.requel.project.GlossaryTerm.class, 4L, "Loan", "A tool a member has");
		when(project.getProjectEntities()).thenReturn(new LinkedHashSet<>(List.of(target, member,
				treasurer, loan)));
		java.util.SortedSet<com.rreganjr.requel.project.GlossaryTerm> terms = new java.util.TreeSet<>(
				java.util.Comparator.comparing(com.rreganjr.requel.project.GlossaryTerm::getId));
		terms.add(loan);
		when(project.getGlossaryTerms()).thenReturn(terms);

		ContextSection section = section(target, "project-names");
		assertThat(section.entities()).extracting(e -> e.relation() + " " + e.name())
				.containsExactly("project actor Dana Whitfield", "project actor Member",
						"glossary term Loan");
		assertThat(section.entities()).extracting(RelatedEntity::text).containsOnlyNulls();
	}

	// ---- fixtures ------------------------------------------------------------------------

	private ContextSection section(Object target, String providerId) {
		EntityContextPack pack = builder.build(target, spec(providerId));
		assertThat(pack.context()).extracting(ContextSection::providerId).containsExactly(providerId);
		return pack.context().get(0);
	}

	private static PackSpec spec(String... providers) {
		List<String> ids = new ArrayList<>(List.of("entity"));
		ids.addAll(List.of(providers));
		return new PackSpec(ids, Map.of(), 0);
	}

	private Goal goal(Long id, String name, String text) {
		Goal goal = entity(Goal.class, id, name, text);
		when(goal.getProjectOrDomain()).thenReturn(project);
		when(goal.getRelationsFromThisGoal()).thenReturn(new LinkedHashSet<>());
		when(goal.getRelationsToThisGoal()).thenReturn(new LinkedHashSet<>());
		when(goal.getReferers()).thenReturn(new LinkedHashSet<>());
		return goal;
	}

	@SuppressWarnings("unchecked")
	private static <T extends com.rreganjr.requel.project.TextEntity> T entity(Class<T> type,
			Long id, String name, String text) {
		T entity = mock(type);
		when(entity.getId()).thenReturn(id);
		when(entity.getName()).thenReturn(name);
		when(entity.getText()).thenReturn(text);
		doReturn(type).when(entity).getProjectOrDomainEntityInterface();
		return entity;
	}

	private NonUserStakeholder nonUser(Long id, String name, String text) {
		NonUserStakeholder stakeholder = mock(NonUserStakeholder.class);
		when(stakeholder.getId()).thenReturn(id);
		when(stakeholder.getName()).thenReturn(name);
		when(stakeholder.getText()).thenReturn(text);
		doReturn(NonUserStakeholder.class).when(stakeholder).getProjectOrDomainEntityInterface();
		return stakeholder;
	}

	private static void hold(com.rreganjr.requel.project.Stakeholder holder, Goal... goals) {
		when(holder.getGoals()).thenReturn(new LinkedHashSet<>(List.of(goals)));
		for (Goal goal : goals) {
			((Set<GoalContainer>) goal.getReferers()).add(holder);
		}
	}

	private static void relate(Goal from, Goal to, GoalRelationType type) {
		GoalRelation relation = mock(GoalRelation.class);
		when(relation.getFromGoal()).thenReturn(from);
		when(relation.getToGoal()).thenReturn(to);
		when(relation.getRelationType()).thenReturn(type);
		from.getRelationsFromThisGoal().add(relation);
		to.getRelationsToThisGoal().add(relation);
	}
}
