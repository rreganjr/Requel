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
package com.rreganjr.requel.assistant.core.definition;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.core.context.ContextProviderRegistry;
import com.rreganjr.requel.assistant.core.persistence.AssistantDefinitionEntity;
import com.rreganjr.requel.assistant.core.persistence.AssistantDefinitionRepository;
import com.rreganjr.requel.project.ProjectAssistantDefinitions;

/**
 * Issue #260: where assistant definitions are read and written. Reads are cached per project
 * (bundled plus the project's own, one query to fill), and every write evicts, so a bulk review
 * reads the definitions once and an edit takes effect on the next run.
 *
 * <p>A project definition with a bundled definition's key overrides it for that project.
 */
@Component
public class AssistantDefinitionStore implements ProjectAssistantDefinitions {

	private static final Logger log = LoggerFactory.getLogger(AssistantDefinitionStore.class);

	/** Cache key for "no project": the bundled definitions alone. */
	private static final long NO_PROJECT = -1L;

	private final AssistantDefinitionRepository repository;
	private final ObjectMapper objectMapper;
	private final AssistantDefinitionValidator validator;
	private final Clock clock;
	private final Map<Long, List<AssistantDefinition>> cache = new ConcurrentHashMap<>();

	@Autowired
	public AssistantDefinitionStore(AssistantDefinitionRepository repository,
			ObjectMapper objectMapper, Environment environment,
			ObjectProvider<ContextProviderRegistry> providers) {
		this(repository, objectMapper, new AssistantDefinitionValidator(maxInputTokens(environment),
				providerIds(providers.getIfAvailable())), Clock.systemUTC());
	}

	/** #261: the registered provider ids, or the built-in set without a registry. */
	private static Set<String> providerIds(ContextProviderRegistry registry) {
		return registry == null ? AssistantDefinitionValidator.CONTEXT_PROVIDERS : registry.ids();
	}

	AssistantDefinitionStore(AssistantDefinitionRepository repository, ObjectMapper objectMapper,
			AssistantDefinitionValidator validator, Clock clock) {
		this.repository = Objects.requireNonNull(repository, "repository");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.validator = Objects.requireNonNull(validator, "validator");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	/** {@code requel.ai.max-input-tokens} (relaxed binding, as {@code AiProperties} reads it). */
	static int maxInputTokens(Environment environment) {
		if (environment == null) {
			return 16000;
		}
		return Binder.get(environment).bind("requel.ai.max-input-tokens", Integer.class)
				.orElse(16000);
	}

	public AssistantDefinitionValidator validator() {
		return validator;
	}

	/**
	 * Every definition {@code projectId} can see - bundled, overridden by its own - that serves
	 * {@code taskType}, enabled or not. Cached.
	 */
	@Transactional(readOnly = true)
	public List<AssistantDefinition> definitionsFor(Long projectId, String taskType) {
		List<AssistantDefinition> matching = new ArrayList<AssistantDefinition>();
		for (AssistantDefinition definition : visibleTo(projectId)) {
			if (Objects.equals(taskType, definition.taskType())) {
				matching.add(definition);
			}
		}
		return List.copyOf(matching);
	}

	/** The bundled definitions (cached), for the assistant toggles. */
	@Transactional(readOnly = true)
	public List<AssistantDefinition> bundled() {
		return visibleTo(null);
	}

	/**
	 * Validate {@code definition} against the definitions it would sit beside, then insert or
	 * replace it (same key and owner) and evict the cache.
	 *
	 * @param by the username making the change, or null for the system
	 * @throws InvalidAssistantDefinitionException when it breaks a rule
	 */
	@Transactional
	public AssistantDefinition save(AssistantDefinition definition, String by) {
		validator.validate(definition, neighbours(definition));
		AssistantDefinitionEntity entity = find(definition.key(), definition.projectId());
		if (entity == null) {
			entity = new AssistantDefinitionEntity(definition.key(), definition.projectId(),
					clock.instant());
			entity.setCreatedBy(by);
		}
		copy(definition, entity);
		entity.setUpdatedBy(by);
		entity.setUpdatedAt(clock.instant());
		repository.save(entity);
		evict(definition.projectId());
		return definition;
	}

	/**
	 * Seed a bundled definition: insert it if missing, replace the stored row when the stored
	 * version is lower, otherwise leave it (an operator's change to the same version stands).
	 * Never touches project rows.
	 *
	 * @return true when the row was inserted or replaced
	 */
	@Transactional
	public boolean seedBundled(AssistantDefinition definition) {
		if (definition.source() != DefinitionSource.BUNDLED || definition.projectId() != null) {
			throw new IllegalArgumentException("only bundled definitions are seeded: "
					+ definition.key());
		}
		AssistantDefinitionEntity entity = find(definition.key(), null);
		if (entity != null && entity.getDefinitionVersion() >= definition.version()) {
			return false;
		}
		if (entity == null) {
			entity = new AssistantDefinitionEntity(definition.key(), null, clock.instant());
		} else {
			log.info("Upgrading bundled assistant definition {} from version {} to {}",
					definition.key(), entity.getDefinitionVersion(), definition.version());
		}
		copy(definition, entity);
		entity.setUpdatedAt(clock.instant());
		repository.save(entity);
		evict(null);
		return true;
	}

	/** Delete the project's own definitions (bundled ones are untouched) and evict its reads. */
	@Override
	@Transactional
	public int deleteForProject(Long projectId) {
		if (projectId == null) {
			return 0;
		}
		int deleted = repository.deleteByProjectId(projectId);
		evict(projectId);
		return deleted;
	}

	/** Drop every cached read. */
	public void evictAll() {
		cache.clear();
	}

	private void evict(Long projectId) {
		if (projectId == null) {
			cache.clear();
		} else {
			cache.remove(projectId);
		}
	}

	private List<AssistantDefinition> visibleTo(Long projectId) {
		long cacheKey = projectId == null ? NO_PROJECT : projectId;
		return cache.computeIfAbsent(cacheKey, k -> load(projectId));
	}

	private List<AssistantDefinition> load(Long projectId) {
		List<AssistantDefinitionEntity> rows = projectId == null ? repository.findByProjectIdIsNull()
				: repository.findVisibleTo(projectId);
		Map<String, AssistantDefinition> byKey = new LinkedHashMap<String, AssistantDefinition>();
		for (AssistantDefinitionEntity row : rows) {
			AssistantDefinition definition = toDefinition(row);
			AssistantDefinition existing = byKey.get(definition.key());
			// A project's own definition overrides the bundled one with the same key.
			if (existing == null || definition.projectId() != null) {
				byKey.put(definition.key(), definition);
			}
		}
		return List.copyOf(byKey.values());
	}

	private Collection<AssistantDefinition> neighbours(AssistantDefinition definition) {
		return visibleTo(definition.projectId());
	}

	private AssistantDefinitionEntity find(String key, Long projectId) {
		return (projectId == null ? repository.findByDefinitionKeyAndProjectIdIsNull(key)
				: repository.findByDefinitionKeyAndProjectId(key, projectId)).orElse(null);
	}

	private void copy(AssistantDefinition definition, AssistantDefinitionEntity entity) {
		entity.setDisplayName(definition.displayName());
		entity.setKind(definition.kind().name());
		entity.setTaskType(definition.taskType());
		entity.setScopeJson(json(new java.util.TreeSet<String>(definition.scope())));
		entity.setContextProvidersJson(json(definition.contextProviders()));
		entity.setContextBudgetsJson(definition.contextBudgets().isEmpty() ? null
				: json(new java.util.TreeMap<String, Integer>(definition.contextBudgets())));
		entity.setInstructions(definition.instructions());
		entity.setVocabularyJson(json(definition.vocabulary()));
		entity.setOutputSchemaName(definition.outputSchemaName());
		entity.setOutputSchemaVersion(definition.outputSchemaVersion());
		entity.setEnabled(definition.enabled());
		entity.setDefinitionVersion(definition.version());
		entity.setSource(definition.source().name());
		entity.setForkedFromVersion(definition.forkedFromVersion());
		entity.setExecutorBean(definition.executorBean());
	}

	AssistantDefinition toDefinition(AssistantDefinitionEntity row) {
		return new AssistantDefinition(row.getDefinitionKey(), row.getDisplayName(),
				DefinitionKind.valueOf(row.getKind()), row.getTaskType(),
				read(row.getScopeJson(), new TypeReference<Set<String>>() {
				}), read(row.getContextProvidersJson(), new TypeReference<List<String>>() {
				}), row.getInstructions(),
				read(row.getVocabularyJson(), new TypeReference<List<VocabularyEntry>>() {
				}), row.getOutputSchemaName(), row.getOutputSchemaVersion(), row.isEnabled(),
				row.getDefinitionVersion(), DefinitionSource.valueOf(row.getSource()),
				row.getProjectId(), row.getForkedFromVersion(), row.getExecutorBean(),
				row.getContextBudgetsJson() == null ? Map.of()
						: read(row.getContextBudgetsJson(), new TypeReference<Map<String, Integer>>() {
						}));
	}

	private String json(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("could not write assistant definition JSON", e);
		}
	}

	private <T> T read(String json, TypeReference<T> type) {
		try {
			return objectMapper.readValue(json == null || json.isBlank() ? "[]" : json, type);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("could not read assistant definition JSON: " + json, e);
		}
	}
}
