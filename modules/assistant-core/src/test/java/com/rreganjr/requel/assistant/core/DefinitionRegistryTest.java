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
package com.rreganjr.requel.assistant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.corpus.CorpusFinderAssistant;
import com.rreganjr.requel.assistant.core.corpus.WordRelations;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionStore;
import com.rreganjr.requel.assistant.core.definition.DefinitionBacked;
import com.rreganjr.requel.assistant.core.definition.DefinitionExecutorFactory;
import com.rreganjr.requel.assistant.core.definition.DefinitionKind;
import com.rreganjr.requel.assistant.core.definition.DefinitionSource;
import com.rreganjr.requel.assistant.core.definition.VocabularyEntry;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;

/**
 * #260: the registry resolves definitions per run - specific beats the fallback, the
 * install-wide and project switches apply first, an executorBean runs that bean - and returns
 * them beside the bean assistants.
 */
class DefinitionRegistryTest {

	private static final String TASK = "REQUIREMENTS_REVIEW";

	private final AssistantDefinitionStore store = mock(AssistantDefinitionStore.class);
	private final DefinitionExecutorFactory factory = definition -> new Executor(definition);
	private SimpleAssistantRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new SimpleAssistantRegistry(List.of());
		registry.setDefinitionStore(store);
		registry.setExecutorFactory(factory);
	}

	@Test
	void theFallbackRunsWhenNoDefinitionIsSpecific() {
		when(store.definitionsFor(1L, TASK)).thenReturn(List.of(definition("default", Set.of()),
				definition("stories", Set.of("Story"))));

		assertThat(keys(registry.findAssistantsFor(goal(), context(TASK)))).containsExactly(
				"default");
	}

	@Test
	void aSpecificDefinitionReplacesTheFallback() {
		when(store.definitionsFor(1L, TASK)).thenReturn(List.of(definition("default", Set.of()),
				definition("goals", Set.of("Goal"))));

		assertThat(keys(registry.findAssistantsFor(goal(), context(TASK)))).containsExactly(
				"goals");
		assertThat(keys(registry.findAssistantsFor(story(), context(TASK)))).containsExactly(
				"default");
	}

	@Test
	void aSwitchedOffDefinitionStopsItsTypesReviewsRatherThanHandingThemToTheFallback() {
		// #263: coverage comes first; the switch only decides whether the covering one runs.
		AssistantDefinition goals = definition("goals", Set.of("Goal"));
		when(store.definitionsFor(1L, TASK)).thenReturn(List.of(definition("default", Set.of()),
				goals.withEnabled(false)));
		assertThat(registry.findAssistantsFor(goal(), context(TASK))).isEmpty();
		assertThat(keys(registry.findAssistantsFor(story(), context(TASK)))).containsExactly(
				"default");

		when(store.definitionsFor(1L, TASK)).thenReturn(List.of(definition("default", Set.of()),
				goals));
		registry.setSettingsStore(disabling("goals"));
		assertThat(registry.findAssistantsFor(goal(), context(TASK))).isEmpty();
		registry.setSettingsStore(disabling());

		when(store.definitionsFor(1L, TASK)).thenReturn(List.of(definition("default", Set.of())));
		registry.setSettingsStore(disabling("default"));
		assertThat(registry.findAssistantsFor(goal(), context(TASK))).isEmpty();
	}

	@Test
	void noTaskOrNoFactoryMeansNoDefinitions() {
		when(store.definitionsFor(1L, TASK)).thenReturn(List.of(definition("default", Set.of())));
		assertThat(registry.findAssistantsFor(goal(), context(null))).isEmpty();
		registry.setExecutorFactory(null);
		assertThat(registry.findAssistantsFor(goal(), context(TASK))).isEmpty();
	}

	@Test
	void anExecutorBeanRunsThatBean() {
		AssistantDefinition d = definition("custom", Set.of());
		AssistantDefinition withBean = new AssistantDefinition(d.key(), d.displayName(), d.kind(),
				d.taskType(), d.scope(), d.contextProviders(), d.instructions(), d.vocabulary(),
				d.outputSchemaName(), d.outputSchemaVersion(), true, 1, DefinitionSource.BUNDLED,
				null, null, "customReviewer");
		when(store.definitionsFor(1L, TASK)).thenReturn(List.of(withBean));
		BeanFactory beans = mock(BeanFactory.class);
		Executor bean = new Executor(withBean);
		when(beans.getBean("customReviewer", RequelAssistant.class)).thenReturn(bean);
		registry.setBeanFactory(beans);

		assertThat(registry.findAssistantsFor(goal(), context(TASK))).containsExactly(bean);
	}

	@Test
	void enabledBundledDefinitionsAreSwitchable() {
		when(store.bundled()).thenReturn(List.of(definition("default", Set.of()),
				definition("off", Set.of("Goal")).withEnabled(false)));

		assertThat(registry.switchableAssistants()).extracting(s -> s.assistantId())
				.containsExactly("default");
	}

	/**
	 * #265: every applicable, enabled, switched-on policy goes into one composed assistant,
	 * ordered by key; a policy scoped elsewhere doesn't; no policy, no assistant.
	 */
	@Test
	void applicablePoliciesAreComposedIntoOneAssistant() {
		List<List<String>> composed = new java.util.ArrayList<>();
		registry.setExecutorFactory(new DefinitionExecutorFactory() {
			@Override
			public RequelAssistant<?> executorFor(AssistantDefinition definition) {
				return new Executor(definition);
			}

			@Override
			public RequelAssistant<?> composedExecutorFor(List<AssistantDefinition> policies) {
				composed.add(policies.stream().map(AssistantDefinition::key).toList());
				return new Executor(policies.get(0));
			}
		});
		when(store.definitionsFor(1L, AssistantDefinition.POLICY_REVIEW)).thenReturn(List.of(
				policy("p-zeta", Set.of()), policy("p-alpha", Set.of("Goal")),
				policy("p-stories", Set.of("Story")), policy("p-off", Set.of()).withEnabled(false),
				policy("p-switched", Set.of())));
		registry.setSettingsStore(disabling("p-switched"));

		assertThat(registry.findAssistantsFor(goal(), context(AssistantDefinition.POLICY_REVIEW)))
				.hasSize(1);
		assertThat(composed).containsExactly(List.of("p-alpha", "p-zeta"));
		assertThat(registry.policiesFor(story(), 1L)).extracting(AssistantDefinition::key)
				.containsExactly("p-stories", "p-zeta");
		// The review task never sees policies, and the policy task never sees reviews.
		when(store.definitionsFor(1L, TASK)).thenReturn(List.of(definition("default", Set.of())));
		assertThat(keys(registry.findAssistantsFor(goal(), context(TASK)))).containsExactly(
				"default");
		when(store.definitionsFor(1L, AssistantDefinition.POLICY_REVIEW)).thenReturn(List.of());
		assertThat(registry.findAssistantsFor(goal(), context(AssistantDefinition.POLICY_REVIEW)))
				.isEmpty();
	}

	@Test
	void enabledPoliciesAreSwitchableUnderTheirOwnGroup() {
		when(store.bundled()).thenReturn(List.of(definition("default", Set.of()),
				policy("p", Set.of())));

		assertThat(registry.switchableAssistants()).extracting(s -> s.assistantId() + ":"
				+ s.group()).containsExactly("default:AI review", "p:Policies");
	}

	/**
	 * #266: a corpus definition's scope names set kinds. A project root makes a PROJECT set, a goal
	 * a GOAL set; anything else roots no set, so no corpus definition runs on it.
	 */
	@Test
	void aCorpusDefinitionIsChosenByTheSetKindOfItsRoot() {
		when(store.definitionsFor(1L, AssistantDefinition.CORPUS_REVIEW)).thenReturn(List.of(
				corpus("c-any", Set.of()), corpus("c-project", Set.of("PROJECT"))));
		AssistantContext context = context(AssistantDefinition.CORPUS_REVIEW);

		assertThat(keys(registry.findAssistantsFor(mock(Project.class), context)))
				.containsExactly("c-project");
		assertThat(keys(registry.findAssistantsFor(goal(), context))).containsExactly("c-any");
		assertThat(registry.findAssistantsFor(story(), context)).isEmpty();
	}

	/**
	 * #266: a corpus-group bean takes any target, so it runs only for a task it handles, never in
	 * an ordinary run.
	 */
	@Test
	void aCorpusBeanRunsOnlyForItsTask() {
		CorpusFinderAssistant finder = finder();
		SimpleAssistantRegistry beans = new SimpleAssistantRegistry(List.of(finder));

		assertThat(beans.findAssistantsFor(goal(), context(CorpusFinderAssistant.TASK_TYPE)))
				.containsExactly(finder);
		assertThat(beans.findAssistantsFor(goal(), context(TASK))).isEmpty();
		assertThat(beans.findAssistantsFor(goal(), context(null))).isEmpty();
		assertThat(beans.findAssistantsFor(goal(), null)).isEmpty();
	}

	/** #266: any assistant is described by id, switchable or not, with its group. */
	@Test
	void assistantsAreDescribedById() {
		Executor plain = new Executor(definition("plain", Set.of()));
		SimpleAssistantRegistry beans = new SimpleAssistantRegistry(List.of(finder(), plain));
		beans.setDefinitionStore(store);
		beans.setExecutorFactory(factory);
		when(store.bundled()).thenReturn(List.of(corpus("c-any", Set.of())));

		assertThat(beans.describe(CorpusFinderAssistant.ASSISTANT_ID)).contains(
				new SwitchableAssistantCatalog.SwitchableAssistant("corpus-finder",
						"Find overlaps", SwitchableAssistantCatalog.CORPUS));
		assertThat(beans.describe("plain")).map(SwitchableAssistantCatalog.SwitchableAssistant::group)
				.contains(SwitchableAssistantCatalog.LEXICAL_CHECKS);
		assertThat(beans.describe("c-any")).map(SwitchableAssistantCatalog.SwitchableAssistant::group)
				.contains(SwitchableAssistantCatalog.CORPUS);
		assertThat(beans.describe("nobody")).isEmpty();
	}

	private static CorpusFinderAssistant finder() {
		@SuppressWarnings("unchecked")
		ObjectProvider<WordRelations> none = mock(ObjectProvider.class);
		return new CorpusFinderAssistant(none, 0.35, 0.20, 0.5);
	}

	private static AssistantDefinition corpus(String key, Set<String> scope) {
		return new AssistantDefinition(key, key, DefinitionKind.CORPUS,
				AssistantDefinition.CORPUS_REVIEW, scope, List.of(), "x",
				List.of(new VocabularyEntry("A", "a")), AssistantDefinition.CORPUS_OUTPUT_SCHEMA,
				"1", true, 1, DefinitionSource.BUNDLED, null, null, null);
	}

	private static AssistantDefinition policy(String key, Set<String> scope) {
		return new AssistantDefinition(key, key, DefinitionKind.POLICY,
				AssistantDefinition.POLICY_REVIEW, scope, List.of("entity"), "x",
				List.of(new VocabularyEntry("A", "a")), AssistantDefinition.POLICY_OUTPUT_SCHEMA,
				"1", true, 1, DefinitionSource.BUNDLED, null, null, null);
	}

	private static List<String> keys(List<RequelAssistant<?>> assistants) {
		return assistants.stream().map(RequelAssistant::assistantId).toList();
	}

	private static AssistantDefinition definition(String key, Set<String> scope) {
		return new AssistantDefinition(key, key, DefinitionKind.REVIEW, TASK, scope,
				List.of("entity"), "x", List.of(new VocabularyEntry("A", "a")),
				"RequirementsReviewOutput", "1", true, 1, DefinitionSource.BUNDLED, null, null,
				null);
	}

	private static Goal goal() {
		Goal goal = mock(Goal.class);
		doReturn(Goal.class).when(goal).getProjectOrDomainEntityInterface();
		return goal;
	}

	private static Story story() {
		Story story = mock(Story.class);
		doReturn(Story.class).when(story).getProjectOrDomainEntityInterface();
		return story;
	}

	private static AssistantContext context(String taskType) {
		return new AssistantContext(UUID.randomUUID(), new UserRef(2L, "human"),
				new UserRef(3L, "assistant"), EntityRef.of("Project", 1L), taskType,
				java.util.Locale.US, Clock.systemUTC(), Map.of());
	}

	private static ProjectAssistantSettingsStore disabling(String... keys) {
		return new ProjectAssistantSettingsStore() {
			@Override
			public Set<String> disabledAssistants(Long id) {
				return Set.of(keys);
			}

			@Override
			public void setEnabled(Long id, String assistant, boolean enabled,
					com.rreganjr.platform.identity.User by) {
			}

			@Override
			public int deleteForProject(Long id) {
				return 0;
			}
		};
	}

	private static final class Executor implements RequelAssistant<Object>, DefinitionBacked {
		private final AssistantDefinition definition;

		Executor(AssistantDefinition definition) {
			this.definition = definition;
		}

		@Override
		public AssistantDefinition definition() {
			return definition;
		}

		@Override
		public String assistantId() {
			return definition.key();
		}

		@Override
		public Class<Object> targetType() {
			return Object.class;
		}

		@Override
		public AssistantResult analyze(AssistantContext context, Object target) {
			return AssistantResult.builder().assistantId(assistantId()).build();
		}
	}
}
