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

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Builds the two halves of a review prompt — the system text and the JSON user body — the same way
 * for every client, so the Spring AI and CLI prompts cannot drift (#259). Each client adds its own
 * output-format instructions: Spring AI's structured-output converter appends the schema itself,
 * while the CLI client appends {@link AiAnalysisRequest#outputSchema()} explicitly.
 */
public final class AiPromptBuilder {

	static final String DEFAULT_GUIDANCE =
			"You are a Requel requirements analysis assistant. Analyze the target requirement and "
					+ "report quality problems. Findings are drafts for reviewable Requel "
					+ "annotations; do not invent commands that directly mutate project data.";

	private final ObjectMapper objectMapper;
	private final AiProperties properties;

	public AiPromptBuilder(ObjectMapper objectMapper, AiProperties properties) {
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.properties = Objects.requireNonNull(properties, "properties");
	}

	/** Task guidance (the request's, else the generic default), task type and locale. */
	public String instructions(AiAnalysisRequest request) {
		String guidance = request.instructions() != null && !request.instructions().isBlank()
				? request.instructions()
				: DEFAULT_GUIDANCE;
		return guidance
				+ "\n\nTask type: " + request.taskType()
				+ "\nLocale: " + request.locale().toLanguageTag();
	}

	/** The request as pretty-printed JSON: refs, context packs, schema name/version, flags. */
	public String prompt(AiAnalysisRequest request) {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("assistantId", request.assistantId());
		root.put("runId", request.runId().toString());
		root.put("taskType", request.taskType());
		root.set("targetRef", objectMapper.valueToTree(request.targetRef()));
		root.set("projectRef", objectMapper.valueToTree(request.projectRef()));
		root.put("locale", request.locale().toLanguageTag());
		root.set("contextPacks", objectMapper.valueToTree(request.contextPacks()));
		root.put("outputSchemaName", request.outputSchemaName());
		root.put("outputSchemaVersion", request.outputSchemaVersion());
		root.set("dataHandlingFlags", objectMapper.valueToTree(request.dataHandlingFlags()));
		root.set("attributes", objectMapper.valueToTree(request.attributes()));
		root.put("approximateInputTokenBudget", properties.getMaxInputTokens());
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize AI analysis prompt", e);
		}
	}
}
