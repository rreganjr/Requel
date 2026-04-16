import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom, take } from 'rxjs';
import { EventStreamService } from './event-stream.service';
import { AuthService } from './auth.service';

describe('EventStreamService', () => {
  let service: EventStreamService;

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('requel_token', 'test-token');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    service = TestBed.inject(EventStreamService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it('starts in idle connection state', () => {
    expect(service.connectionState()).toBe('idle');
    expect(service.isConnected()).toBe(false);
    expect(service.sessionId()).toBeNull();
  });

  it('addSubscription() is a no-op when not connected (no sessionId)', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response());
    await service.addSubscription('Goal', 1);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it('removeSubscription() is a no-op when not connected (no sessionId)', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response());
    await service.removeSubscription('Goal', 1);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it('disconnect() moves state back to idle', () => {
    // Simulate a connected state by setting state directly via connect() stub
    // Since connect() calls fetch internally, stub it to avoid real network calls
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new DOMException('AbortError', 'AbortError'));
    service.connect(['Project:1']);
    service.disconnect();
    expect(service.connectionState()).toBe('idle');
    expect(service.sessionId()).toBeNull();
  });

  it('events$ emits parsed events from processSSEBlock (via handleEvent default branch)', async () => {
    // Access the private method to unit-test SSE parsing without a real stream
    const svcAny = service as any;
    const collected: unknown[] = [];
    service.events$.pipe(take(1)).subscribe(e => collected.push(e));

    svcAny.processSSEBlock('data: {"eventType":"EntityUpdated","payload":{"id":42}}');

    expect(collected).toHaveLength(1);
    expect((collected[0] as any).eventType).toBe('EntityUpdated');
  });

  it('Session event updates sessionId signal', () => {
    const svcAny = service as any;
    svcAny.handleEvent({ eventType: 'Session', payload: { sessionId: 'abc-123' } });
    expect(service.sessionId()).toBe('abc-123');
  });
});
