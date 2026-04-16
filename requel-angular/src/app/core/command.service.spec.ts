import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CommandService } from './command.service';
import { CommandResult } from '../models/command';

describe('CommandService', () => {
  let service: CommandService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
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
});
