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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantRegistry;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionStore;
import com.rreganjr.requel.assistant.core.definition.DefinitionExecutorFactory;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.SwitchableAssistantCatalog;

/**
 * Registry implementation that matches assistants by their declared target type. Issue #268: an
 * assistant a project has switched off ({@link ProjectAssistantSettingsStore}) is left out of
 * that project's runs; it is also the {@link SwitchableAssistantCatalog} of the assistants a
 * project can switch.
 *
 * <p>Issue #260: alongside the bean assistants it returns an executor for each matching
 * {@link AssistantDefinition}, resolved per run from the run's project and task type. Per task
 * type a definition specific to the target's entity type wins; the empty-scope definition is the
 * fallback when none is. A definition switched off install-wide ({@code enabled}) or for the
 * project (#268's store, keyed by definition key) is left out first. A definition naming an
 * {@code executorBean} runs that bean.
 */
@Component
public class SimpleAssistantRegistry implements AssistantRegistry, SwitchableAssistantCatalog,
		BeanFactoryAware {

	private static final Logger log = LoggerFactory.getLogger(SimpleAssistantRegistry.class);

	private final List<RequelAssistant<?>> assistants;
	private ProjectAssistantSettingsStore settingsStore;
	private AssistantDefinitionStore definitionStore;
	private DefinitionExecutorFactory executorFactory;
	private BeanFactory beanFactory;

	@Autowired
	public SimpleAssistantRegistry(List<RequelAssistant<?>> assistants) {
		this.assistants = List.copyOf(assistants);
	}

	/** Optional: with no store every assistant runs everywhere (unit tests, tools). */
	@Autowired(required = false)
	public void setSettingsStore(ProjectAssistantSettingsStore settingsStore) {
		this.settingsStore = settingsStore;
	}

	/** Issue #260: optional; with no store only the bean assistants run. */
	@Autowired(required = false)
	public void setDefinitionStore(AssistantDefinitionStore definitionStore) {
		this.definitionStore = definitionStore;
	}

	/** Issue #260: optional; present only when AI is enabled. */
	@Autowired(required = false)
	public void setExecutorFactory(DefinitionExecutorFactory executorFactory) {
		this.executorFactory = executorFactory;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public List<RequelAssistant<?>> findAssistantsFor(Object target, AssistantContext context) {
		Objects.requireNonNull(target, "target");
		Set<String> disabled = disabledFor(context);
		List<RequelAssistant<?>> matches = new ArrayList<RequelAssistant<?>>();
		for (RequelAssistant<?> assistant : assistants) {
			if (!assistant.targetType().isInstance(target)) {
				continue;
			}
			if (SwitchableAssistantCatalog.CORPUS.equals(assistant.group())
					&& (context == null || !assistant.handlesTask(context.taskType()))) {
				// #266: a corpus assistant takes any root (a project, a goal, a use case), so it
				// is matched by its task alone and never counts towards an ordinary run
				continue;
			}
			if (assistant.projectSwitchable() && disabled.contains(assistant.assistantId())) {
				log.debug("assistant {} is switched off in {}", assistant.assistantId(),
						context.projectRef());
				continue;
			}
			matches.add(assistant);
		}
		if (context != null && AssistantDefinition.POLICY_REVIEW.equals(context.taskType())) {
			// #265: every applicable policy, composed into one assistant (one provider call).
			List<AssistantDefinition> policies = policiesFor(target, projectId(context), disabled);
			if (!policies.isEmpty()) {
				matches.add(executorFactory.composedExecutorFor(policies));
			}
			return List.copyOf(matches);
		}
		for (AssistantDefinition definition : definitionsFor(target, context, disabled)) {
			matches.add(executorFor(definition));
		}
		return List.copyOf(matches);
	}

	/**
	 * Issue #265: the enabled policies that apply to {@code target} in {@code projectId}, after
	 * the project's switches, ordered by key. Empty when policies can't run (no AI).
	 */
	public List<AssistantDefinition> policiesFor(Object target, Long projectId) {
		Set<String> disabled = settingsStore == null || projectId == null ? Set.of()
				: settingsStore.disabledAssistants(projectId);
		return policiesFor(target, projectId, disabled);
	}

	private List<AssistantDefinition> policiesFor(Object target, Long projectId,
			Set<String> disabled) {
		if (definitionStore == null || executorFactory == null
				|| !(target instanceof ProjectOrDomainEntity entity)) {
			return List.of();
		}
		String entityType = entity.getProjectOrDomainEntityInterface().getSimpleName();
		List<AssistantDefinition> policies = new ArrayList<AssistantDefinition>();
		for (AssistantDefinition definition : definitionStore.definitionsFor(projectId,
				AssistantDefinition.POLICY_REVIEW)) {
			if (definition.isPolicy() && definition.appliesTo(entityType) && definition.enabled()
					&& !disabled.contains(definition.key())) {
				policies.add(definition);
			}
		}
		policies.sort(java.util.Comparator.comparing(AssistantDefinition::key));
		return List.copyOf(policies);
	}

	/**
	 * Issue #260: the definitions that run on {@code target} for this run - specific to its entity
	 * type if any, else the fallback - after the install-wide and project switches.
	 */
	List<AssistantDefinition> definitionsFor(Object target, AssistantContext context,
			Set<String> disabled) {
		if (definitionStore == null || executorFactory == null || context == null
				|| context.taskType() == null) {
			return List.of();
		}
		// #266: a corpus definition's scope names the set kind its root makes
		String entityType = scopeKey(target, context.taskType());
		if (entityType == null) {
			return List.of();
		}
		// #263: coverage is decided before the switches. A type with its own definition is never
		// reviewed by the fallback, so switching that definition off stops its reviews rather
		// than handing them to the generic one.
		List<AssistantDefinition> specific = new ArrayList<AssistantDefinition>();
		List<AssistantDefinition> fallback = new ArrayList<AssistantDefinition>();
		for (AssistantDefinition definition : definitionStore.definitionsFor(projectId(context),
				context.taskType())) {
			if (definition.isSpecificTo(entityType)) {
				specific.add(definition);
			} else if (definition.isFallback()) {
				fallback.add(definition);
			}
		}
		List<AssistantDefinition> chosen = new ArrayList<AssistantDefinition>();
		for (AssistantDefinition definition : specific.isEmpty() ? fallback : specific) {
			if (definition.enabled() && !disabled.contains(definition.key())) {
				chosen.add(definition);
			}
		}
		return List.copyOf(chosen);
	}

	/** What a definition's scope names for {@code target}: its entity type, or its set kind. */
	private static String scopeKey(Object target, String taskType) {
		if (AssistantDefinition.CORPUS_REVIEW.equals(taskType)) {
			com.rreganjr.requel.assistant.core.corpus.CorpusMembers.SetKind kind =
					com.rreganjr.requel.assistant.core.corpus.CorpusMembers.SetKind.of(target);
			return kind == null ? null : kind.name();
		}
		return target instanceof ProjectOrDomainEntity entity
				? entity.getProjectOrDomainEntityInterface().getSimpleName() : null;
	}

	private RequelAssistant<?> executorFor(AssistantDefinition definition) {
		if (definition.executorBean() != null && !definition.executorBean().isBlank()) {
			if (beanFactory == null) {
				throw new IllegalStateException("definition " + definition.key()
						+ " names executorBean " + definition.executorBean()
						+ " but no bean factory is available");
			}
			return beanFactory.getBean(definition.executorBean(), RequelAssistant.class);
		}
		return executorFactory.executorFor(definition);
	}

	private static Long projectId(AssistantContext context) {
		if (context.projectRef() == null || !"Project".equals(context.projectRef().entityType())) {
			return null;
		}
		return context.projectRef().entityId();
	}

	@Override
	public java.util.Optional<SwitchableAssistant> describe(String assistantId) {
		for (RequelAssistant<?> assistant : assistants) {
			if (assistant.assistantId().equals(assistantId)) {
				return java.util.Optional.of(describe(assistant));
			}
		}
		return find(assistantId);
	}

	private static SwitchableAssistant describe(RequelAssistant<?> assistant) {
		return new SwitchableAssistant(assistant.assistantId(), assistant.displayName(),
				assistant.group() == null ? SwitchableAssistantCatalog.LEXICAL_CHECKS
						: assistant.group());
	}

	@Override
	public List<SwitchableAssistant> switchableAssistants() {
		return switchableAssistants(null);
	}

	/**
	 * The switchable bean assistants, then the enabled definitions when they can run (#260): the
	 * bundled ones, or with a project, the ones it sees - its forks under their own names and its
	 * own new ones (#264).
	 */
	@Override
	public List<SwitchableAssistant> switchableAssistants(Long projectId) {
		List<SwitchableAssistant> switchable = new ArrayList<>();
		for (RequelAssistant<?> assistant : assistants) {
			if (assistant.projectSwitchable()) {
				switchable.add(describe(assistant));
			}
		}
		if (definitionStore != null && executorFactory != null) {
			List<AssistantDefinition> definitions = projectId == null ? definitionStore.bundled()
					: definitionStore.visible(projectId);
			for (AssistantDefinition definition : definitions) {
				if (definition.enabled()) {
					switchable.add(new SwitchableAssistant(definition.key(),
							definition.displayName(), definition.isPolicy()
									? SwitchableAssistantCatalog.POLICIES
									: definition.isCorpus() ? SwitchableAssistantCatalog.CORPUS
											: SwitchableAssistantCatalog.AI_REVIEW));
				}
			}
		}
		return List.copyOf(switchable);
	}

	private Set<String> disabledFor(AssistantContext context) {
		if (settingsStore == null || context == null || context.projectRef() == null
				|| !"Project".equals(context.projectRef().entityType())) {
			return Set.of();
		}
		return settingsStore.disabledAssistants(context.projectRef().entityId());
	}
}
