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
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ProjectSearchService } from './project-search.service';

describe('ProjectSearchService (#504)', () => {
  let service: ProjectSearchService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProjectSearchService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends only the query when nothing else is given', async () => {
    const answer = service.search('My Project', 'login');
    const req = http.expectOne(r => r.url === '/api/projects/My%20Project/search');
    expect(req.request.params.keys()).toEqual(['q']);
    expect(req.request.params.get('q')).toBe('login');
    req.flush({ results: [], total: 0, countsByType: {}, offset: 0, limit: 200 });
    expect((await answer).total).toBe(0);
  });

  it('sends the type, offset and limit when given', async () => {
    const answer = service.search('p', 'login', { type: 'Goal', offset: 200, limit: 50 });
    const req = http.expectOne(r => r.url === '/api/projects/p/search');
    expect(req.request.params.get('type')).toBe('Goal');
    expect(req.request.params.get('offset')).toBe('200');
    expect(req.request.params.get('limit')).toBe('50');
    req.flush({ results: [], total: 0, countsByType: {}, offset: 200, limit: 50 });
    await answer;
  });
});
