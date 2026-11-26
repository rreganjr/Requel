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
import { provideRouter, Router } from '@angular/router';
import { ProjectSearchBoxComponent } from './project-search-box';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

describe('ProjectSearchBoxComponent (#504)', () => {
  let router: Router;

  function create(inputs: Record<string, unknown> = {}) {
    const fixture = TestBed.createComponent(ProjectSearchBoxComponent);
    fixture.componentRef.setInput('projectName', 'Acme');
    for (const [key, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(key, value);
    }
    fixture.detectChanges();
    return fixture;
  }

  function type(el: HTMLElement, text: string) {
    const input = el.querySelector('[data-testid="project-search-input"]') as HTMLInputElement;
    input.value = text;
    input.dispatchEvent(new Event('input'));
  }

  function submitButton(el: HTMLElement): HTMLButtonElement {
    return el.querySelector('[data-testid="project-search-submit"] button') as HTMLButtonElement;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ProjectSearchBoxComponent], providers: [provideRouter([])] });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('disables search and shows the hint under 2 characters', () => {
    const fixture = create();
    const el = fixture.nativeElement as HTMLElement;
    type(el, ' a ');
    fixture.detectChanges();

    expect(submitButton(el).disabled).toBe(true);
    const hint = el.querySelector('[data-testid="project-search-hint"]')!;
    expect(hint.textContent).toContain('at least 2 characters');
    expect(hint.classList).not.toContain('rq-visually-hidden');

    fixture.componentInstance.submit();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('keeps the hint for screen readers only while the box is empty or searchable', () => {
    const fixture = create();
    const el = fixture.nativeElement as HTMLElement;
    const hint = () => el.querySelector('[data-testid="project-search-hint"]')!;
    expect(hint().classList).toContain('rq-visually-hidden');
    type(el, 'login');
    fixture.detectChanges();
    expect(hint().classList).toContain('rq-visually-hidden');
    expect(submitButton(el).disabled).toBe(false);
  });

  it('opens the results page with the trimmed text on submit', () => {
    const fixture = create();
    const el = fixture.nativeElement as HTMLElement;
    type(el, '  login  ');
    fixture.detectChanges();
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'Acme', 'search'], { queryParams: { q: 'login' } });
  });

  it('starts with the given query and carries the type filter', () => {
    const fixture = create({ query: 'report', type: 'Goal' });
    const el = fixture.nativeElement as HTMLElement;
    expect((el.querySelector('[data-testid="project-search-input"]') as HTMLInputElement).value).toBe('report');

    fixture.componentInstance.submit();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'Acme', 'search'],
      { queryParams: { q: 'report', type: 'Goal' } });
  });

  it('has no axe violations', async () => {
    const fixture = create();
    type(fixture.nativeElement, 'a');
    fixture.detectChanges();
    await expectNoAxeViolations(fixture.nativeElement);
  });
});
