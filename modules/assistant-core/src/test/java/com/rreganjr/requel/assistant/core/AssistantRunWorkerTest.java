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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.api.UserRef;

class AssistantRunWorkerTest {

	@Test
	void runReloadsTargetAndAppliesMatchingAssistantResult() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		StringAssistant assistant = new StringAssistant();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(assistant)), applicator,
				List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED));
		assertThat(applicator.appliedResults).hasSize(1);
		assertThat(applicator.appliedResults.get(0).assistantId()).isEqualTo("string-assistant");
		// The run's task type is threaded into the AssistantContext the assistant sees.
		assertThat(assistant.seenTaskType).isEqualTo("REQUIREMENTS_REVIEW");
	}

	@Test
	void runIsSkippedWhenNoAssistantHandlesTheTask() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		// DefaultTaskAssistant serves only the null/default task; the run is REQUIREMENTS_REVIEW.
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new DefaultTaskAssistant())), applicator,
				List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SKIPPED));
		assertThat(applicator.appliedResults).isEmpty();
	}

	/**
	 * #247: the apply phase re-checks the target. A target deleted while the (slow)
	 * analysis ran must not have findings written for it - that insert would hit the
	 * FK of a row that no longer exists.
	 */
	@Test
	void findingsAreDiscardedWhenTargetDisappearsBetweenAnalyzeAndApply() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		VanishingTargetLoader loader = new VanishingTargetLoader();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), applicator,
				List.of(loader));

		worker.run(record.runId());

		assertThat(loader.loads).isEqualTo(2);
		assertThat(applicator.appliedResults).isEmpty();
		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SKIPPED));
	}

	/**
	 * #247: analysis and apply run in separate transactions, analysis first and
	 * committed before apply begins.
	 */
	@Test
	void analyzeTransactionCompletesBeforeApplyTransactionStarts() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		List<String> phases = new java.util.ArrayList<>();
		RecordingApplicator applicator = new RecordingApplicator() {
			@Override
			public AppliedAssistantResult apply(AssistantContext context, AssistantResult result,
					CleanupPolicy cleanupPolicy, EntityRef dispatchTarget) {
				phases.add("apply-body");
				return super.apply(context, result, cleanupPolicy, dispatchTarget);
			}
		};
		org.springframework.transaction.support.TransactionOperations analyzeTx = phaseRecorder(
				phases, "analyze");
		org.springframework.transaction.support.TransactionOperations applyTx = phaseRecorder(
				phases, "apply");
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), applicator,
				List.of(new StringTargetLoader()), java.time.Clock.systemUTC(), analyzeTx,
				applyTx);

		worker.run(record.runId());

		assertThat(phases).containsExactly("analyze-begin", "analyze-commit", "apply-begin",
				"apply-body", "apply-commit");
		assertThat(applicator.appliedResults).hasSize(1);
	}

	@Test
	void oneAssistantFailingToAnalyzeDoesNotAbortTheOthers() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new ThrowingAssistant(), new StringAssistant())),
				applicator, List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED));
		assertThat(applicator.appliedResults).hasSize(1);
		assertThat(applicator.appliedResults.get(0).assistantId()).isEqualTo("string-assistant");
	}

	@Test
	void oneResultFailingToApplyDoesNotFailTheRun() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator() {
			private boolean first = true;

			@Override
			public AppliedAssistantResult apply(AssistantContext context, AssistantResult result,
					CleanupPolicy cleanupPolicy, EntityRef dispatchTarget) {
				if (first) {
					first = false;
					throw new IllegalStateException("apply boom");
				}
				return super.apply(context, result, cleanupPolicy, dispatchTarget);
			}
		};
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant(), new StringAssistant())),
				applicator, List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED));
		assertThat(applicator.appliedResults).hasSize(1);
	}

	@Test
	void runIsSkippedWhenNoLoaderResolvesTheTarget() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), applicator, List.of());

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SKIPPED));
		assertThat(applicator.appliedResults).isEmpty();
	}

	@Test
	void runIsSkippedWhenNoAssistantIsRegisteredForTheTarget() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of()), applicator, List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SKIPPED));
		assertThat(applicator.appliedResults).isEmpty();
	}

	@Test
	void unknownRunIdIsANoOp() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), applicator,
				List.of(new StringTargetLoader()));

		worker.run(java.util.UUID.randomUUID());

		assertThat(applicator.appliedResults).isEmpty();
	}

	@Test
	void infrastructureFailureMarksTheRunFailedAndRethrows() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		AssistantTargetLoader brokenLoader = new AssistantTargetLoader() {
			@Override
			public boolean supports(EntityRef targetRef) {
				return true;
			}

			@Override
			public Optional<Object> loadTarget(EntityRef targetRef) {
				throw new IllegalStateException("database gone");
			}
		};
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())),
				new RecordingApplicator(), List.of(brokenLoader));

		org.junit.jupiter.api.Assertions.assertThrows(AssistantWorkerException.class,
				() -> worker.run(record.runId()));

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.FAILED));
	}

	/**
	 * The Spring constructor wraps both phases in REQUIRES_NEW templates over the
	 * transaction manager: one begin/commit per phase, none nested.
	 */
	@Test
	@SuppressWarnings("deprecation")
	void springConstructorRunsEachPhaseInItsOwnNewTransaction() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		CountingTransactionManager transactionManager = new CountingTransactionManager();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), applicator,
				List.of(new StringTargetLoader()), transactionManager);

		worker.runInNewTransaction(record.runId()); // deprecated alias of run()

		assertThat(transactionManager.begun).isEqualTo(2);
		assertThat(transactionManager.committed).isEqualTo(2);
		assertThat(transactionManager.rolledBack).isZero();
		assertThat(transactionManager.propagations).containsOnly(
				org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertThat(applicator.appliedResults).hasSize(1);
	}

	private static final class CountingTransactionManager
			implements org.springframework.transaction.PlatformTransactionManager {
		private int begun;
		private int committed;
		private int rolledBack;
		private final List<Integer> propagations = new java.util.ArrayList<>();

		@Override
		public org.springframework.transaction.TransactionStatus getTransaction(
				org.springframework.transaction.TransactionDefinition definition) {
			begun++;
			propagations.add(definition.getPropagationBehavior());
			return new org.springframework.transaction.support.SimpleTransactionStatus();
		}

		@Override
		public void commit(org.springframework.transaction.TransactionStatus status) {
			committed++;
		}

		@Override
		public void rollback(org.springframework.transaction.TransactionStatus status) {
			rolledBack++;
		}
	}

	private static final class ThrowingAssistant implements RequelAssistant<String> {
		@Override
		public String assistantId() {
			return "throwing-assistant";
		}

		@Override
		public Class<String> targetType() {
			return String.class;
		}

		@Override
		public boolean handlesTask(String taskType) {
			return true;
		}

		@Override
		public AssistantResult analyze(AssistantContext context, String target) {
			throw new IllegalStateException("analyze boom");
		}
	}

	private static org.springframework.transaction.support.TransactionOperations phaseRecorder(
			List<String> phases, String name) {
		return new org.springframework.transaction.support.TransactionOperations() {
			@Override
			public <T> T execute(
					org.springframework.transaction.support.TransactionCallback<T> action) {
				phases.add(name + "-begin");
				T result = action.doInTransaction(
						new org.springframework.transaction.support.SimpleTransactionStatus());
				phases.add(name + "-commit");
				return result;
			}
		};
	}

	/** Loads the target once, then reports it gone (deleted mid-run). */
	private static final class VanishingTargetLoader implements AssistantTargetLoader {
		private int loads;

		@Override
		public boolean supports(EntityRef targetRef) {
			return "Goal".equals(targetRef.entityType());
		}

		@Override
		public Optional<Object> loadTarget(EntityRef targetRef) {
			loads++;
			return (loads == 1) ? Optional.of("target") : Optional.empty();
		}
	}

	private AnalysisRequest request() {
		return new AnalysisRequest(EntityRef.of("Goal", 1L), EntityRef.of("Project", 2L),
				new UserRef(3L, "human"), new UserRef(4L, "assistant"),
				"REQUIREMENTS_REVIEW", java.util.Locale.ROOT, Map.of());
	}

	private static final class StringTargetLoader implements AssistantTargetLoader {
		@Override
		public boolean supports(EntityRef targetRef) {
			return "Goal".equals(targetRef.entityType());
		}

		@Override
		public Optional<Object> loadTarget(EntityRef targetRef) {
			return Optional.of("target");
		}
	}

	private static final class StringAssistant implements RequelAssistant<String> {
		private String seenTaskType;

		@Override
		public String assistantId() {
			return "string-assistant";
		}

		@Override
		public Class<String> targetType() {
			return String.class;
		}

		@Override
		public boolean handlesTask(String taskType) {
			return true; // handles any task, including the test's REQUIREMENTS_REVIEW run
		}

		@Override
		public AssistantResult analyze(AssistantContext context, String target) {
			this.seenTaskType = context.taskType();
			return AssistantResult.builder().assistantId(assistantId()).runId(context.runId())
					.summary(target).build();
		}
	}

	/** Serves only the default (null) task via the SPI default {@code handlesTask}. */
	private static final class DefaultTaskAssistant implements RequelAssistant<String> {
		@Override
		public String assistantId() {
			return "default-task-assistant";
		}

		@Override
		public Class<String> targetType() {
			return String.class;
		}

		@Override
		public AssistantResult analyze(AssistantContext context, String target) {
			return AssistantResult.builder().assistantId(assistantId()).runId(context.runId())
					.build();
		}
	}

	private static class RecordingApplicator implements AssistantResultApplicator {
		private final List<AssistantResult> appliedResults = new java.util.ArrayList<AssistantResult>();

		@Override
		public AppliedAssistantResult apply(AssistantContext context, AssistantResult result,
				CleanupPolicy cleanupPolicy, EntityRef dispatchTarget) {
			appliedResults.add(result);
			return new AppliedAssistantResult(0, List.of());
		}
	}
}
