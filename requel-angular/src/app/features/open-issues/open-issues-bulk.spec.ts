import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ConfirmationService, Confirmation } from 'primeng/api';
import { BehaviorSubject } from 'rxjs';
import { OpenIssuesComponent } from './open-issues';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

// #507: bulk actions on the Open Issues page.
const ISSUES = [
  { issueId: 1, issueText: 'Possible misspelling: systm', mustBeResolved: false, severity: 'LOW' as const,
    entityType: 'Goal', entityId: 10, entityName: 'Goal A' },
  { issueId: 2, issueText: 'Possible misspelling: costz', mustBeResolved: false, severity: 'LOW' as const,
    entityType: 'Goal', entityId: 11, entityName: 'Goal B' },
  { issueId: 3, issueText: 'Measured how?', mustBeResolved: true, severity: 'MEDIUM' as const,
    entityType: 'Story', entityId: 20, entityName: 'Story C' },
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('OpenIssuesComponent bulk actions (#507)', () => {
  let http: HttpTestingController;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: OpenIssuesComponent;
  let confirmations: Confirmation[];

  async function load(rows = ISSUES) {
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('open-issues')).flush(rows);
    await flush();
    fixture.detectChanges();
  }

  const el = () => fixture.nativeElement as HTMLElement;
  const byId = (id: string) => el().querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const button = (id: string) => byId(id)!.querySelector('button') as HTMLButtonElement;

  function select(ids: number[]) {
    comp.selected.set(comp.issues().filter(i => ids.includes(i.issueId)));
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [OpenIssuesComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: new BehaviorSubject(convertToParamMap({ name: 'proj1' })).asObservable() } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OpenIssuesComponent);
    comp = fixture.componentInstance;
    confirmations = [];
    const confirmer = fixture.debugElement.injector.get(ConfirmationService);
    vi.spyOn(confirmer, 'confirm').mockImplementation(((c: Confirmation) => {
      confirmations.push(c);
      return confirmer;
    }) as typeof confirmer.confirm);
  });

  afterEach(() => http.verify());

  it('shows a checkbox per row and a select-all for the page', async () => {
    await load();
    expect(el().querySelectorAll('.dt-row p-tablecheckbox, .dt-row p-tableCheckbox').length).toBe(3);
    expect(el().querySelector('th.dt-select-col')).not.toBeNull();
    expect(byId('bulk-toolbar')).toBeNull();
  });

  it('shows the bulk toolbar with the count while issues are selected, and hides it when cleared', async () => {
    await load();
    select([1, 2]);
    expect(byId('bulk-count')!.textContent).toContain('2 selected');
    button('bulk-clear').click();
    fixture.detectChanges();
    expect(byId('bulk-toolbar')).toBeNull();
    expect(comp.selected()).toEqual([]);
  });

  it('asks first, naming the action and the count', async () => {
    await load();
    select([1, 2]);
    button('bulk-apply-fix').click();
    expect(confirmations).toHaveLength(1);
    expect(confirmations[0].header).toBe('Apply suggested fix: 2 issues?');
    expect(confirmations[0].acceptLabel).toBe('Apply suggested fix');
    http.expectNone(r => r.url.includes('/commands/'));
  });

  it('sends the selected ids, then shows the summary, reloads and clears the selection', async () => {
    await load();
    select([1, 2, 3]);
    button('bulk-apply-fix').click();
    confirmations[0].accept!();

    const req = http.expectOne(r => r.url.endsWith('/commands/BulkResolveIssues'));
    expect(req.request.body.action).toBe('APPLY_FIX');
    expect(req.request.body.projectName).toBe('proj1');
    expect([...req.request.body.issueIds].sort()).toEqual([1, 2, 3]);
    req.flush({ success: true, entityType: 'BulkResolveIssues', error: null, violations: null, entity: {
      action: 'APPLY_FIX', done: 1, skipped: 2, refused: 0, failed: 0, outcomes: [
        { issueId: 1, outcome: 'DONE', positionId: 7 },
        { issueId: 2, outcome: 'SKIPPED_AMBIGUOUS', reason: '2 suggested fixes; choose one on the issue.' },
        { issueId: 3, outcome: 'SKIPPED_NOT_APPLICABLE', reason: 'No suggested fix.' },
      ] } });
    await flush();
    http.expectOne(r => r.url.includes('open-issues')).flush([ISSUES[1], ISSUES[2]]);
    await flush();
    fixture.detectChanges();

    expect(byId('bulk-summary-counts')!.textContent).toContain('1 done, 2 skipped, 0 refused, 0 failed');
    const outcomes = Array.from(el().querySelectorAll('[data-testid="bulk-outcome"]')).map(o => o.textContent!.replace(/\s+/g, ' ').trim());
    expect(outcomes[0]).toBe('Done: Possible misspelling: systm');
    expect(outcomes[1]).toContain('Skipped, more than one fix: Possible misspelling: costz');
    expect(comp.selected()).toEqual([]);
    expect(byId('bulk-toolbar')).toBeNull();
    expect(comp.issues()).toHaveLength(2);
  });

  it('sends the severity with Set severity, which needs one picked', async () => {
    await load();
    select([3]);
    expect(button('bulk-set-severity').disabled).toBe(true);
    comp.bulkSeverity.set('HIGH');
    fixture.detectChanges();
    button('bulk-set-severity').click();
    expect(confirmations[0].header).toBe('Set severity High: 1 issue?');
    confirmations[0].accept!();
    const req = http.expectOne(r => r.url.endsWith('/commands/BulkResolveIssues'));
    expect(req.request.body).toEqual({ projectName: 'proj1', issueIds: [3], action: 'SET_SEVERITY', severity: 'HIGH' });
    req.flush({ success: true, entityType: null, error: null, violations: null,
      entity: { action: 'SET_SEVERITY', done: 1, skipped: 0, refused: 0, failed: 0, outcomes: [{ issueId: 3, outcome: 'DONE' }] } });
    await flush();
    http.expectOne(r => r.url.includes('open-issues')).flush(ISSUES);
    await flush();
  });

  it("shows the server's error and keeps the selection when the request fails", async () => {
    await load();
    select([1]);
    button('bulk-mark-advisory').click();
    confirmations[0].accept!();
    http.expectOne(r => r.url.endsWith('/commands/BulkResolveIssues'))
      .flush({ success: false, error: 'Issue 9 belongs to another project.' }, { status: 400, statusText: 'Bad Request' });
    await flush();
    fixture.detectChanges();
    expect(byId('open-issues-error')!.textContent).toContain('belongs to another project');
    expect(comp.selected()).toHaveLength(1);
  });

  it('has no axe violations with the toolbar and a summary showing', async () => {
    // The confirm dialog renders while something is selected; axe would flag its closed host,
    // which PrimeNG gives role=alertdialog with no name, so check the page around it.
    await load();
    comp.bulkResult.set({ label: 'Ignored', done: 1, skipped: 0, refused: 0, failed: 0,
      outcomes: [{ issueId: 1, outcome: 'DONE', label: 'Done', text: 'Possible misspelling: systm' }] });
    select([1]);
    el().querySelectorAll('p-confirmdialog, p-confirmDialog').forEach(d => d.remove());
    await expectNoAxeViolations(el());
  });
});
