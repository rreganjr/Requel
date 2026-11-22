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
import { ApiTokensComponent } from './api-tokens';
import { TokenService } from '../../core/token.service';
import { ApiTokenDto } from '../../models/api-token';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

/** #519: the token list is an app-data-table, like every other list. */
describe('ApiTokensComponent token table', () => {
  const TOKENS: ApiTokenDto[] = [
    { id: 1, name: 'claude-desktop', createdAt: '2026-11-01T10:00:00Z', lastUsedAt: null,
      expiresAt: '2027-01-30T10:00:00Z', status: 'ACTIVE' },
    { id: 2, name: 'old-cli', createdAt: '2026-01-01T10:00:00Z', lastUsedAt: '2026-02-01T10:00:00Z',
      expiresAt: null, status: 'REVOKED' },
  ];

  async function render(): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      imports: [ApiTokensComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TokenService, useValue: { list: vi.fn().mockResolvedValue(TOKENS) } },
      ],
    });
    const fixture = TestBed.createComponent(ApiTokensComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders a row per token with its status, dates and action', async () => {
    const el = await render();
    expect(el.querySelector('app-data-table[data-testid="pat-table"]')).not.toBeNull();
    const rows = Array.from(el.querySelectorAll('tr[data-testid="pat-row"]'));
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('claude-desktop');
    expect(rows[0].querySelector('.status-active')?.textContent).toContain('ACTIVE');
    expect(rows[0].textContent).toContain('Revoke');
    expect(rows[1].querySelector('.status-revoked')).not.toBeNull();
    expect(rows[1].textContent).toContain('Never');
    expect(rows[1].querySelector('[data-testid="pat-delete"]')).not.toBeNull();
    const headers = Array.from(el.querySelectorAll('thead th')).map(h => h.textContent?.trim());
    expect(headers).toEqual(['Name', 'Status', 'Created', 'Last used', 'Expires', 'Actions']);
  });

  it('has no axe-core violations', async () => {
    const el = await render();
    await expectNoAxeViolations(el);
  });
});
