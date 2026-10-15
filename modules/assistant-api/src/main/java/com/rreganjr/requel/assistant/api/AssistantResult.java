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
package com.rreganjr.requel.assistant.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Normalized assistant output. Persisted changes are represented as actions
 * and are applied by implementation code.
 */
public record AssistantResult(String assistantId, UUID runId, String summary, String severity,
		List<AnnotationAction> annotationActions, List<AssistantMessage> messages,
		List<ExternalAction> externalActions, Map<String, Object> metadata) {

	public AssistantResult {
		Objects.requireNonNull(assistantId, "assistantId");
		annotationActions = List.copyOf(Objects.requireNonNull(annotationActions,
				"annotationActions"));
		messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
		externalActions = List.copyOf(Objects.requireNonNull(externalActions,
				"externalActions"));
		metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
	}

	public Optional<UUID> runIdValue() {
		return Optional.ofNullable(runId);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String assistantId;
		private UUID runId;
		private String summary;
		private String severity;
		private final List<AnnotationAction> annotationActions = new ArrayList<AnnotationAction>();
		private final List<AssistantMessage> messages = new ArrayList<AssistantMessage>();
		private final List<ExternalAction> externalActions = new ArrayList<ExternalAction>();
		private Map<String, Object> metadata = Map.of();

		private Builder() {
		}

		public Builder assistantId(String assistantId) {
			this.assistantId = assistantId;
			return this;
		}

		public Builder runId(UUID runId) {
			this.runId = runId;
			return this;
		}

		public Builder summary(String summary) {
			this.summary = summary;
			return this;
		}

		public Builder severity(String severity) {
			this.severity = severity;
			return this;
		}

		public Builder annotationAction(AnnotationAction action) {
			annotationActions.add(action);
			return this;
		}

		public Builder message(AssistantMessage message) {
			messages.add(message);
			return this;
		}

		public Builder externalAction(ExternalAction action) {
			externalActions.add(action);
			return this;
		}

		public Builder metadata(Map<String, Object> metadata) {
			this.metadata = Map.copyOf(metadata);
			return this;
		}

		public AssistantResult build() {
			return new AssistantResult(assistantId, runId, summary, severity, annotationActions,
					messages, externalActions, metadata);
		}
	}
}
