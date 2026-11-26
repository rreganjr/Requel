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
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter, Params } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { ProjectSearchComponent } from './project-search';
import { ProjectSearchService } from '../../core/project-search.service';
import { AnnouncerService } from '../../core/announcer.service';
import { ProjectSearchPage, ProjectSearchResult } from '../../models/project-search';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const flush = () => new Promise(r => setTimeout(r, 0));

const GOAL: ProjectSearchResult = { entityType: 'Goal', id: 1, name: 'Speed up login', matchedField: 'name',
  snippet: 'Speed up login', matchStart: 9, matchLength: 5 };
const STORY: ProjectSearchResult = { entityType: 'Story', id: 2, name: 'Clerk signs in', matchedField: 'text',
  snippet: '…then the login form opens…', matchStart: 10, matchLength: 5 };
const ISSUE: ProjectSearchResult = { entityType: 'Issue', id: 3, name: 'Is login measured?', matchedField: 'text',
  snippet: 'Is login measured?', matchStart: 3, matchLength: 5, resolved: true,
  onEntityType: 'Goal', onEntityId: 1, onEntityName: 'Speed up login' };
const STAKEHOLDER: ProjectSearchResult = { entityType: 'Stakeholder', id: 4, name: 'Vendor', matchedField: 'text',
  snippet: 'Supplies the cafe', matchStart: -1, matchLength: 0 };

function page(results: ProjectSearchResult[], total = results.length,
    countsByType: Record<string, number> = countsOf(results), offset = 0): ProjectSearchPage {
  return { results, total, countsByType, offset, limit: 200 };
}

function countsOf(results: ProjectSearchResult[]): Record<string, number> {
  const counts: Record<string, number> = {};
  results.forEach(r => counts[r.entityType] = (counts[r.entityType] ?? 0) + 1);
  return counts;
}

describe('ProjectSearchComponent (#504)', () => {
  let query$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let search: ReturnType<typeof vi.fn>;
  let announce: ReturnType<typeof vi.fn>;

  async function open(queryParams: Params) {
    query$ = new BehaviorSubject(convertToParamMap(queryParams));
    TestBed.overrideProvider(ActivatedRoute, { useValue: {
      paramMap: new BehaviorSubject(convertToParamMap({ name: 'Acme' })).asObservable(),
      queryParamMap: query$.asObservable(),
    } });
    const fixture = TestBed.createComponent(ProjectSearchComponent);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture;
  }

  const el = (fixture: { nativeElement: HTMLElement }) => fixture.nativeElement as HTMLElement;
  const all = (fixture: { nativeElement: HTMLElement }, testid: string) =>
    Array.from(el(fixture).querySelectorAll(`[data-testid="${testid}"]`)) as HTMLElement[];

  beforeEach(() => {
    search = vi.fn();
    announce = vi.fn();
    TestBed.configureTestingModule({
      imports: [ProjectSearchComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ProjectSearchService, useValue: { search } },
        { provide: AnnouncerService, useValue: { announce } },
        { provide: ActivatedRoute, useValue: {} },
      ],
    });
  });

  it('asks for 2 characters and does not search a shorter query', async () => {
    const fixture = await open({ q: 'a' });
    expect(search).not.toHaveBeenCalled();
    expect(all(fixture, 'search-prompt')).toHaveLength(1);
  });

  it('groups the results by type with every count, and announces the total', async () => {
    search.mockResolvedValue(page([GOAL, STORY, ISSUE]));
    const fixture = await open({ q: 'login' });

    expect(search).toHaveBeenCalledWith('Acme', 'login', { type: null, limit: 200 });
    const groups = all(fixture, 'search-group-Goal').concat(all(fixture, 'search-group-Story'),
      all(fixture, 'search-group-Issue'));
    expect(groups).toHaveLength(3);
    expect(all(fixture, 'search-summary')[0].textContent).toContain('Showing 3 of 3 matches');
    expect(all(fixture, 'search-filter-all')[0].textContent).toContain('3');
    expect(all(fixture, 'search-filter-Goal')[0].getAttribute('aria-current')).toBeNull();
    expect(all(fixture, 'search-filter-all')[0].getAttribute('aria-current')).toBe('page');
    expect(announce).toHaveBeenCalledWith('3 matches');
  });

  it('highlights the match and shows the snippet whole when it could not be placed', async () => {
    search.mockResolvedValue(page([STORY, STAKEHOLDER]));
    const fixture = await open({ q: 'login' });

    const snippets = all(fixture, 'search-result-snippet');
    expect(snippets[0].querySelector('mark')!.textContent).toBe('login');
    expect(snippets[0].textContent).toContain('Text:');
    expect(snippets[1].querySelector('mark')).toBeNull();
    expect(snippets[1].textContent).toContain('Supplies the cafe');
  });

  it('links each result to its page, an issue to the entity it is on', async () => {
    search.mockResolvedValue(page([GOAL, ISSUE, STAKEHOLDER]));
    const fixture = await open({ q: 'login' });

    const links = all(fixture, 'search-result-link').map(a => a.getAttribute('href'));
    // grouped in type order: Goal, Stakeholder, then Issue
    expect(links).toEqual(['/projects/Acme/goals/1', '/projects/Acme/stakeholders/4', '/projects/Acme/goals/1']);
    expect(all(fixture, 'search-issue-state')[0].textContent).toContain('Resolved');
    expect(fixture.componentInstance.routeFor({ ...ISSUE, onEntityType: 'Project', onEntityId: 9 }))
      .toEqual(['/projects', 'Acme']);
  });

  it('filters to a type and keeps every type counted', async () => {
    search.mockImplementation((_name: string, _q: string, options: { type?: string }) =>
      Promise.resolve(options.type ? page([GOAL], 1, { Goal: 1 }) : page([GOAL, STORY])));
    const fixture = await open({ q: 'login', type: 'Goal' });

    expect(search).toHaveBeenCalledWith('Acme', 'login', { type: 'Goal', limit: 200 });
    expect(search).toHaveBeenCalledWith('Acme', 'login', { limit: 1 });
    expect(all(fixture, 'search-filter-Story')[0].textContent).toContain('1');
    expect(all(fixture, 'search-filter-Goal')[0].getAttribute('aria-current')).toBe('page');
    expect(all(fixture, 'search-result')).toHaveLength(1);
  });

  it('loads the next 200 after the ones shown', async () => {
    const first = Array.from({ length: 2 }, (_, i) => ({ ...GOAL, id: i + 1 }));
    search.mockResolvedValueOnce(page(first, 3, { Goal: 3 }));
    const fixture = await open({ q: 'login' });
    expect(all(fixture, 'search-summary')[0].textContent).toContain('Showing 2 of 3');

    search.mockResolvedValueOnce(page([{ ...GOAL, id: 3 }], 3, { Goal: 3 }, 2));
    all(fixture, 'search-load-more')[0].querySelector('button')!.click();
    await flush();
    fixture.detectChanges();

    expect(search).toHaveBeenLastCalledWith('Acme', 'login', { type: null, offset: 2, limit: 200 });
    expect(all(fixture, 'search-result')).toHaveLength(3);
    expect(all(fixture, 'search-load-more')).toHaveLength(0);
  });

  it('says so when nothing matches', async () => {
    search.mockResolvedValue(page([]));
    const fixture = await open({ q: 'zzz' });
    expect(all(fixture, 'search-empty')[0].textContent).toContain('No matches for');
    expect(all(fixture, 'search-type-filter')).toHaveLength(0);
  });

  it("shows the server's message for a refused search", async () => {
    search.mockRejectedValue(new HttpErrorResponse({ status: 400,
      error: { code: 'BAD_REQUEST', message: "Unknown entity type 'Position'." } }));
    const fixture = await open({ q: 'login', type: 'Position' });
    expect(all(fixture, 'search-error')[0].textContent).toContain("Unknown entity type 'Position'.");
  });

  it('searches again when the query changes', async () => {
    search.mockResolvedValue(page([GOAL]));
    const fixture = await open({ q: 'login' });
    query$.next(convertToParamMap({ q: 'speed' }));
    await flush();
    fixture.detectChanges();
    expect(search).toHaveBeenLastCalledWith('Acme', 'speed', { type: null, limit: 200 });
  });

  it('has no axe violations with results', async () => {
    search.mockResolvedValue(page([GOAL, STORY, ISSUE, STAKEHOLDER], 5, { Goal: 2, Story: 1, Issue: 1, Stakeholder: 1 }));
    const fixture = await open({ q: 'login' });
    await expectNoAxeViolations(el(fixture));
  });
});
