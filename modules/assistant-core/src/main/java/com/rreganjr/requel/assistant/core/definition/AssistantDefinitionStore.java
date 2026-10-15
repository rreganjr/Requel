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
import com.rreganjr.requel.project.InvalidDefinitionException.Problem;
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

	/** Issue #264: the most definitions one project may own. */
	static final int DEFAULT_MAX_PER_PROJECT = 50;

	private int maxPerProject = DEFAULT_MAX_PER_PROJECT;
	private ObjectProvider<com.rreganjr.requel.assistant.api.RequelAssistant<?>> beanAssistants;

	@Autowired
	public AssistantDefinitionStore(AssistantDefinitionRepository repository,
			ObjectMapper objectMapper, Environment environment,
			ObjectProvider<ContextProviderRegistry> providers) {
		this(repository, objectMapper, new AssistantDefinitionValidator(maxInputTokens(environment),
				providerIds(providers.getIfAvailable())), Clock.systemUTC());
		this.maxPerProject = environment == null ? DEFAULT_MAX_PER_PROJECT
				: Binder.get(environment).bind("requel.ai.definitions.max-per-project",
						Integer.class).orElse(DEFAULT_MAX_PER_PROJECT);
	}

	/**
	 * Issue #264: the bean assistants, whose ids a project definition can't take. Read lazily: some
	 * of them are built from this store.
	 */
	@Autowired(required = false)
	public void setBeanAssistants(
			ObjectProvider<com.rreganjr.requel.assistant.api.RequelAssistant<?>> beanAssistants) {
		this.beanAssistants = beanAssistants;
	}

	void setMaxPerProject(int maxPerProject) {
		this.maxPerProject = maxPerProject;
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

	/**
	 * Issue #264: every definition {@code projectId} can see, any task type - bundled, overridden by
	 * its own, then its own new ones. Cached.
	 */
	@Transactional(readOnly = true)
	public List<AssistantDefinition> visible(Long projectId) {
		return visibleTo(projectId);
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
		// #263: a dev override (see overrideBundled) never outlives its directory; the shipped
		// file replaces it on the next start without the override.
		boolean devOverride = entity != null && DEV_OVERRIDE.equals(entity.getUpdatedBy());
		if (entity != null && !devOverride && entity.getDefinitionVersion() >= definition.version()) {
			return false;
		}
		if (entity == null) {
			entity = new AssistantDefinitionEntity(definition.key(), null, clock.instant());
		} else if (devOverride) {
			log.info("Replacing the dev override of bundled assistant definition {} with the"
					+ " shipped version {}", definition.key(), definition.version());
		} else {
			log.info("Upgrading bundled assistant definition {} from version {} to {}",
					definition.key(), entity.getDefinitionVersion(), definition.version());
		}
		copy(definition, entity);
		entity.setUpdatedBy(null);
		entity.setUpdatedAt(clock.instant());
		repository.save(entity);
		evict(null);
		return true;
	}

	/** {@code updated_by} of a bundled row written by {@link #overrideBundled}. */
	static final String DEV_OVERRIDE = "dev-override";

	/**
	 * Issue #263, dev only: replace the bundled row for {@code definition}'s key whatever the
	 * versions, so a definition can be tuned without a rebuild. The row is marked, and the next
	 * start without the override directory reseeds the shipped file over it.
	 */
	@Transactional
	public void overrideBundled(AssistantDefinition definition) {
		if (definition.source() != DefinitionSource.BUNDLED || definition.projectId() != null) {
			throw new IllegalArgumentException("only bundled definitions are overridden: "
					+ definition.key());
		}
		AssistantDefinitionEntity entity = find(definition.key(), null);
		if (entity == null) {
			entity = new AssistantDefinitionEntity(definition.key(), null, clock.instant());
		}
		copy(definition, entity);
		entity.setUpdatedBy(DEV_OVERRIDE);
		entity.setUpdatedAt(clock.instant());
		repository.save(entity);
		evict(null);
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

	// ---- #264: project authoring (ProjectAssistantDefinitions) --------------------------------

	@Override
	@Transactional(readOnly = true)
	public List<ProjectAssistantDefinitions.View> effective(Long projectId) {
		Objects.requireNonNull(projectId, "projectId");
		Map<String, AssistantDefinitionEntity> rows = new LinkedHashMap<>();
		Map<String, AssistantDefinition> bundledByKey = new LinkedHashMap<>();
		for (AssistantDefinitionEntity row : repository.findVisibleTo(projectId)) {
			if (row.getProjectId() == null) {
				bundledByKey.put(row.getDefinitionKey(), toDefinition(row));
				rows.putIfAbsent(row.getDefinitionKey(), row);
			} else {
				rows.put(row.getDefinitionKey(), row);
			}
		}
		List<AssistantDefinition> effective = new ArrayList<>();
		for (AssistantDefinitionEntity row : rows.values()) {
			effective.add(toDefinition(row));
		}
		List<ProjectAssistantDefinitions.View> views = new ArrayList<>();
		for (AssistantDefinitionEntity row : rows.values()) {
			AssistantDefinition definition = toDefinition(row);
			views.add(view(definition, bundledByKey.get(definition.key()), row.getLockVersion(),
					inEffectFor(definition, effective)));
		}
		views.sort(java.util.Comparator.comparing(ProjectAssistantDefinitions.View::taskType)
				.thenComparing(ProjectAssistantDefinitions.View::key));
		return List.copyOf(views);
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<ProjectAssistantDefinitions.View> find(Long projectId, String key) {
		return effective(projectId).stream().filter(v -> v.key().equals(key)).findFirst();
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<ProjectAssistantDefinitions.View> bundled(String key) {
		AssistantDefinitionEntity row = find(key, null);
		if (row == null) {
			return java.util.Optional.empty();
		}
		AssistantDefinition definition = toDefinition(row);
		return java.util.Optional.of(view(definition, definition, 0, List.of()));
	}

	@Override
	@Transactional
	public ProjectAssistantDefinitions.View create(Long projectId,
			ProjectAssistantDefinitions.Draft draft, String by) {
		Objects.requireNonNull(projectId, "projectId");
		List<Problem> problems = new ArrayList<>();
		DefinitionKind kind = kind(draft.kind(), problems);
		String key = draft.key() == null ? null : draft.key().strip();
		AssistantDefinitionEntity bundledRow = key == null ? null : find(key, null);
		if (bundledRow != null) {
			problems.add(new Problem("key", "key " + key + " is a bundled definition's; customize"
					+ " (fork) it instead"));
		} else if (key != null && find(key, projectId) != null) {
			problems.add(new Problem("key", "the project already has a definition " + key));
		}
		AssistantDefinition definition = fromDraft(key, kind, taskType(kind),
				outputSchema(kind), outputSchemaVersion(kind), draft, 1, projectId, null, true);
		problems.addAll(AssistantDefinitionValidator.projectProblems(definition, null, null,
				reservedKeys(), projectCount(projectId), maxPerProject, draft.executorBean()));
		return write(definition, null, problems, by);
	}

	@Override
	@Transactional
	public ProjectAssistantDefinitions.View edit(Long projectId, String key, int lockVersion,
			ProjectAssistantDefinitions.Draft draft, String by) {
		AssistantDefinitionEntity row = projectRow(projectId, key);
		checkLock(row, lockVersion);
		AssistantDefinition stored = toDefinition(row);
		DefinitionKind kind = stored.kind();
		List<Problem> problems = new ArrayList<>();
		if (draft.kind() != null && !draft.kind().isBlank()) {
			kind = kind(draft.kind(), problems);
		}
		AssistantDefinition definition = fromDraft(key, kind == null ? stored.kind() : kind,
				stored.taskType(), stored.outputSchemaName(), stored.outputSchemaVersion(), draft,
				stored.version() + 1, projectId, stored.forkedFromVersion(), stored.enabled());
		problems.addAll(AssistantDefinitionValidator.projectProblems(definition, stored,
				null, reservedKeys(), 0, maxPerProject, draft.executorBean()));
		return write(definition, row, problems, by);
	}

	@Override
	@Transactional
	public ProjectAssistantDefinitions.View fork(Long projectId, String key, String by) {
		Objects.requireNonNull(projectId, "projectId");
		AssistantDefinitionEntity bundledRow = key == null ? null : find(key, null);
		if (bundledRow == null) {
			throw invalid(key, "key", "there is no bundled definition " + key);
		}
		if (find(key, projectId) != null) {
			throw invalid(key, "key", "the project has already customized " + key);
		}
		AssistantDefinition bundled = toDefinition(bundledRow);
		AssistantDefinition definition = new AssistantDefinition(bundled.key(),
				bundled.displayName(), bundled.kind(), bundled.taskType(), bundled.scope(),
				bundled.contextProviders(), bundled.instructions(), bundled.vocabulary(),
				bundled.outputSchemaName(), bundled.outputSchemaVersion(), bundled.enabled(), 1,
				DefinitionSource.PROJECT, projectId, bundled.version(), bundled.executorBean(),
				bundled.contextBudgets(), bundled.localOnly());
		List<Problem> problems = new ArrayList<>(AssistantDefinitionValidator.projectProblems(
				definition, null, bundled, reservedKeys(), projectCount(projectId), maxPerProject,
				null));
		return write(definition, null, problems, by);
	}

	@Override
	@Transactional
	public void revert(Long projectId, String key, int lockVersion) {
		AssistantDefinitionEntity row = projectRow(projectId, key);
		if (find(key, null) == null) {
			throw invalid(key, "key", key + " is the project's own definition; delete it instead");
		}
		checkLock(row, lockVersion);
		repository.delete(row);
		evict(projectId);
	}

	@Override
	@Transactional
	public void delete(Long projectId, String key, int lockVersion) {
		AssistantDefinitionEntity row = projectRow(projectId, key);
		if (find(key, null) != null) {
			throw invalid(key, "key", key + " customizes a bundled definition; revert it instead");
		}
		checkLock(row, lockVersion);
		repository.delete(row);
		evict(projectId);
	}

	private ProjectAssistantDefinitions.View write(AssistantDefinition definition,
			AssistantDefinitionEntity row, List<Problem> problems, String by) {
		validator.validate(definition, neighbours(definition), problems);
		AssistantDefinitionEntity entity = row;
		if (entity == null) {
			entity = new AssistantDefinitionEntity(definition.key(), definition.projectId(),
					clock.instant());
			entity.setCreatedBy(by);
		}
		copy(definition, entity);
		entity.setUpdatedBy(by);
		entity.setUpdatedAt(clock.instant());
		AssistantDefinitionEntity saved = repository.saveAndFlush(entity);
		evict(definition.projectId());
		AssistantDefinitionEntity bundledRow = find(definition.key(), null);
		return view(definition, bundledRow == null ? null : toDefinition(bundledRow),
				saved.getLockVersion(), inEffectFor(definition, visibleTo(definition.projectId())));
	}

	private AssistantDefinitionEntity projectRow(Long projectId, String key) {
		Objects.requireNonNull(projectId, "projectId");
		AssistantDefinitionEntity row = key == null ? null : find(key, projectId);
		if (row == null) {
			throw invalid(key, "key", "the project has no definition " + key
					+ (key != null && find(key, null) != null ? "; customize the bundled one first"
							: ""));
		}
		return row;
	}

	private static void checkLock(AssistantDefinitionEntity row, int lockVersion) {
		if (row.getLockVersion() != lockVersion) {
			throw com.rreganjr.platform.exception.EntityLockException.staleEntity(
					com.rreganjr.requel.project.AssistantDefinition.class, null,
					com.rreganjr.platform.exception.EntityExceptionActionType.Updating);
		}
	}

	private static InvalidAssistantDefinitionException invalid(String key, String field,
			String message) {
		return new InvalidAssistantDefinitionException(key, List.of(new Problem(field, message)));
	}

	private int projectCount(Long projectId) {
		return (int) repository.countByProjectId(projectId);
	}

	private Set<String> reservedKeys() {
		Set<String> keys = new java.util.HashSet<>();
		if (beanAssistants != null) {
			beanAssistants.forEach(assistant -> keys.add(assistant.assistantId()));
		}
		return keys;
	}

	private static DefinitionKind kind(String value, List<Problem> problems) {
		try {
			return DefinitionKind.valueOf(value == null ? "" : value.strip().toUpperCase(
					java.util.Locale.ROOT));
		} catch (IllegalArgumentException e) {
			problems.add(new Problem("kind", "kind must be one of REVIEW, POLICY, CORPUS"));
			return null;
		}
	}

	/** The task type a new definition of {@code kind} serves. */
	static String taskType(DefinitionKind kind) {
		if (kind == DefinitionKind.POLICY) {
			return AssistantDefinition.POLICY_REVIEW;
		}
		return kind == DefinitionKind.CORPUS ? AssistantDefinition.CORPUS_REVIEW
				: REQUIREMENTS_REVIEW;
	}

	/** The review task (#255); a REVIEW definition serves it. */
	static final String REQUIREMENTS_REVIEW = "REQUIREMENTS_REVIEW";

	private static String outputSchema(DefinitionKind kind) {
		if (kind == DefinitionKind.POLICY) {
			return AssistantDefinition.POLICY_OUTPUT_SCHEMA;
		}
		return kind == DefinitionKind.CORPUS ? AssistantDefinition.CORPUS_OUTPUT_SCHEMA
				: "RequirementsReviewOutput";
	}

	private static String outputSchemaVersion(DefinitionKind kind) {
		return kind == DefinitionKind.REVIEW || kind == null ? "2" : "1";
	}

	private static AssistantDefinition fromDraft(String key, DefinitionKind kind, String taskType,
			String schema, String schemaVersion, ProjectAssistantDefinitions.Draft draft,
			int version, Long projectId, Integer forkedFromVersion, boolean enabled) {
		List<VocabularyEntry> vocabulary = new ArrayList<>();
		for (ProjectAssistantDefinitions.Vocabulary entry : draft.vocabulary()) {
			// a missing entry becomes a typeless one, which validation reports
			vocabulary.add(entry == null ? new VocabularyEntry(null, null, null)
					: new VocabularyEntry(strip(entry.type()), strip(entry.description()),
							strip(entry.category())));
		}
		return new AssistantDefinition(key, strip(draft.displayName()),
				kind == null ? DefinitionKind.REVIEW : kind, taskType, draft.scope(),
				draft.contextProviders(), draft.instructions(), vocabulary, schema, schemaVersion,
				enabled, version, DefinitionSource.PROJECT, projectId, forkedFromVersion, null,
				draft.contextBudgets(), draft.localOnly());
	}

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}

	/**
	 * The entity types (or set kinds) {@code definition} covers among {@code effective}, the
	 * definitions in effect in its project: a specific definition beats the fallback for its
	 * types (as {@code SimpleAssistantRegistry} decides it), and a policy with an empty scope
	 * covers every type.
	 */
	static List<String> inEffectFor(AssistantDefinition definition,
			Collection<AssistantDefinition> effective) {
		Set<String> types = definition.isCorpus() ? AssistantDefinition.CORPUS_SET_KINDS
				: AssistantDefinitionValidator.REVIEWABLE_TYPES;
		Set<String> covered = new java.util.TreeSet<>();
		if (definition.isPolicy() || !definition.isFallback()) {
			for (String type : types) {
				if (definition.appliesTo(type)) {
					covered.add(type);
				}
			}
			return List.copyOf(covered);
		}
		covered.addAll(types);
		for (AssistantDefinition other : effective) {
			if (Objects.equals(other.taskType(), definition.taskType())) {
				covered.removeAll(other.scope());
			}
		}
		return List.copyOf(covered);
	}

	private static ProjectAssistantDefinitions.View view(AssistantDefinition definition,
			AssistantDefinition bundled, int lockVersion, List<String> inEffectFor) {
		List<ProjectAssistantDefinitions.Vocabulary> vocabulary = new ArrayList<>();
		for (VocabularyEntry entry : definition.vocabulary()) {
			vocabulary.add(new ProjectAssistantDefinitions.Vocabulary(entry.type(),
					entry.description(), entry.category()));
		}
		boolean project = definition.source() == DefinitionSource.PROJECT;
		return new ProjectAssistantDefinitions.View(definition.key(), definition.displayName(),
				definition.kind().name(), definition.taskType(),
				new java.util.TreeSet<>(definition.scope()), definition.contextProviders(),
				definition.contextBudgets(), definition.instructions(), List.copyOf(vocabulary),
				definition.localOnly(), definition.source().name(), definition.version(),
				definition.forkedFromVersion(), bundled == null ? null : bundled.version(),
				project ? lockVersion : 0, inEffectFor);
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
		entity.setLocalOnly(definition.localOnly());
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
						}), row.isLocalOnly());
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
