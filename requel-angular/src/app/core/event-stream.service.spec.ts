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


describe('EventStreamService reliability (issue #145)', () => {
  let service: EventStreamService;

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('requel_token', 'test-token');
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    service = TestBed.inject(EventStreamService);
  });

  afterEach(() => {
    service.disconnect(); // cancel any reconnect timer a failed attempt scheduled
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it('replays all live subscriptions on reconnect, including runtime additions (AC1)', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    const svcAny = service as any;
    service.sessionId.set('sess-1');
    svcAny.liveSubscriptions.add('Project:0');                 // initial (as connect() seeds)
    expect(await service.addSubscription('Goal', 7)).toBe(true); // added at runtime
    expect(svcAny.liveSubscriptions.has('Goal:7')).toBe(true);

    fetchSpy.mockClear();
    svcAny.reconnectAttempt = 1;                                // as a scheduled reconnect would run
    await svcAny.startConnection(svcAny.generation);

    const streamUrl = fetchSpy.mock.calls
      .map((c: unknown[]) => String(c[0]))
      .find((u: string) => u.includes('/events/stream?'))!;
    expect(streamUrl).toContain('subscribe=Project%3A0');
    expect(streamUrl).toContain('subscribe=Goal%3A7');
  });

  it('addSubscription resolves false and records the error on a non-ok response (AC2)', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 500 }));
    service.sessionId.set('sess-1');
    const ok = await service.addSubscription('Goal', 7);
    expect(ok).toBe(false);
    expect(service.lastSubscriptionError()).toContain('Goal:7');
    expect((service as any).liveSubscriptions.has('Goal:7')).toBe(false);
  });

  it('addSubscription resolves true and clears the error on success (AC2)', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    service.sessionId.set('sess-1');
    service.lastSubscriptionError.set('stale');
    const ok = await service.addSubscription('Goal', 7);
    expect(ok).toBe(true);
    expect(service.lastSubscriptionError()).toBeNull();
    expect((service as any).liveSubscriptions.has('Goal:7')).toBe(true);
  });

  it('addSubscription returns false without calling fetch when there is no session (AC2)', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    const ok = await service.addSubscription('Goal', 7);
    expect(ok).toBe(false);
    expect(fetchSpy).not.toHaveBeenCalled();
    expect(service.lastSubscriptionError()).not.toBeNull();
  });

  it('removeSubscription resolves true and drops the key on success (AC2)', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    service.sessionId.set('sess-1');
    (service as any).liveSubscriptions.add('Goal:7');
    const ok = await service.removeSubscription('Goal', 7);
    expect(ok).toBe(true);
    expect((service as any).liveSubscriptions.has('Goal:7')).toBe(false);
  });

  it('a failed connection attempt moves to the degraded state (AC3)', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network down'));
    const svcAny = service as any;
    svcAny.liveSubscriptions.add('Project:0');
    await svcAny.startConnection(svcAny.generation);
    expect(service.connectionState()).toBe('degraded');
  });

  it('SESSION_EXPIRED tears down to the expired state and logs out (AC3)', () => {
    const auth = TestBed.inject(AuthService);
    const logoutSpy = vi.spyOn(auth, 'logout').mockImplementation(() => {});
    service.sessionId.set('sess-1');
    (service as any).liveSubscriptions.add('Goal:7');
    (service as any).handleEvent({ eventType: 'SESSION_EXPIRED', payload: {} });
    expect(service.connectionState()).toBe('expired');
    expect(service.sessionId()).toBeNull();
    expect((service as any).liveSubscriptions.size).toBe(0);
    expect(logoutSpy).toHaveBeenCalled();
  });
});
