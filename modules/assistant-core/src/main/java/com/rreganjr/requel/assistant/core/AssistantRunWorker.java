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

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantException;
import com.rreganjr.requel.assistant.api.AssistantRegistry;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.RequelAssistant;

/**
 * Runs queued assistant work behind a Spring transactional proxy.
 */
@Component
public class AssistantRunWorker {

	private final AssistantRunStore runStore;
	private final AssistantRegistry assistantRegistry;
	private final AssistantResultApplicator resultApplicator;
	private final List<AssistantTargetLoader> targetLoaders;
	private final Clock clock;

	public AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders) {
		this(runStore, assistantRegistry, resultApplicator, targetLoaders, Clock.systemUTC());
	}

	AssistantRunWorker(AssistantRunStore runStore, AssistantRegistry assistantRegistry,
			AssistantResultApplicator resultApplicator, List<AssistantTargetLoader> targetLoaders,
			Clock clock) {
		this.runStore = runStore;
		this.assistantRegistry = assistantRegistry;
		this.resultApplicator = resultApplicator;
		this.targetLoaders = List.copyOf(targetLoaders);
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void runInNewTransaction(UUID runId) {
		Objects.requireNonNull(runId, "runId");
		Optional<AssistantRunRecord> recordValue = runStore.findRun(runId);
		if (recordValue.isEmpty()) {
			return;
		}

		runStore.markRunning(runId);
		try {
			AssistantRunRecord record = recordValue.get();
			AnalysisRequest request = record.request();
			Optional<Object> targetValue = loadTarget(request);
			if (targetValue.isEmpty()) {
				runStore.markSkipped(runId, "No assistant target loader resolved "
						+ request.targetRef().entityType());
				return;
			}

			Object target = targetValue.get();
			AssistantContext context = new AssistantContext(runId, request.triggeringUser(),
					request.assistantUser(), request.projectRef(), Locale.getDefault(), clock,
					request.attributes());
			List<RequelAssistant<?>> assistants = assistantRegistry.findAssistantsFor(target,
					context);
			if (assistants.isEmpty()) {
				runStore.markSkipped(runId, "No assistants registered for "
						+ target.getClass().getName());
				return;
			}

			for (RequelAssistant<?> assistant : assistants) {
				AssistantResult result = analyze(assistant, context, target);
				resultApplicator.apply(context, result);
			}
			runStore.markSucceeded(runId);
		} catch (RuntimeException | AssistantException e) {
			runStore.markFailed(runId, e);
			throw new AssistantWorkerException("Assistant run failed: " + runId, e);
		}
	}

	private Optional<Object> loadTarget(AnalysisRequest request) {
		for (AssistantTargetLoader targetLoader : targetLoaders) {
			if (targetLoader.supports(request.targetRef())) {
				return targetLoader.loadTarget(request.targetRef());
			}
		}
		return Optional.empty();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private AssistantResult analyze(RequelAssistant assistant, AssistantContext context,
			Object target) throws AssistantException {
		return assistant.analyze(context, target);
	}
}
