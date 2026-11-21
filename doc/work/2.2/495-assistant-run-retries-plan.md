# #495 — Assistant runs fail silently when they race each other or a delete

https://github.com/rreganjr/Requel/issues/495

Branch `495-assistant-run-retries` from `release/2.2`, PR to `release/2.2`. Size 3.

## Summary

`AssistantRunWorker` runs a queued analysis in phases: analyze in one transaction, make any model
calls with none open (#363), then apply the findings in one short transaction that first locks the
project row (#279) and re-checks the target. The apply phase had no retry:

- It writes through the command handler, but `RetryOnLockFailuresCommandHandler` deliberately
  doesn't retry inside an open transaction (#247), so a deadlock, lock timeout or stale version
  propagates.
- The apply loop caught each assistant's failure as a "problem", but the failure had already
  marked the transaction rollback-only, so the commit threw and the run was marked FAILED.
- Two runs offering the same position (shared by text within a project) collide on its version;
  a scenario deleted during the apply deadlocks against the inserts or fails a foreign key.

Nothing retried, so the findings were lost until the entity was next edited, and the user saw no
error.

## Locked decisions (2026-11-21)

1. **The apply phase is retried, up to 3 attempts** (the command handler's count), with a 50–200 ms
   random pause that grows per attempt. Each attempt is a fresh transaction: it takes the project
   lock again, re-checks the target and re-reads what it writes. The analysis and model calls are
   never repeated.
2. **Retried:** deadlocks and lock timeouts, stale and optimistic-lock versions (including
   `StaleVersionException`), and foreign-key violations. The cause chain is checked, so a wrapped
   failure still counts.
3. **A retryable failure in one assistant's apply aborts the attempt** (rethrown) rather than being
   recorded as a PARTIAL problem. Other per-assistant failures keep today's PARTIAL handling.
4. **A target deleted during the run** is found missing by the retry, and the run is SKIPPED.
5. **Logging** matches the command handler: a line per retry, and "succeeded after retry" when one
   recovers, so the issue's e2e log check (`Deadlock found` equals `succeeded after retry`, no
   `Assistant run failed`) reads the same for both. CI's e2e job runs on MySQL and keeps the app log.

## Steps

1. `RetryableApplyFailure.isRetryable(Throwable)` (assistant-core): walks the cause chain for
   `EntityLockException` (so `StaleVersionException`), Spring's `ConcurrencyFailureException` and
   `DataIntegrityViolationException`, the JPA lock exceptions, `SQLIntegrityConstraintViolation`,
   an `SQLException` with SQLState `40001` or MySQL error 1213/1205, and Hibernate's lock, stale
   and constraint exceptions (matched by class name; this module doesn't compile against
   Hibernate).
2. `AssistantRunWorker`: `applyWithRetry` around the apply transaction; the per-assistant catch
   rethrows retryable failures; a test-replaceable `ApplyBackoff`.

## Test plan

- **`AssistantRunWorkerTest`:** a stale-version failure then success ends SUCCEEDED with two
  apply attempts; three retryable failures end FAILED; a target that's gone on the retry ends
  SKIPPED; a non-retryable failure isn't retried; a retryable failure from one assistant's apply
  aborts the attempt instead of becoming PARTIAL.
- **`RetryableApplyFailureTest`:** each retryable type, directly and wrapped; ordinary failures
  are not retryable.
- **`AssistantRunRetryMySqlIT`** (Testcontainers MySQL). The worker is built around the
  application's run store, applicator, target loaders, gate and transaction manager, with a test
  assistant offering one issue and a shared "Do nothing." position. Its applicator is wrapped so
  a test can act from another connection while the apply transaction is open:
  - runs on four goals of one project offering the shared position, started together, three
    rounds: every run SUCCEEDED and the position is on every issue. (Applies in one project
    queue on the project gate, so this guards the shared-position path rather than forcing a
    collision.)
  - the shared position's version bumped from another connection after the apply has read it:
    the commit fails stale, the retry succeeds (two attempts);
  - the goal deleted from another connection before the apply writes: the `goals_annotations`
    insert fails its foreign key, the retry finds the goal gone, the run is SKIPPED.
  - Without the retry, the last two fail (checked against a local MySQL 8.0).
- Verify gate: `mvn clean verify`; then the e2e log check on the CI run.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| MySQL IT: two runs offering the same position at once both apply their findings | `AssistantRunRetryMySqlIT` |
| A run whose target is deleted mid-run ends without a failure | Decision 4, `AssistantRunWorkerTest`, `AssistantRunRetryMySqlIT` |
| The e2e log shows `Deadlock found` = `succeeded after retry`, no `Assistant run failed` | Decision 5, checked on the PR's CI e2e log |
