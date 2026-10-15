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
import { AppTagComponent } from './app-tag';
import { RQ_TONE_ICON, RqTone } from './severity';

const TONES: RqTone[] = ['primary', 'success', 'info', 'warning', 'danger', 'neutral'];

describe('AppTagComponent (issue #155)', () => {
  function render(inputs: Partial<AppTagComponent>): HTMLElement {
    const fixture = TestBed.createComponent(AppTagComponent);
    Object.assign(fixture.componentInstance, inputs);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders a visible label so colour is never the only signal (#141)', () => {
    const el = render({ tone: 'warning', label: 'Issue' });
    expect(el.querySelector('.rq-tag-label')?.textContent?.trim()).toBe('Issue');
  });

  it('applies the tone class and data-tone for every tone', () => {
    for (const tone of TONES) {
      const el = render({ tone, label: tone });
      const tag = el.querySelector('.rq-tag');
      expect(tag?.classList).toContain('rq-tag--' + tone);
      expect(tag?.getAttribute('data-tone')).toBe(tone);
    }
  });

  it('adds the pill modifier only for the pill variant', () => {
    expect(render({ variant: 'pill', label: 'x' }).querySelector('.rq-tag')?.classList)
      .toContain('rq-tag--pill');
    expect(render({ variant: 'default', label: 'x' }).querySelector('.rq-tag')?.classList)
      .not.toContain('rq-tag--pill');
  });

  it('falls back to the tone default icon for the icon variant', () => {
    const el = render({ tone: 'success', variant: 'icon', label: 'Done' });
    const icon = el.querySelector('.rq-tag-icon');
    expect(icon).not.toBeNull();
    for (const cls of RQ_TONE_ICON.success.split(' ')) {
      expect(icon?.classList).toContain(cls);
    }
  });

  it('prefers an explicit icon over the tone default, on any variant', () => {
    const el = render({ tone: 'info', variant: 'default', icon: 'pi pi-comment', label: 'Note' });
    expect(el.querySelector('.rq-tag-icon')?.classList).toContain('pi-comment');
  });

  it('renders no icon for the default variant when none is supplied', () => {
    expect(render({ label: 'x' }).querySelector('.rq-tag-icon')).toBeNull();
  });
});
