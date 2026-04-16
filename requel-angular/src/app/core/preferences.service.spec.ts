import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { PreferencesService } from './preferences.service';
import { UserPreferencesDto } from '../models/preferences';

describe('PreferencesService', () => {
  let service: PreferencesService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PreferencesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('starts with default preferences and isLoaded() false', () => {
    expect(service.preferences().sidebarProjectLimit).toBe(10);
    expect(service.isLoaded()).toBe(false);
  });

  it('load() sends GET /api/user-preferences and updates the signal', async () => {
    const serverPrefs: UserPreferencesDto = { sidebarProjectLimit: 5, sidebarProjectStaleness: 'ONE_MONTH' };
    const promise = service.load();
    const req = httpMock.expectOne('/api/user-preferences');
    expect(req.request.method).toBe('GET');
    req.flush(serverPrefs);
    await promise;
    expect(service.preferences().sidebarProjectLimit).toBe(5);
    expect(service.isLoaded()).toBe(true);
  });

  it('save() sends PUT /api/user-preferences and updates the signal', async () => {
    const updated: UserPreferencesDto = { sidebarProjectLimit: 20, sidebarProjectStaleness: 'SIX_MONTHS' };
    const promise = service.save(updated);
    const req = httpMock.expectOne('/api/user-preferences');
    expect(req.request.method).toBe('PUT');
    req.flush(updated);
    await promise;
    expect(service.preferences().sidebarProjectLimit).toBe(20);
  });
});
