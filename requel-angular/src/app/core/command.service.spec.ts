import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CommandService, isNetworkError } from './command.service';
import { CommandResult } from '../models/command';
import { EventStreamService } from './event-stream.service';

describe('CommandService', () => {
  let service: CommandService;
  let httpMock: HttpTestingController;
  let sessionId: string | null;

  beforeEach(() => {
    sessionId = null;
    const eventStreamStub = { sessionId: () => sessionId };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: EventStreamService, useValue: eventStreamStub }
      ]
    });
    service = TestBed.inject(CommandService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('execute() posts to /api/commands/{type} and returns the result', async () => {
    const mockResult: CommandResult = {
      success: true, entityType: 'EditGoal', entity: { id: 1 }, error: null, violations: null
    };
    const promise = service.execute('EditGoal', { name: 'My Goal' });
    const req = httpMock.expectOne('/api/commands/EditGoal');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'My Goal' });
    req.flush(mockResult);
    const result = await promise;
    expect(result.success).toBe(true);
    expect(result.entityType).toBe('EditGoal');
  });

  it('execute() returns structured error when server replies with CommandResult failure body', async () => {
    const errorBody: CommandResult = {
      success: false, entityType: 'EditGoal', entity: null,
      error: 'Name already exists', violations: null
    };
    const promise = service.execute('EditGoal', { name: 'Dup' });
    const req = httpMock.expectOne('/api/commands/EditGoal');
    req.flush(errorBody, { status: 409, statusText: 'Conflict' });
    const result = await promise;
    expect(result.success).toBe(false);
    expect(result.error).toBe('Name already exists');
  });

  it('execute() wraps plain ErrorResponse into CommandResult on HTTP error', async () => {
    const promise = service.execute('EditGoal', {});
    const req = httpMock.expectOne('/api/commands/EditGoal');
    req.flush({ error: 'Bad Request', message: 'Invalid input', timestamp: '' }, { status: 400, statusText: 'Bad Request' });
    const result = await promise;
    expect(result.success).toBe(false);
    expect(result.error).toBe('Invalid input');
  });

  it('execute() with empty body still posts to the endpoint', async () => {
    const promise = service.execute('DeleteGoal');
    const req = httpMock.expectOne('/api/commands/DeleteGoal');
    req.flush({ success: true, entityType: 'DeleteGoal', entity: null, error: null, violations: null });
    const result = await promise;
    expect(result.success).toBe(true);
  });

  it('execute() sends the X-Session-Id header when a stream session exists', async () => {
    sessionId = 'sess-xyz';
    const promise = service.execute('EditGoal', { name: 'x' });
    const req = httpMock.expectOne('/api/commands/EditGoal');
    expect(req.request.headers.get('X-Session-Id')).toBe('sess-xyz');
    req.flush({ success: true, entityType: 'EditGoal', entity: null, error: null, violations: null });
    await promise;
  });

  it('execute() omits the X-Session-Id header when no stream session is open', async () => {
    sessionId = null;
    const promise = service.execute('EditGoal', { name: 'x' });
    const req = httpMock.expectOne('/api/commands/EditGoal');
    expect(req.request.headers.has('X-Session-Id')).toBe(false);
    req.flush({ success: true, entityType: 'EditGoal', entity: null, error: null, violations: null });
    await promise;
  });

  it('execute() reports a transport failure as a retryable network error (status 0)', async () => {
    const promise = service.execute('EditGoal', { name: 'x' });
    const req = httpMock.expectOne('/api/commands/EditGoal');
    req.error(new ProgressEvent('error'));
    const result = await promise;
    expect(result.success).toBe(false);
    expect(result.status).toBe(0);
    expect(isNetworkError(result)).toBe(true);
  });

  describe('isNetworkError()', () => {
    const base: CommandResult = {
      success: false, entityType: 'X', entity: null, error: 'e', violations: null,
    };
    it('is true only when status is 0 (no HTTP response)', () => {
      expect(isNetworkError({ ...base, status: 0 })).toBe(true);
    });
    it('is false for a 409 conflict', () => {
      expect(isNetworkError({ ...base, status: 409 })).toBe(false);
    });
    it('is false for a server body failure with no status', () => {
      expect(isNetworkError({ ...base })).toBe(false);
    });
    it('is false for a success result', () => {
      expect(isNetworkError({
        success: true, entityType: 'X', entity: {}, error: null, violations: null,
      })).toBe(false);
    });
  });
});
