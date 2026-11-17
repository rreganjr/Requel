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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;

/**
 * The agent call of the idea-to-requirements feature: sends a product manager's free-text idea,
 * with the names the project already has, to the configured AI provider under the
 * {@link IdeaToRequirements} prompt, and returns the raw {@link AiAnalysisResponse}. It does not
 * parse or validate the proposals; a later step maps the findings to drafts.
 *
 * <p>
 * It reuses {@link DefinitionExecutorAssistant} for the skip gating (task type, enabled flag,
 * project allowlist), the data-handling flags and usage recording. There is no entity to read,
 * so the call needs no transaction.
 *
 * <p>
 * The idea and the names are sent as given: the entity context builder's redaction does not run
 * on them. The data-handling flags still go with the request, so a remote client refuses unless
 * the project allows external providers.
 *
 * <p>
 * The bean exists only when {@code requel.ai.enabled=true}, like {@link AiDefinitionExecutorFactory}.
 */
@Component
@ConditionalOnProperty(name = "requel.ai.enabled", havingValue = "true")
public class IdeaToRequirementsService {

	private static final Logger log = LoggerFactory.getLogger(IdeaToRequirementsService.class);

	/** Rough chars-per-token, as {@link DefinitionExecutorAssistant} estimates input size. */
	private static final int CHARS_PER_TOKEN_ESTIMATE = 4;

	private static final int MAX_EXISTING_NAMES = 200;

	private static final int MAX_NAME_LENGTH = 255;

	/**
	 * What the model is sent: the PM's idea and the names the project already has, so it doesn't
	 * propose them again. The record is the context pack, serialized as it is. The name lists
	 * may be null or empty.
	 */
	public record Idea(String ideaText, List<String> existingGoals, List<String> existingActors,
			List<String> existingGlossaryTerms) {
	}

	/**
	 * The result of one call. When {@link #skipped()}, no call was made and
	 * {@code skippedReason} says why; otherwise {@code response} is the model's reply, unparsed
	 * beyond the review contract.
	 */
	public record Outcome(AiAnalysisResponse response, String skippedReason) {

		static Outcome skipped(String reason) {
			return new Outcome(null, reason);
		}

		public boolean skipped() {
			return response == null;
		}
	}

	private final AiDefinitionExecutorFactory runtime;

	@Autowired
	public IdeaToRequirementsService(AiDefinitionExecutorFactory runtime) {
		this.runtime = runtime;
	}

	/**
	 * Send {@code idea} to the provider.
	 *
	 * @param context a run context whose {@code taskType()} is {@link IdeaToRequirements#TASK_TYPE}
	 *        and whose {@code projectRef()} is the project the idea is for
	 * @throws AssistantException if the idea is blank, the context has no project, the input
	 *         can't be sized, or the provider call fails
	 */
	public Outcome analyze(AssistantContext context, Idea idea) throws AssistantException {
		if (idea == null || idea.ideaText() == null || idea.ideaText().isBlank()) {
			throw new AssistantException("An idea is required");
		}
		if (context.projectRef() == null) {
			throw new AssistantException("An idea is analysed for a project");
		}
		int max = Math.max(1, runtime.aiProperties.getIdea().getMaxProposals());
		DefinitionExecutorAssistant executor = new DefinitionExecutorAssistant(
				IdeaToRequirements.definition(max), runtime);

		String skipReason = executor.skipReason(context);
		if (skipReason != null) {
			log.debug("idea-to-requirements skipping run {}: {}", context.runId(), skipReason);
			return Outcome.skipped(skipReason);
		}

		Idea pack = bounded(idea);
		int cap = runtime.aiProperties.getMaxInputTokens();
		int estimatedInputTokens = estimateInputTokens(pack);
		if (estimatedInputTokens > cap) {
			log.info("Skipping idea-to-requirements for run {}: estimated {} input tokens exceeds cap {}",
					context.runId(), estimatedInputTokens, cap);
			return Outcome.skipped("The idea exceeds the configured AI input cap (estimated "
					+ estimatedInputTokens + " tokens, requel.ai.maxInputTokens=" + cap + ").");
		}

		AiAnalysisRequest request = new AiAnalysisRequest(executor.assistantId(), context.runId(),
				IdeaToRequirements.TASK_TYPE, context.projectRef(), context.projectRef(),
				context.locale(), List.of(pack), IdeaToRequirements.OUTPUT_SCHEMA_NAME,
				IdeaToRequirements.OUTPUT_SCHEMA_VERSION, executor.outputSchema(),
				executor.dataHandlingFlags(context.projectRef()), executor.attributes(context),
				executor.instructions());
		try {
			AiAnalysisResponse response = runtime.aiAnalysisClient.analyze(request);
			executor.persistUsage(context.runId(), response.usage());
			return new Outcome(response, null);
		} catch (AiAnalysisException e) {
			log.warn("Idea-to-requirements failed for run {}: {}", context.runId(), e.getMessage(),
					e);
			throw new AssistantException("AI idea analysis failed: " + e.getMessage(), e);
		}
	}

	/** The idea as sent: the text stripped, and the name lists cleaned and capped. */
	private static Idea bounded(Idea idea) {
		return new Idea(idea.ideaText().strip(), names(idea.existingGoals()),
				names(idea.existingActors()), names(idea.existingGlossaryTerms()));
	}

	private static List<String> names(List<String> names) {
		List<String> cleaned = new ArrayList<String>();
		if (names == null) {
			return cleaned;
		}
		for (String name : names) {
			if (name == null || name.isBlank() || cleaned.size() >= MAX_EXISTING_NAMES) {
				continue;
			}
			String oneLine = name.replaceAll("\\s+", " ").strip();
			cleaned.add(oneLine.length() <= MAX_NAME_LENGTH ? oneLine
					: oneLine.substring(0, MAX_NAME_LENGTH));
		}
		return cleaned;
	}

	/**
	 * The input size in tokens, from the serialized pack's length. Unlike a review's best-effort
	 * estimate, a failure here is an error: the idea is the whole input and can't go unsized.
	 */
	private int estimateInputTokens(Idea pack) throws AssistantException {
		try {
			return runtime.objectMapper.writeValueAsString(List.of(pack)).length()
					/ CHARS_PER_TOKEN_ESTIMATE;
		} catch (JsonProcessingException e) {
			throw new AssistantException("Could not read the idea: " + e.getMessage(), e);
		}
	}
}
