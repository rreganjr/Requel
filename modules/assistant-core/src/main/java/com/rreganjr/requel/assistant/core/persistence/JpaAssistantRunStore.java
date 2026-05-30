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
package com.rreganjr.requel.assistant.core.persistence;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.assistant.core.AssistantRunRecord;
import com.rreganjr.requel.assistant.core.AssistantRunStatus;
import com.rreganjr.requel.assistant.core.AssistantRunStore;

/**
 * Production {@link AssistantRunStore} backed by the {@code assistant_runs}
 * table. Each entry point opens its own transaction so the dispatcher (no
 * ambient transaction) can persist a {@code QUEUED} row before handing work
 * to the executor, and the worker (already inside a {@code REQUIRES_NEW}
 * transaction) can reuse that transaction for status updates.
 *
 * <p>The richer {@link AssistantRunEntity} fields beyond what
 * {@link AssistantRunRecord} exposes ({@code project_id}, {@code target_type},
 * {@code target_id}, etc.) are populated from the {@link AnalysisRequest} at
 * queue time so downstream tooling (run history, audit, MCP) can read them
 * without reconstructing the request payload.</p>
 */
@Component
public class JpaAssistantRunStore implements AssistantRunStore {

	private final AssistantRunRepository runRepository;
	private final Clock clock;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	public JpaAssistantRunStore(AssistantRunRepository runRepository) {
		this(runRepository, Clock.systemUTC());
	}

	JpaAssistantRunStore(AssistantRunRepository runRepository, Clock clock) {
		this.runRepository = Objects.requireNonNull(runRepository, "runRepository");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public AssistantRunRecord queueRun(AnalysisRequest request) {
		Objects.requireNonNull(request, "request");
		UUID runId = UUID.randomUUID();
		Instant now = clock.instant();
		AssistantRunEntity entity = new AssistantRunEntity(runId, assistantIdFromRequest(request),
				AssistantRunStatus.QUEUED.name(), now, now);
		UserRef triggeringUser = request.triggeringUser();
		if (triggeringUser != null) {
			entity.setTriggeredByUserId(triggeringUser.userId());
			entity.setTriggeredByUsername(triggeringUser.username());
		}
		UserRef assistantUser = request.assistantUser();
		if (assistantUser != null) {
			entity.setAssistantUserId(assistantUser.userId());
			entity.setAssistantUsername(assistantUser.username());
		}
		EntityRef projectRef = request.projectRef();
		if (projectRef != null) {
			entity.setProjectId(projectRef.entityId());
		}
		EntityRef targetRef = request.targetRef();
		if (targetRef != null) {
			entity.setTargetType(targetRef.entityType());
			entity.setTargetId(targetRef.entityId());
		}
		entity.setTaskType(request.taskType());
		if (request.locale() != null) {
			entity.setLocale(request.locale().toLanguageTag());
		}
		entity.setAttributesJson(writeAttributes(request.attributes()));
		runRepository.save(entity);
		return toRecord(entity, request);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void markRunning(UUID runId) {
		update(runId, AssistantRunStatus.RUNNING, null, entity -> entity.setStartedAt(clock.instant()));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void markSucceeded(UUID runId) {
		update(runId, AssistantRunStatus.SUCCEEDED, null, entity -> entity.setCompletedAt(clock.instant()));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void markSkipped(UUID runId, String reason) {
		update(runId, AssistantRunStatus.SKIPPED, reason, entity -> entity.setCompletedAt(clock.instant()));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void markFailed(UUID runId, Throwable failure) {
		String summary = failure == null ? null : failure.getMessage();
		String kind = failure == null ? null : failure.getClass().getSimpleName();
		update(runId, AssistantRunStatus.FAILED, summary, entity -> {
			entity.setErrorKind(kind);
			entity.setCompletedAt(clock.instant());
		});
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Optional<AssistantRunRecord> findRun(UUID runId) {
		Objects.requireNonNull(runId, "runId");
		return runRepository.findById(runId.toString())
				.map(entity -> toRecord(entity, null));
	}

	private void update(UUID runId, AssistantRunStatus status, String errorSummary,
			java.util.function.Consumer<AssistantRunEntity> mutator) {
		Objects.requireNonNull(runId, "runId");
		Objects.requireNonNull(status, "status");
		Optional<AssistantRunEntity> found = runRepository.findById(runId.toString());
		if (found.isEmpty()) {
			return;
		}
		AssistantRunEntity entity = found.get();
		entity.setStatus(status.name());
		entity.setErrorSummary(errorSummary);
		entity.setUpdatedAt(clock.instant());
		if (mutator != null) {
			mutator.accept(entity);
		}
		runRepository.save(entity);
	}

	/**
	 * Translate an entity to the lightweight record. {@code request} may be
	 * {@code null} when the caller is reading a previously queued run from
	 * persistence; in that case the returned record carries an
	 * {@link AnalysisRequest} reconstructed from persisted fields. Since Step 1
	 * of the Phase 4.5 plan, the reconstruction is faithful: usernames, locale,
	 * and the attributes map are persisted and round-tripped.
	 */
	private AssistantRunRecord toRecord(AssistantRunEntity entity, AnalysisRequest request) {
		AnalysisRequest effective = request != null ? request : rebuildRequest(entity);
		return new AssistantRunRecord(entity.getRunId(), effective,
				AssistantRunStatus.valueOf(entity.getStatus()), entity.getCreatedAt(),
				entity.getUpdatedAt(), entity.getErrorSummary());
	}

	/**
	 * Reconstructs an {@link AnalysisRequest} from persisted fields. The target
	 * ref, project ref, triggering/assistant user refs (id + username), task
	 * type, locale, and attributes are all stored, so the rebuilt request equals
	 * the dispatched one (modulo absent optional fields). When the target ref was
	 * not stored, an {@code Unknown:0} placeholder is used to satisfy
	 * {@code AnalysisRequest}'s non-null target contract.
	 */
	private AnalysisRequest rebuildRequest(AssistantRunEntity entity) {
		EntityRef targetRef = entity.getTargetType() != null && entity.getTargetId() != null
				? EntityRef.of(entity.getTargetType(), entity.getTargetId())
				: EntityRef.of("Unknown", 0L);
		EntityRef projectRef = entity.getProjectId() != null
				? EntityRef.of("Project", entity.getProjectId())
				: null;
		UserRef triggeringUser = userRef(entity.getTriggeredByUserId(),
				entity.getTriggeredByUsername());
		UserRef assistantUser = userRef(entity.getAssistantUserId(), entity.getAssistantUsername());
		Locale locale = entity.getLocale() != null ? Locale.forLanguageTag(entity.getLocale())
				: Locale.ROOT;
		return new AnalysisRequest(targetRef, projectRef, triggeringUser, assistantUser,
				entity.getTaskType(), locale, readAttributes(entity.getAttributesJson()));
	}

	private static UserRef userRef(Long userId, String username) {
		long id = userId != null ? userId : 0L;
		String name = username != null && !username.isBlank() ? username
				: (userId != null ? "id:" + userId : "unknown");
		return new UserRef(id, name);
	}

	private String writeAttributes(Map<String, Object> attributes) {
		if (attributes == null || attributes.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(attributes);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize assistant run attributes", e);
		}
	}

	private Map<String, Object> readAttributes(String json) {
		if (json == null || json.isBlank()) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
			});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not read assistant run attributes", e);
		}
	}

	/**
	 * Derive an {@code assistant_id} for the run row when none is supplied on
	 * the request. The first AI assistant uses task type {@code REQUIREMENTS_REVIEW};
	 * legacy dispatches that pre-date task types use the assistant pseudo-user
	 * name or fall back to {@code unspecified}.
	 */
	private static String assistantIdFromRequest(AnalysisRequest request) {
		if (request.taskType() != null && !request.taskType().isBlank()) {
			return request.taskType();
		}
		UserRef assistantUser = request.assistantUser();
		if (assistantUser != null && assistantUser.username() != null
				&& !assistantUser.username().isBlank()) {
			return assistantUser.username();
		}
		return "unspecified";
	}
}
