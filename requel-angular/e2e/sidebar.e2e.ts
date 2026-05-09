import { test, expect } from './fixtures/auth';
import {
  createProject, createGoal, createActor, createStory,
  exportProjectXml,
} from './fixtures/api-helper';
import { SidebarPage } from './pages/SidebarPage';
import * as fs from 'fs';

/**
 * Tests for the global app sidebar (`app-sidebar-nav`).
 *
 * Currently covers:
 *   - Import project XML via the sidebar Import button → the new project
 *     appears in the sidebar tree, and its entity-group children render
 *     once the node is expanded.
 *   - SSE-driven tree refresh: when a project-scoped command fires in
 *     the same context (or any other context sharing the backend), the
 *     sidebar reloads `/api/projects` without a manual page refresh.
 *     Backend wiring: `CommandController.publishProjectChangedIfScoped`
 *     broadcasts `Project:0` (PROJECT_BROADCAST_ID); auth-layout's
 *     `connect(['Project:0'])` is the matching client subscription;
 *     `SidebarNavComponent.ngOnInit` re-runs `loadProjects()` on every
 *     `targetType === 'Project'` envelope.
 *
 *     The SSE-readiness wait is on the GET `/events/stream` *response*
 *     (not the request) — the URL-param subscription is registered
 *     server-side before headers are returned, so once we've seen the
 *     200 we know any subsequent `Project` broadcast will reach this
 *     client. Waiting only on the request being sent left a race where
 *     the `EditGoal` broadcast could fire before subscription
 *     registration completed.
 */
test.describe('Sidebar', () => {

  test('import project XML via sidebar → project appears in tree with entity groups', async ({ adminContext, request }, testInfo) => {
    test.setTimeout(60_000);

    // ----- Build a populated source project, then export it to XML so we
    // have a real, validate-able file to feed the sidebar import input. We
    // can't use the placeholder import-project.xml fixture here because the
    // assertion explicitly cares about entity-group children rendering with
    // non-zero counts after import.
    const nonce = Date.now();
    const sourceName = `e2e-sidebar-import-${nonce}`;
    const goalName = `Sidebar Import Goal ${nonce}`;
    const actorName = `Sidebar Import Actor ${nonce}`;
    const storyName = `Sidebar Import Story ${nonce}`;

    await createProject(request, sourceName, 'Sidebar import source project');
    await createGoal(request, sourceName, goalName, 'goal text');
    await createActor(request, sourceName, actorName, 'actor text');
    await createStory(request, sourceName, storyName, 'Success', 'story text', actorName);

    const xml = await exportProjectXml(request, sourceName);
    const exportPath = testInfo.outputPath('sidebar-import.xml');
    fs.writeFileSync(exportPath, xml, 'utf-8');

    // ----- Drive the sidebar import via the UI -----
    // The sidebar is visible on any authenticated route — /projects is the
    // standard landing page after login, so we use it as our entry point.
    const page = await adminContext.newPage();
    const sidebar = new SidebarPage(page);

    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/projects') && r.status() === 200),
      page.goto('/projects'),
    ]);
    await expect(sidebar.tree()).toBeVisible();

    // Wait for the ImportProject command response so we can read back the
    // server-assigned project name (the backend may rename on collision).
    const importResponsePromise = page.waitForResponse(response =>
      response.url().includes('/api/commands/ImportProject') &&
      response.request().method() === 'POST'
    );
    await sidebar.importProjectFromFile(exportPath);
    const importResponse = await importResponsePromise;
    expect(importResponse.ok(), 'sidebar import command returns HTTP success').toBeTruthy();
    const importResult = await importResponse.json() as { entity?: { name?: string } };
    const importedName = importResult.entity?.name;
    expect(importedName, 'import command returns the imported project name').toBeTruthy();

    // ----- "project appears in list" — sidebar tree shows the new project -----
    await sidebar.expectProjectInTree(importedName!);

    // ----- "entities visible in tree" — expand the imported project and
    // verify its entity-group children render with non-zero counts that
    // match what we exported (1 goal, 1 actor, 1 story). The sidebar
    // formats counts inline as "Goals (N)", "Actors (N)", "Stories (N)".
    await sidebar.expandProject(importedName!);
    await sidebar.expectEntityGroup(importedName!, /^Goals \(1\)$/);
    await sidebar.expectEntityGroup(importedName!, /^Actors \(1\)$/);
    await sidebar.expectEntityGroup(importedName!, /^Stories \(1\)$/);

    await page.close();
  });

  test('edit a goal via API → sidebar tree refreshes counts via SSE without a full page reload', async ({ adminContext, request }) => {
    test.setTimeout(30_000);

    // Project starts with zero goals so the SSE-driven refresh is observable
    // as a clean "Goals (0)" → "Goals (1)" count change.
    const nonce = Date.now();
    const projectName = `e2e-sse-sidebar-${nonce}`;
    const goalName = `SSE Sidebar Goal ${nonce}`;
    await createProject(request, projectName, 'SSE-driven sidebar refresh source');

    const page = await adminContext.newPage();
    const sidebar = new SidebarPage(page);

    // CDP attached BEFORE navigation so the diagnostic timeline below
    // captures the FULL lifecycle — initial /api/projects fetches, the
    // /events/stream connection setup (and its requestId, which we need
    // to attribute later `Network.dataReceived` chunks back to the SSE
    // stream), keep-alives, the createGoal POST, and the SSE-triggered
    // refresh fetch. Network.enable starts emitting events from this
    // call onward; nothing is delivered retroactively, so the order
    // matters.
    const cdp = await page.context().newCDPSession(page);
    await cdp.send('Network.enable');

    type NetEvent = { kind: string; label: string; method?: string; status?: number; t: number };
    const start = Date.now();
    const netLog: NetEvent[] = [];
    const interesting = (url: string) =>
      url.includes('/api/') || url.includes('/events/');
    // requestId → friendly label (last path segment). Lets us re-attribute
    // dataReceived/loadingFailed events (which only carry requestId) back
    // to "the /events/stream connection" or "/api/projects fetch", which
    // is the whole reason this diagnostic exists.
    const reqLabels = new Map<string, string>();
    const labelFor = (url: string): string => {
      try {
        const u = new URL(url);
        // For /events/stream specifically, preserve the query string so we
        // can verify the `subscribe=Project:0` param is actually reaching
        // the server. For everything else, pathname is enough noise.
        if (u.pathname === '/api/events/stream') {
          return u.pathname + (u.search || '');
        }
        return u.pathname;
      } catch {
        return url;
      }
    };
    const labelById = (requestId: string): string =>
      reqLabels.get(requestId) ?? `<reqId=${requestId}>`;

    const onRequestWillBeSent = (event: { requestId: string; request: { url: string; method: string }; type?: string }) => {
      if (interesting(event.request.url)) {
        const label = labelFor(event.request.url);
        reqLabels.set(event.requestId, label);
        netLog.push({
          kind: `req(${event.type ?? '?'})`,
          label,
          method: event.request.method,
          t: Date.now() - start,
        });
      }
    };
    const onResponseReceived = (event: { requestId: string; response: { url: string; status: number; fromDiskCache?: boolean; fromServiceWorker?: boolean } }) => {
      if (interesting(event.response.url)) {
        netLog.push({
          kind: `resp${event.response.fromDiskCache ? '(cache)' : event.response.fromServiceWorker ? '(sw)' : ''}`,
          label: labelFor(event.response.url),
          status: event.response.status,
          t: Date.now() - start,
        });
      }
    };
    const onDataReceived = (event: { requestId: string; dataLength: number }) => {
      // We only care about chunks on requests we already labeled as
      // interesting — chunks on the SSE stream are the diagnostic signal
      // for "is the event channel actually pushing bytes after EditGoal?"
      if (event.dataLength > 0 && reqLabels.has(event.requestId)) {
        netLog.push({
          kind: `data(${event.dataLength}b)`,
          label: labelById(event.requestId),
          t: Date.now() - start,
        });
      }
    };
    const onLoadingFailed = (event: { requestId: string; errorText: string; canceled?: boolean }) => {
      if (reqLabels.has(event.requestId)) {
        netLog.push({
          kind: `failed${event.canceled ? '(canceled)' : ''}`,
          label: `${labelById(event.requestId)} — ${event.errorText}`,
          t: Date.now() - start,
        });
      }
    };

    cdp.on('Network.requestWillBeSent', onRequestWillBeSent);
    cdp.on('Network.responseReceived', onResponseReceived);
    cdp.on('Network.dataReceived', onDataReceived);
    cdp.on('Network.loadingFailed', onLoadingFailed);

    const mark = (note: string) => {
      netLog.push({ kind: 'MARK', label: note, t: Date.now() - start });
    };

    // Wait for both /api/projects (initial sidebar load) AND /events/stream
    // RESPONSE (not just the request — see comment below). The Project:0
    // subscription rides in the URL params of `GET /events/stream`, so by
    // the time the server returns 200 OK headers it has parsed the param,
    // created the session, and registered the subscription.
    mark('navigating to /projects');
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/projects') && r.status() === 200),
      page.waitForResponse(
        r => r.url().includes('/events/stream') &&
             !r.url().includes('/events/stream/subscriptions') &&
             r.status() === 200
      ),
      page.goto('/projects'),
    ]);
    mark('initial /api/projects + /events/stream both 200');
    await expect(sidebar.tree()).toBeVisible();
    await sidebar.expectProjectInTree(projectName);
    mark('expectProjectInTree settled');

    let resolveSidebarRefresh!: () => void;
    let rejectSidebarRefresh!: (err: Error) => void;
    const sidebarRefresh = new Promise<void>((resolve, reject) => {
      resolveSidebarRefresh = resolve;
      rejectSidebarRefresh = reject;
    });

    let armed = false;
    let resolved = false;
    const refreshDetector = (event: { request: { url: string; method: string } }) => {
      if (!armed || resolved) return;
      if (event.request.method !== 'GET') return;
      try {
        if (new URL(event.request.url).pathname === '/api/projects') {
          resolved = true;
          resolveSidebarRefresh();
        }
      } catch { /* malformed URL — skip */ }
    };
    cdp.on('Network.requestWillBeSent', refreshDetector);

    // Belt-and-braces timeout that produces a real network timeline when
    // the SSE chain is broken — pinpointing whether the /events/stream
    // connection died, no chunk arrived after EditGoal, no fetch was
    // initiated, etc.
    const refreshTimeout = setTimeout(() => {
      if (!resolved) {
        resolved = true;
        const lines = netLog.map(e => {
          const m = e.method ? ` [${e.method}]` : '';
          const s = e.status != null ? ` → ${e.status}` : '';
          return `  +${e.t.toString().padStart(5)}ms  ${e.kind.padEnd(14)}${m}${s}  ${e.label}`;
        });
        rejectSidebarRefresh(new Error(
          'sidebar did not fire GET /api/projects within 15 s after EditGoal.\n\n' +
          'SSE chain (server publishProjectChangedIfScoped → /events/stream →\n' +
          'EventStreamService → sidebar events$ subscriber → loadProjects).\n\n' +
          'Network timeline (only /api/* and /events/* shown — t=0 is when\n' +
          'CDP attached, BEFORE page.goto. MARKs annotate test-side phases):\n' +
          lines.join('\n') + '\n\n' +
          'How to read this:\n' +
          '  * If you see no `data(...)` rows on `/events/stream` AFTER\n' +
          '    the `EditGoal-completed` MARK, the SSE chunk never reached\n' +
          '    the browser → server didn\'t broadcast OR the connection\n' +
          '    dropped (look for `failed`).\n' +
          '  * If you DO see `data(...)` rows AFTER the MARK but no\n' +
          '    subsequent `req(...) /api/projects`, the chunk arrived but\n' +
          '    sidebar-nav.ts events$ subscriber didn\'t fire loadProjects\n' +
          '    → JSON parse failed, targetType filter missed, subscription\n' +
          '    leaked, etc.'
        ));
      }
    }, 15_000);

    armed = true;
    mark('detector armed');

    // Trigger an EditGoal via the API — this is the "edit a goal in one
    // context" simulation. The goal is created, the backend runs both
    // publishEntityChangedIfPresent (Goal:newId — sidebar isn't subscribed
    // to that, but the editor in the multi-context test in
    // sse-refresh.e2e.ts is) and publishProjectChangedIfScoped (Project:0
    // — the sidebar IS).
    mark('EditGoal-fired');
    await createGoal(request, projectName, goalName, 'SSE refresh test goal');
    mark('EditGoal-completed');

    try {
      await sidebarRefresh;
    } finally {
      clearTimeout(refreshTimeout);
      cdp.off('Network.requestWillBeSent', refreshDetector);
      cdp.off('Network.requestWillBeSent', onRequestWillBeSent);
      cdp.off('Network.responseReceived', onResponseReceived);
      cdp.off('Network.dataReceived', onDataReceived);
      cdp.off('Network.loadingFailed', onLoadingFailed);
    }

    // `loadProjects()` rebuilds the tree, but reapplies the persisted set of
    // expanded project names from localStorage
    // (`requel_sidebar_expanded_projects`) so any user-expanded project
    // would survive the rebuild. In this test the user never expanded the
    // project before the SSE refresh, so the rebuild leaves it collapsed
    // and we expand it here for the first time to assert the count.
    // (See the dedicated "expanded state persists" test below for a direct
    // check that an expansion DOES survive an SSE-driven rebuild.)
    // We use Playwright's auto-retrying `expect()` for the count check —
    // not a `toPass` closure. Auto-retry here covers only the trailing
    // microtask gap between the GET response landing in `listProjects()`'s
    // `await` and Angular flushing the signal-driven tree rebuild — at
    // most one or two change-detection ticks. It is NOT polling for the
    // SSE chain itself; that's already happened by the time we get here.
    await sidebar.expandProject(projectName);
    await sidebar.expectEntityGroup(projectName, /^Goals \(1\)$/);

    await page.close();
  });

  test('expanded project state persists across page reload and SSE refresh', async ({ adminContext, request }) => {
    test.setTimeout(30_000);

    // Two projects so we can assert the expanded one stays open and the
    // untouched one stays closed across both a page reload and an
    // SSE-driven `loadProjects()` rebuild.
    const nonce = Date.now();
    const expandedName = `e2e-sidebar-persist-open-${nonce}`;
    const collapsedName = `e2e-sidebar-persist-closed-${nonce}`;
    await createProject(request, expandedName, 'Sidebar persistence: expanded');
    await createProject(request, collapsedName, 'Sidebar persistence: collapsed');

    const page = await adminContext.newPage();
    const sidebar = new SidebarPage(page);

    // ---- 1. expand one project, leave the other collapsed --------------
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/projects') && r.status() === 200),
      page.goto('/projects'),
    ]);
    await expect(sidebar.tree()).toBeVisible();
    await sidebar.expectProjectInTree(expandedName);
    await sidebar.expectProjectInTree(collapsedName);
    await sidebar.expandProject(expandedName);

    // Sanity-check the persisted set — the localStorage entry is the
    // contract that makes the rest of this test work. If the component
    // ever stops writing the key, this assertion fails fast with a clear
    // signal of where the persistence chain broke.
    const persisted = await page.evaluate(
      () => localStorage.getItem('requel_sidebar_expanded_projects')
    );
    expect(persisted, 'localStorage records the expanded project').toBeTruthy();
    const persistedNames = JSON.parse(persisted!) as string[];
    expect(persistedNames).toContain(expandedName);
    expect(persistedNames).not.toContain(collapsedName);

    // ---- 2. full page reload — expand state should restore -------------
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/api/projects') && r.status() === 200),
      page.reload(),
    ]);
    await expect(sidebar.tree()).toBeVisible();
    await sidebar.expectProjectExpanded(expandedName);
    await expect(sidebar.projectNode(collapsedName))
      .toHaveAttribute('aria-expanded', 'false');

    // ---- 3. SSE-driven loadProjects() rebuild — expand state preserved -
    // Editing a goal in *either* project triggers a Project:0 broadcast,
    // so the sidebar's events$ subscriber fires loadProjects() and the
    // tree is rebuilt from scratch. Without persistence both nodes would
    // reset to `expanded: false`.
    const refreshFired = page.waitForResponse(r =>
      r.url().includes('/api/projects') &&
      r.request().method() === 'GET' &&
      r.status() === 200
    );
    await createGoal(request, collapsedName, `Persistence Goal ${nonce}`, 'goal text');
    await refreshFired;

    await sidebar.expectProjectExpanded(expandedName);
    await expect(sidebar.projectNode(collapsedName))
      .toHaveAttribute('aria-expanded', 'false');

    await page.close();
  });

});
