import { test, expect } from './fixtures/auth';
import {
  createProject, createGoal, updateGoal, createActor, addGoalToActor,
} from './fixtures/api-helper';
import { gotoAndWaitForGet } from './helpers/navigation';

/**
 * Live-refresh tests for the SSE event stream wired through
 * `EventStreamService` and the per-editor `addSubscription(...)` calls.
 *
 * Backend wiring (see `service-impl/.../CommandController.java`):
 *   - `publishEntityChangedIfPresent` broadcasts `<EntityType>:<entityId>`
 *     when a command result DTO has an `id()` accessor.
 *   - `publishProjectChangedIfScoped` broadcasts `Project:0` (the
 *     PROJECT_BROADCAST_ID sentinel) for any project-scoped command.
 *
 * Client wiring:
 *   - `auth/layout.ts` opens the connection with `connect(['Project:0'])`.
 *   - `goals/goal-editor.ts` adds a `Goal:<goalId>` subscription after
 *     `loadGoal()` resolves and re-runs `loadGoal(true)` on matching
 *     events (skipping the refresh when the local form has unsaved
 *     changes — see `hasUnsavedChanges()`).
 *
 * The single test in this file simulates the multi-context scenario by
 * driving the "second context" via the API rather than spawning a real
 * second browser context. From the SSE pipeline's perspective both look
 * identical — both produce backend command broadcasts that the
 * already-open editor in the first context must react to.
 */
test.describe('SSE live refresh', () => {

  test('open goal editor in context A; edit same goal via API → context A name input refreshes via SSE without page reload', async ({ adminContext, request }) => {
    test.setTimeout(30_000);

    const nonce = Date.now();
    const projectName = `e2e-sse-multi-${nonce}`;
    const originalName = `SSE Multi Original ${nonce}`;
    const updatedName = `SSE Multi Updated ${nonce}`;
    await createProject(request, projectName, 'SSE multi-context source');
    const goal = await createGoal(request, projectName, originalName, 'goal text');

    const page = await adminContext.newPage();

    // Listen for the goal editor's `addSubscription('Goal', goalId)` call
    // BEFORE driving navigation, so we can be sure the editor has registered
    // a server-side subscription before we trigger the simulated context-B
    // edit. The POST is a no-op until the SSE Session event has populated
    // a sessionId (see `EventStreamService.addSubscription`), so a 200
    // response on this POST also implicitly confirms the SSE connection
    // is established.
    const subscriptionPromise = page.waitForResponse(
      r => r.url().includes('/events/stream/subscriptions') &&
           r.request().method() === 'POST' &&
           r.status() === 200,
      { timeout: 15_000 }
    );

    // gotoAndWaitForGet, not goto: page.goto() resolves on document load, well before the goal
    // detail fetch returns, and #185 now gates the form on that fetch. The assertion below
    // happens to be safe either way - toHaveValue(originalName) retries and cannot pass against
    // an absent element - but waiting explicitly is the point of the helper: the test should say
    // what it depends on rather than rely on a locator to paper over it.
    await gotoAndWaitForGet(
      page,
      `/projects/${encodeURIComponent(projectName)}/goals/${goal.id}`,
      response => response.url().includes(`/goals/${goal.id}`)
    );
    // Since #158 the goal form is app-field rows with generated ids — locate by testid.
    const nameInput = page.getByTestId('goal-name');
    await expect(nameInput).toHaveValue(originalName);
    await subscriptionPromise;

    // Sentinel pinned in the page's window object — if a full page reload
    // happens during the SSE refresh, the property gets cleared. We assert
    // it survives at the end as evidence that the form update was driven
    // by SSE-fed change-detection and not by navigation/reload.
    await page.evaluate(() => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (globalThis as any).__sseRefreshSentinel = Date.now();
    });

    // Drive the simulated context-B edit. The backend will fire both
    // `Goal:<goalId>` (the editor's subscription target) and `Project:0`
    // (the sidebar broadcast). Only the first matters for this test —
    // it's what the editor's events$ subscriber filters on.
    await updateGoal(request, goal, updatedName, 'goal text');

    // The goal editor's events$ handler calls `loadGoal(true)` on the
    // matching envelope, which re-fetches the goal and assigns the new
    // name to `this.name` — and PrimeNG's [(ngModel)] binding propagates
    // that into the visible input. Toleration window covers SSE network
    // hop + reactive change detection.
    await expect(nameInput).toHaveValue(updatedName, { timeout: 10_000 });

    // Sentinel still set ⇒ no full page reload occurred during the test.
    const sentinelStillSet = await page.evaluate(() => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      return (globalThis as any).__sseRefreshSentinel != null;
    });
    expect(sentinelStillSet, 'sentinel survives, proving no page reload happened').toBeTruthy();

    await page.close();
  });

  test('open actor editor in context A; associate a goal via API → context A goals table refreshes via targeted SSE (issue #178)', async ({ adminContext, request }) => {
    test.setTimeout(30_000);

    const nonce = Date.now();
    const projectName = `e2e-sse-assoc-${nonce}`;
    const actorName = `SSE Assoc Actor ${nonce}`;
    const goalName = `SSE Assoc Goal ${nonce}`;
    await createProject(request, projectName, 'SSE association source');
    const actor = await createActor(request, projectName, actorName, 'actor text');
    const goal = await createGoal(request, projectName, goalName, 'goal text');

    const page = await adminContext.newPage();

    // Same rationale as the goal test above: wait for the editor's Actor:<id> subscription POST
    // (which also confirms the SSE connection is live) before firing the simulated context-B write.
    const subscriptionPromise = page.waitForResponse(
      r => r.url().includes('/events/stream/subscriptions') &&
           r.request().method() === 'POST' &&
           r.status() === 200,
      { timeout: 15_000 }
    );

    await gotoAndWaitForGet(
      page,
      `/projects/${encodeURIComponent(projectName)}/actors/${actor.id}`,
      response => response.url().includes(`/actors/${actor.id}`)
    );
    await expect(page.getByTestId('actor-name')).toHaveValue(actorName);
    await subscriptionPromise;

    // No goal on the actor yet.
    await expect(page.getByTestId('actor-goal-link')).toHaveCount(0);

    await page.evaluate(() => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (globalThis as any).__sseAssocSentinel = Date.now();
    });

    // Simulated context-B write: AddGoalToGoalContainer. Before #178 this command registered no
    // result extractor, so no targeted Actor:<id> event fired and this open editor never refreshed.
    // Now the merged container's detail DTO is the result, so the editor gets a targeted event.
    await addGoalToActor(request, projectName, actor.id, goal.id);

    // The actor editor's events$ handler calls loadActor(false); the goals table now shows the goal.
    await expect(page.getByTestId('actor-goal-link')).toHaveText(goalName, { timeout: 10_000 });

    // Sentinel still set ⇒ no full page reload occurred — the update was SSE-driven change detection.
    const sentinelStillSet = await page.evaluate(() => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      return (globalThis as any).__sseAssocSentinel != null;
    });
    expect(sentinelStillSet, 'sentinel survives, proving no page reload happened').toBeTruthy();

    await page.close();
  });

  test('originating context adds a goal via the UI and issues no follow-up actor GET (issue #180)', async ({ adminContext, request }) => {
    test.setTimeout(30_000);

    const nonce = Date.now();
    const projectName = `e2e-assoc-noget-${nonce}`;
    const actorName = `NoGet Actor ${nonce}`;
    const goalName = `NoGet Goal ${nonce}`;
    await createProject(request, projectName, 'no-follow-up-GET source');
    const actor = await createActor(request, projectName, actorName, 'actor text');
    await createGoal(request, projectName, goalName, 'goal text');

    const page = await adminContext.newPage();

    const subscriptionPromise = page.waitForResponse(
      r => r.url().includes('/events/stream/subscriptions') &&
           r.request().method() === 'POST' &&
           r.status() === 200,
      { timeout: 15_000 }
    );

    await gotoAndWaitForGet(
      page,
      `/projects/${encodeURIComponent(projectName)}/actors/${actor.id}`,
      response => response.url().includes(`/actors/${actor.id}`)
    );
    await expect(page.getByTestId('actor-name')).toHaveValue(actorName);
    await subscriptionPromise;

    // From here on, any GET of this actor's detail is a follow-up read we want to prove does NOT
    // happen: #180 has the editor consume the association response, and #178 excludes the acting
    // session from the targeted SSE event, so the originating context must not re-read the actor.
    const detailPath = `/projects/${encodeURIComponent(projectName)}/actors/${actor.id}`;
    let followUpActorGets = 0;
    page.on('request', req => {
      if (req.method() === 'GET' && req.url().includes(detailPath)) {
        followUpActorGets += 1;
      }
    });

    // Add the goal through the UI so the command carries this context's X-Session-Id.
    await page.getByTestId('actor-add-goal').click();
    await page.getByTestId('entity-selector-row').first().click();

    // The goal lands in the table from result.entity...
    await expect(page.getByTestId('actor-goal-link')).toHaveText(goalName, { timeout: 10_000 });

    // ...and the acting context issued no follow-up actor GET to get there.
    expect(followUpActorGets, 'originating context must not re-read the actor after its own association').toBe(0);

    await page.close();
  });

});
