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
package com.rreganjr.requel.assistant.ai;

import jakarta.annotation.PostConstruct;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.core.AssistantRunStore;
import com.rreganjr.requel.assistant.core.context.EntityContextPackBuilder;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.DefinitionExecutorFactory;
import com.rreganjr.requel.assistant.core.persistence.AssistantUsageRepository;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;

/**
 * Issue #260: builds a {@link DefinitionExecutorAssistant} for each definition a run resolves,
 * sharing the provider client and the rest of what {@code RequirementsReviewAssistant} held. The
 * bean exists only when {@code requel.ai.enabled=true}; without it definitions are not run, as
 * the old assistant bean didn't exist then either.
 */
@Component
@ConditionalOnProperty(name = "requel.ai.enabled", havingValue = "true")
public class AiDefinitionExecutorFactory implements DefinitionExecutorFactory {

	private static final Logger log = LoggerFactory.getLogger(AiDefinitionExecutorFactory.class);

	final AiAnalysisClient aiAnalysisClient;
	final EntityContextPackBuilder entityContextPackBuilder;
	final AiProperties aiProperties;
	final AssistantUsageRepository usageRepository;
	final ObjectMapper objectMapper;
	final Clock clock;
	final OutputSchemas outputSchemas;
	/** #262: per-project data-handling settings; defaults (all on) when absent. */
	ProjectAssistantSettingsStore settingsStore;
	/** #262: where the run's redaction counts are recorded; skipped when absent. */
	AssistantRunStore runStore;
	/** #262: the provider's locality, reported in the data-handling flags. */
	AiProviderLocality providerLocality;

	@Autowired
	public AiDefinitionExecutorFactory(AiAnalysisClient aiAnalysisClient,
			EntityContextPackBuilder entityContextPackBuilder, AiProperties aiProperties,
			AssistantUsageRepository usageRepository, ObjectMapper objectMapper) {
		this(aiAnalysisClient, entityContextPackBuilder, aiProperties, usageRepository, objectMapper,
				Clock.systemUTC());
	}

	AiDefinitionExecutorFactory(AiAnalysisClient aiAnalysisClient,
			EntityContextPackBuilder entityContextPackBuilder, AiProperties aiProperties,
			AssistantUsageRepository usageRepository, ObjectMapper objectMapper, Clock clock) {
		this.aiAnalysisClient = aiAnalysisClient;
		this.entityContextPackBuilder = entityContextPackBuilder;
		this.aiProperties = aiProperties;
		this.usageRepository = usageRepository;
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.outputSchemas = new OutputSchemas(objectMapper);
	}

	@Autowired(required = false)
	public void setSettingsStore(ProjectAssistantSettingsStore settingsStore) {
		this.settingsStore = settingsStore;
	}

	@Autowired(required = false)
	public void setRunStore(AssistantRunStore runStore) {
		this.runStore = runStore;
	}

	@Autowired(required = false)
	public void setProviderLocality(AiProviderLocality providerLocality) {
		this.providerLocality = providerLocality;
	}

	/**
	 * Confirms at startup that AI review is active, with the provider and model (never the key),
	 * so the wiring can be checked from the boot log.
	 */
	@PostConstruct
	void logStartupState() {
		List<String> allowlist = aiProperties.getProjectAllowlist();
		log.info("AI review definitions enabled (provider={}, model={}, projectAllowlist={})",
				aiProperties.getProvider(), aiProperties.getModel(),
				allowlist == null || allowlist.isEmpty() ? "all projects" : allowlist);
	}

	@Override
	public RequelAssistant<?> executorFor(AssistantDefinition definition) {
		return new DefinitionExecutorAssistant(definition, this);
	}
}
