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
import { AppChipComponent } from './app-chip';

describe('AppChipComponent (issue #155)', () => {
  function render(inputs: Partial<AppChipComponent>): { el: HTMLElement; cmp: AppChipComponent } {
    const fixture = TestBed.createComponent(AppChipComponent);
    Object.assign(fixture.componentInstance, inputs);
    fixture.detectChanges();
    return { el: fixture.nativeElement as HTMLElement, cmp: fixture.componentInstance };
  }

  it('renders the label and defaults to the neutral tone', () => {
    const { el } = render({ label: 'Priority' });
    expect(el.querySelector('.rq-chip-label')?.textContent?.trim()).toBe('Priority');
    expect(el.querySelector('.rq-chip')?.classList).toContain('rq-chip--neutral');
  });

  it('applies a tone class when a tone is set', () => {
    expect(render({ label: 'x', tone: 'primary' }).el.querySelector('.rq-chip')?.classList)
      .toContain('rq-chip--primary');
  });

  it('shows a colour dot when dotColor is set and no icon/avatar/image', () => {
    const dot = render({ label: 'x', dotColor: '#ff0000' }).el.querySelector('.rq-chip-dot') as HTMLElement;
    expect(dot).not.toBeNull();
    expect(dot.style.background).toContain('rgb(255, 0, 0)');
  });

  it('prefers icon over dot, and image over avatar', () => {
    const iconEl = render({ label: 'x', icon: 'pi pi-tag', dotColor: '#000' }).el;
    expect(iconEl.querySelector('.rq-chip-icon')).not.toBeNull();
    expect(iconEl.querySelector('.rq-chip-dot')).toBeNull();

    const mediaEl = render({ label: 'x', avatarUrl: 'a.png', imageUrl: 'b.png' }).el;
    expect(mediaEl.querySelector('.rq-chip-media')).not.toBeNull();
    expect(mediaEl.querySelector('.rq-chip-avatar')).toBeNull();
  });

  it('renders no remove control unless removable', () => {
    expect(render({ label: 'x' }).el.querySelector('.rq-chip-remove')).toBeNull();
  });

  it('renders an accessible remove button that emits remove', () => {
    const { el, cmp } = render({ label: 'Beta', removable: true, removeAriaLabel: 'Remove tag Beta', removeTestid: 'tag-remove' });
    const btn = el.querySelector('.rq-chip-remove') as HTMLButtonElement;
    expect(btn).not.toBeNull();
    expect(btn.getAttribute('aria-label')).toBe('Remove tag Beta');
    expect(btn.getAttribute('data-testid')).toBe('tag-remove');

    let emitted = false;
    cmp.remove.subscribe(() => (emitted = true));
    btn.click();
    expect(emitted).toBe(true);
  });
});
