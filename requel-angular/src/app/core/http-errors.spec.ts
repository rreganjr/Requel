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
import { HttpErrorResponse } from '@angular/common/http';
import { isNotFound } from './http-errors';

describe('isNotFound (#525)', () => {
  it('is true for an HTTP 404', () => {
    expect(isNotFound(new HttpErrorResponse({ status: 404 }))).toBe(true);
  });

  it('is false for any other HTTP status', () => {
    for (const status of [0, 400, 403, 409, 500, 503]) {
      expect(isNotFound(new HttpErrorResponse({ status }))).toBe(false);
    }
  });

  it('is false for something that is not an HTTP error', () => {
    expect(isNotFound(new Error('404'))).toBe(false);
    expect(isNotFound({ status: 404 })).toBe(false);
    expect(isNotFound(null)).toBe(false);
    expect(isNotFound(undefined)).toBe(false);
  });
});
