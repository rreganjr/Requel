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

	/**
	 * #265: a composed assistant's results are applied one per member, each with that member's
	 * cleanup policy, as if the members had run alone.
	 */
	@Test
	void aComposedAssistantsResultsAreAppliedPerMember() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		List<String> applied = new java.util.ArrayList<>();
		AssistantResultApplicator applicator = (context, result, cleanupPolicy, target) -> {
			applied.add(result.assistantId() + ":" + cleanupPolicy);
			return new AppliedAssistantResult(0, List.of());
		};
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new Composed())), applicator,
				List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED));
		assertThat(applied).containsExactly("member-a:AUTO_RESOLVE_IF_UNTOUCHED",
				"member-b:MANUAL");
	}

	/** #265: a composed assistant that throws fails a run it was alone in, with its message. */
	@Test
	void aComposedAssistantThatThrowsFailsTheRun() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		Composed composed = new Composed();
		composed.failure = "9 policies apply; the limit is 8";
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(composed)), new RecordingApplicator(),
				List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> {
			assertThat(updated.status()).isEqualTo(AssistantRunStatus.FAILED);
			assertThat(updated.errorSummary()).contains("the limit is 8");
		});
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

	/**
	 * #363: a staged assistant prepares inside the analyze transaction and makes its call after
	 * that transaction has committed and before apply begins, with no transaction open.
	 */
	@Test
	void aStagedAssistantsCallRunsBetweenTheTransactions() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		List<String> phases = new java.util.ArrayList<>();
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new Staged(phases))), applicator,
				List.of(new StringTargetLoader()), java.time.Clock.systemUTC(),
				phaseRecorder(phases, "analyze"), phaseRecorder(phases, "apply"));

		worker.run(record.runId());

		assertThat(phases).containsExactly("analyze-begin", "prepare", "analyze-commit", "call",
				"apply-begin", "apply-commit");
		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED));
		assertThat(applicator.appliedResults).extracting(AssistantResult::summary)
				.containsExactly("called with target");
	}

	/** #363: a call that fails, alone in its run, fails the run and applies nothing. */
	@Test
	void aStagedCallThatThrowsFailsARunItWasAloneIn() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		Staged staged = new Staged(new java.util.ArrayList<>());
		staged.callFailure = "model timed out";
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(staged)), applicator,
				List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> {
			assertThat(updated.status()).isEqualTo(AssistantRunStatus.FAILED);
			assertThat(updated.errorSummary()).contains("staged-assistant failed")
					.contains("model timed out");
		});
		assertThat(applicator.appliedResults).isEmpty();
	}

	/**
	 * #363: a staged assistant that fails, in prepare or in its call, leaves the others' results
	 * applied and the run partial: it succeeded, and the summary names what failed.
	 */
	@Test
	void aFailingStagedAssistantBesideAnotherIsPartial() {
		for (boolean inPrepare : new boolean[] { true, false }) {
			InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
			AssistantRunRecord record = runStore.queueRun(request());
			Staged staged = new Staged(new java.util.ArrayList<>());
			if (inPrepare) {
				staged.prepareFailure = "no context";
			} else {
				staged.callFailure = "model timed out";
			}
			RecordingApplicator applicator = new RecordingApplicator();
			AssistantRunWorker worker = new AssistantRunWorker(runStore,
					new SimpleAssistantRegistry(List.of(staged, new StringAssistant())), applicator,
					List.of(new StringTargetLoader()));

			worker.run(record.runId());

			assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> {
				assertThat(updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED);
				assertThat(updated.errorSummary())
						.contains(inPrepare ? "no context" : "model timed out");
			});
			assertThat(applicator.appliedResults).extracting(AssistantResult::assistantId)
					.containsExactly("string-assistant");
		}
	}

	/** #355: the run keeps the assistants' own summary; a failed assistant contributes none. */
	@Test
	void theRunRecordsTheResultSummaryOfTheAssistantsThatRan() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new ThrowingAssistant(), new StringAssistant())),
				new RecordingApplicator(), List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.resultSummary(record.runId())).hasValueSatisfying(
				summary -> assertThat(summary).isNotBlank().doesNotContain("analyze boom"));
	}

	/** #260: the run records its definitions and the unverified evidence count. */
	@Test
	void theRunRecordsItsDefinitionsAndUnverifiedEvidence() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new DefinitionAssistant("goals", 3, 2),
						new DefinitionAssistant("fallback", 1, 1))),
				new RecordingApplicator(), List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.definitions(record.runId()))
				.contains(List.of("goals,fallback", "3,1", "BUNDLED,BUNDLED"));
		assertThat(runStore.evidenceUnverified(record.runId())).contains(3);
	}

	/** #260: no definition, no record (the lexical path). */
	@Test
	void aRunWithoutDefinitionsRecordsNone() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), new RecordingApplicator(),
				List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.definitions(record.runId())).isEmpty();
		assertThat(runStore.evidenceUnverified(record.runId())).isEmpty();
	}

	/** #355: a store that cannot record the summary never fails the run. */
	@Test
	void aFailureToRecordTheResultSummaryDoesNotFailTheRun() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore() {
			@Override
			public void recordResultSummary(java.util.UUID runId, String summary) {
				throw new IllegalStateException("summary column missing");
			}
		};
		AssistantRunRecord record = runStore.queueRun(request());
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), new RecordingApplicator(),
				List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED));
	}

	/** #355: blank summaries are not recorded. */
	@Test
	void blankResultSummariesAreNotRecorded() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new BlankSummaryAssistant())),
				new RecordingApplicator(), List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.resultSummary(record.runId())).isEmpty();
	}

	/** #355: the in-memory store forgets a summary that is set blank. */
	@Test
	void theInMemoryStoreClearsABlankSummary() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		java.util.UUID runId = runStore.queueRun(request()).runId();
		runStore.recordResultSummary(runId, "kept");
		assertThat(runStore.resultSummary(runId)).contains("kept");
		runStore.recordResultSummary(runId, " ");
		assertThat(runStore.resultSummary(runId)).isEmpty();
		runStore.recordResultSummary(runId, null);
		assertThat(runStore.resultSummary(runId)).isEmpty();
	}

	/** #355: the SPI's default ignores the summary, so existing stores need not implement it. */
	@Test
	void theStoreSpiDefaultIgnoresTheSummary() {
		AssistantRunStore store = org.mockito.Mockito.mock(AssistantRunStore.class,
				org.mockito.Mockito.CALLS_REAL_METHODS);
		store.recordResultSummary(java.util.UUID.randomUUID(), "ignored");
		org.mockito.Mockito.verify(store).recordResultSummary(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.eq("ignored"));
		org.mockito.Mockito.verifyNoMoreInteractions(store);
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

		// #268: the run is PARTIAL - it succeeded, and the summary names the assistant that threw.
		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> {
			assertThat(updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED);
			assertThat(updated.errorSummary()).contains("throwing-assistant failed")
					.contains("analyze boom").doesNotContain("string-assistant");
		});
		assertThat(applicator.appliedResults).hasSize(1);
		assertThat(applicator.appliedResults.get(0).assistantId()).isEqualTo("string-assistant");
	}

	/**
	 * #259: when every assistant on the run threw, the run did not do its job - it is FAILED,
	 * not a partial success, and nothing is applied. (A review run has exactly one assistant, so
	 * a provider failure lands here.)
	 */
	@Test
	void theRunFailsWhenEveryAssistantThrows() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new ThrowingAssistant())), applicator,
				List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> {
			assertThat(updated.status()).isEqualTo(AssistantRunStatus.FAILED);
			assertThat(updated.errorSummary()).contains("throwing-assistant failed")
					.contains("analyze boom");
		});
		assertThat(applicator.appliedResults).isEmpty();
	}

	@Test
	void anIncompleteResultIsAppliedAndTheRunIsPartial() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new IncompleteAssistant(), new StringAssistant())),
				applicator, List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> {
			assertThat(updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED);
			assertThat(updated.errorSummary()).contains("incomplete-assistant incomplete")
					.contains("Text");
		});
		assertThat(applicator.appliedResults).extracting(AssistantResult::assistantId)
				.containsExactly("incomplete-assistant", "string-assistant");
	}

	@Test
	void aCompleteRunHasNoErrorSummary() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), new RecordingApplicator(),
				List.of(new StringTargetLoader()));

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(
				updated -> assertThat(updated.errorSummary()).isNull());
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

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> {
			assertThat(updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED);
			assertThat(updated.errorSummary()).contains("string-assistant apply failed");
		});
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
				List.of(new StringTargetLoader()), openGate(), transactionManager);

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

	// -------------------------------------------------------------------------
	// #279: the project gate
	// -------------------------------------------------------------------------

	/**
	 * #279: the target re-check is not enough. The findings are grouped under the project,
	 * and {@code annotations.grouping_object_id} has no foreign key, so writing them for a
	 * deleted project produces silent orphans rather than a loud FK failure. A run that
	 * finds the project gone must write nothing and record CANCELLED - distinct from
	 * SKIPPED, which means there was nothing to do in the first place.
	 */
	@Test
	void cancelsWhenTheProjectWasDeletedDuringAnalysis() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), applicator,
				List.of(new StringTargetLoader()), gate(AssistantProjectGate.State.GONE));

		worker.run(record.runId());

		assertThat(applicator.appliedResults).isEmpty();
		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> {
			assertThat(updated.status()).isEqualTo(AssistantRunStatus.CANCELLED);
			assertThat(updated.errorSummary()).contains("Project#2").contains("deleted");
		});
	}

	/**
	 * #279: losing the lock race is the same outcome as losing the project - drop the
	 * findings rather than write them without the gate held.
	 */
	@Test
	void cancelsWhenTheProjectRowCannotBeLocked() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), applicator,
				List.of(new StringTargetLoader()), gate(AssistantProjectGate.State.BUSY));

		worker.run(record.runId());

		assertThat(applicator.appliedResults).isEmpty();
		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> {
			assertThat(updated.status()).isEqualTo(AssistantRunStatus.CANCELLED);
			assertThat(updated.errorSummary()).contains("Project#2").contains("locked");
		});
	}

	/** #279: an open gate changes nothing about the happy path. */
	@Test
	void appliesWhenTheGateIsOpen() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		RecordingApplicator applicator = new RecordingApplicator();
		RecordingGate recordingGate = new RecordingGate(AssistantProjectGate.State.OPEN);
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), applicator,
				List.of(new StringTargetLoader()), recordingGate);

		worker.run(record.runId());

		assertThat(recordingGate.acquired).containsExactly(EntityRef.of("Project", 2L));
		assertThat(applicator.appliedResults).hasSize(1);
		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED));
	}

	/**
	 * #279: AnalysisRequest null-checks every field except projectRef, and
	 * AnalysisRequestDispatcher really does pass null when the target has no
	 * ProjectOrDomain. With no project there is nothing to gate on, so the gate must not
	 * be consulted at all and the pre-#279 target-only behaviour stands.
	 */
	@Test
	void skipsWithoutGatingWhenTheRequestHasNoProject() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AnalysisRequest projectless = new AnalysisRequest(EntityRef.of("Goal", 1L), null,
				new UserRef(3L, "human"), new UserRef(4L, "assistant"), "REQUIREMENTS_REVIEW",
				java.util.Locale.ROOT, Map.of());
		AssistantRunRecord record = runStore.queueRun(projectless);
		RecordingApplicator applicator = new RecordingApplicator();
		// A gate that would cancel the run if it were ever asked.
		RecordingGate recordingGate = new RecordingGate(AssistantProjectGate.State.GONE);
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())), applicator,
				List.of(new VanishingTargetLoader()), recordingGate);

		worker.run(record.runId());

		assertThat(recordingGate.acquired).isEmpty();
		assertThat(applicator.appliedResults).isEmpty();
		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SKIPPED));
	}

	/**
	 * #279: {@code TransactionOperations.execute} is declared {@code @Nullable}. apply()
	 * never returns null, so this is a defensive branch rather than a reachable one - but it
	 * decides whether a run is recorded at all, so it is worth pinning: nothing said
	 * otherwise, so the run succeeded.
	 */
	@Test
	void aNullApplyOutcomeIsTreatedAsApplied() {
		InMemoryAssistantRunStore runStore = new InMemoryAssistantRunStore();
		AssistantRunRecord record = runStore.queueRun(request());
		org.springframework.transaction.support.TransactionOperations nullReturningApply =
				new org.springframework.transaction.support.TransactionOperations() {
					@Override
					public <T> T execute(
							org.springframework.transaction.support.TransactionCallback<T> action) {
						action.doInTransaction(
								new org.springframework.transaction.support.SimpleTransactionStatus());
						return null;
					}
				};
		AssistantRunWorker worker = new AssistantRunWorker(runStore,
				new SimpleAssistantRegistry(List.of(new StringAssistant())),
				new RecordingApplicator(), List.of(new StringTargetLoader()),
				java.time.Clock.systemUTC(),
				org.springframework.transaction.support.TransactionOperations.withoutTransaction(),
				nullReturningApply);

		worker.run(record.runId());

		assertThat(runStore.findRun(record.runId())).hasValueSatisfying(updated -> assertThat(
				updated.status()).isEqualTo(AssistantRunStatus.SUCCEEDED));
	}

	private static AssistantProjectGate openGate() {
		return gate(AssistantProjectGate.State.OPEN);
	}

	private static AssistantProjectGate gate(AssistantProjectGate.State state) {
		return projectRef -> state;
	}

	/** Remembers what it was asked to lock, so a test can assert it was never consulted. */
	private static final class RecordingGate implements AssistantProjectGate {
		private final State state;
		private final List<EntityRef> acquired = new java.util.ArrayList<>();

		private RecordingGate(State state) {
			this.state = state;
		}

		@Override
		public State acquire(EntityRef projectRef) {
			acquired.add(projectRef);
			return state;
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

	/** #265: two members; member-a cleans up its untouched findings, member-b leaves it to people. */
	private static final class Composed implements RequelAssistant<String>, ComposedAssistant {
		private String failure;

		@Override
		public String assistantId() {
			return "composed";
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
			throw new UnsupportedOperationException();
		}

		@Override
		public List<RequelAssistant<?>> members() {
			return List.of(new Member("member-a", CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED),
					new Member("member-b", CleanupPolicy.MANUAL));
		}

		@Override
		public List<AssistantResult> analyzeAll(AssistantContext context, Object target)
				throws com.rreganjr.requel.assistant.api.AssistantException {
			if (failure != null) {
				throw new com.rreganjr.requel.assistant.api.AssistantException(failure);
			}
			return List.of(AssistantResult.builder().assistantId("member-a").runId(context.runId())
					.build(), AssistantResult.builder().assistantId("member-b")
							.runId(context.runId()).build());
		}
	}

	private record Member(String assistantId, CleanupPolicy cleanupPolicy)
			implements RequelAssistant<String> {
		@Override
		public Class<String> targetType() {
			return String.class;
		}

		@Override
		public AssistantResult analyze(AssistantContext context, String target) {
			throw new UnsupportedOperationException();
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

	/** #260: a definition-backed assistant reporting some unverified evidence. */
	private static final class DefinitionAssistant implements RequelAssistant<String>,
			com.rreganjr.requel.assistant.core.definition.DefinitionBacked {
		private final com.rreganjr.requel.assistant.core.definition.AssistantDefinition definition;
		private final int unverified;

		DefinitionAssistant(String key, int version, int unverified) {
			this.definition = new com.rreganjr.requel.assistant.core.definition.AssistantDefinition(
					key, key, com.rreganjr.requel.assistant.core.definition.DefinitionKind.REVIEW,
					"REQUIREMENTS_REVIEW", java.util.Set.of(), List.of("entity"), "x", List.of(),
					"RequirementsReviewOutput", "1", true, version,
					com.rreganjr.requel.assistant.core.definition.DefinitionSource.BUNDLED, null,
					null, null);
			this.unverified = unverified;
		}

		@Override
		public com.rreganjr.requel.assistant.core.definition.AssistantDefinition definition() {
			return definition;
		}

		@Override
		public String assistantId() {
			return definition.key();
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
			return AssistantResult.builder().assistantId(assistantId()).runId(context.runId())
					.metadata(Map.of(AssistantRunWorker.EVIDENCE_UNVERIFIED, unverified)).build();
		}
	}

	/** #355: a result with a blank summary. */
	private static final class BlankSummaryAssistant implements RequelAssistant<String> {
		@Override
		public String assistantId() {
			return "blank-summary-assistant";
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
			return AssistantResult.builder().assistantId(assistantId()).runId(context.runId())
					.summary("   ").build();
		}
	}

	/** Returns a result that says its Text wasn't analyzed (#268). */
	private static final class IncompleteAssistant implements RequelAssistant<String> {
		@Override
		public String assistantId() {
			return "incomplete-assistant";
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
			return AssistantResult.builder().assistantId(assistantId()).runId(context.runId())
					.summary(target).metadata(java.util.Map.of("incomplete", Boolean.TRUE,
							"failedProperties", List.of("Text")))
					.build();
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

	/** #363: records when it prepares and when it calls; either can be made to fail. */
	private static final class Staged implements RequelAssistant<String>, StagedAssistant {
		private final List<String> phases;
		private String prepareFailure;
		private String callFailure;

		Staged(List<String> phases) {
			this.phases = phases;
		}

		@Override
		public String assistantId() {
			return "staged-assistant";
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
			throw new AssertionError("the worker runs a staged assistant through prepare");
		}

		@Override
		public Stage prepare(AssistantContext context, Object target) {
			phases.add("prepare");
			if (prepareFailure != null) {
				throw new IllegalStateException(prepareFailure);
			}
			String text = (String) target;
			return () -> {
				phases.add("call");
				if (callFailure != null) {
					throw new com.rreganjr.requel.assistant.api.AssistantException(callFailure);
				}
				return List.of(AssistantResult.builder().assistantId(assistantId())
						.runId(context.runId()).summary("called with " + text).build());
			};
		}
	}
}
