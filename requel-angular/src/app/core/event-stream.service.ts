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
import { Injectable, signal, computed, OnDestroy } from '@angular/core';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { StreamConnectionState, StreamEventEnvelope } from '../models/stream';
import { Subject } from 'rxjs';

/**
 * Fetch-based SSE streaming service. Uses native fetch() + ReadableStream
 * instead of EventSource to support JWT in the Authorization header.
 *
 * Manages a single SSE connection with session-based subscriptions.
 * Supports dynamic subscribe/unsubscribe, keep-alive, exponential
 * backoff reconnect, and graceful server-side disconnect.
 */
@Injectable({ providedIn: 'root' })
export class EventStreamService implements OnDestroy {

  readonly connectionState = signal<StreamConnectionState>('idle');
  readonly sessionId = signal<string | null>(null);
  readonly isConnected = computed(() => this.connectionState() === 'open');

  /**
   * Error from the most recent add/removeSubscription call, or null when the
   * last one succeeded. Lets the UX surface a subscription that didn't take.
   */
  readonly lastSubscriptionError = signal<string | null>(null);

  /** Observable of all received stream events */
  private readonly eventsSubject = new Subject<StreamEventEnvelope>();
  readonly events$ = this.eventsSubject.asObservable();

  /**
   * Single source of truth for the subscriptions that must be (re)established on
   * every (re)connect. Seeded by connect() and kept current by add/remove, so a
   * subscription added at runtime survives a reconnect (see scheduleReconnect).
   */
  private readonly liveSubscriptions = new Set<string>();

  private abortController: AbortController | null = null;
  private generation = 0;
  private reconnectAttempt = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly maxReconnectDelay = 30_000;

  constructor(private authService: AuthService) {}

  /**
   * Open the SSE connection with initial subscriptions.
   * @param subscriptions target keys like ["Project:1", "Goal:7"]
   */
  connect(subscriptions: string[] = []): void {
    this.disconnect();
    this.liveSubscriptions.clear();
    for (const sub of subscriptions) {
      this.liveSubscriptions.add(sub);
    }
    this.generation++;
    this.reconnectAttempt = 0;
    this.startConnection(this.generation);
  }

  /**
   * Gracefully close the SSE connection.
   */
  disconnect(): void {
    this.generation++;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
    }
    // Notify server of graceful close
    const sid = this.sessionId();
    if (sid) {
      this.closeServerConnection(sid);
    }
    this.connectionState.set('idle');
    this.sessionId.set(null);
  }

  /**
   * Add a subscription to the current session.
   */
  async addSubscription(targetType: string, targetId: number): Promise<boolean> {
    const sid = this.sessionId();
    if (!sid) {
      this.lastSubscriptionError.set('Cannot subscribe: no active stream session');
      return false;
    }

    const key = `${targetType}:${targetId}`;
    const token = this.authService.token();
    try {
      const response = await fetch(`${environment.apiBaseUrl}/events/stream/subscriptions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Session-Id': sid,
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ targetType, targetId })
      });
      if (!response.ok) {
        this.lastSubscriptionError.set(`Subscribe failed (${response.status}) for ${key}`);
        return false;
      }
      this.liveSubscriptions.add(key);
      this.lastSubscriptionError.set(null);
      return true;
    } catch {
      this.lastSubscriptionError.set(`Subscribe request failed for ${key}`);
      return false;
    }
  }

  /**
   * Remove a subscription from the current session.
   */
  async removeSubscription(targetType: string, targetId: number): Promise<boolean> {
    const sid = this.sessionId();
    if (!sid) {
      this.lastSubscriptionError.set('Cannot unsubscribe: no active stream session');
      return false;
    }

    const key = `${targetType}:${targetId}`;
    const token = this.authService.token();
    try {
      const response = await fetch(`${environment.apiBaseUrl}/events/stream/subscriptions`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
          'X-Session-Id': sid,
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ targetType, targetId })
      });
      if (!response.ok) {
        // Keep the key: server state is unknown, so replaying it on the next
        // reconnect is the safe default.
        this.lastSubscriptionError.set(`Unsubscribe failed (${response.status}) for ${key}`);
        return false;
      }
      this.liveSubscriptions.delete(key);
      this.lastSubscriptionError.set(null);
      return true;
    } catch {
      this.lastSubscriptionError.set(`Unsubscribe request failed for ${key}`);
      return false;
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.eventsSubject.complete();
  }

  private async startConnection(gen: number): Promise<void> {
    if (gen !== this.generation) return;

    // A first attempt is 'connecting'; a retry (reconnectAttempt > 0) is
    // 'degraded' so the UX can distinguish "establishing" from "recovering".
    this.connectionState.set(this.reconnectAttempt === 0 ? 'connecting' : 'degraded');
    this.abortController = new AbortController();

    const params = new URLSearchParams();
    // Replay the CURRENT live subscription set (initial + any added at runtime).
    for (const sub of this.liveSubscriptions) {
      params.append('subscribe', sub);
    }
    const sid = this.sessionId();
    if (sid) {
      params.append('sessionId', sid);
    }

    const token = this.authService.token();
    if (!token) {
      this.connectionState.set('degraded');
      return;
    }

    try {
      const response = await fetch(
        `${environment.apiBaseUrl}/events/stream?${params.toString()}`,
        {
          headers: { 'Authorization': `Bearer ${token}` },
          signal: this.abortController.signal
        }
      );

      if (!response.ok || !response.body) {
        throw new Error(`SSE connection failed: ${response.status}`);
      }

      if (gen !== this.generation) return;
      this.connectionState.set('open');
      this.reconnectAttempt = 0;
      await this.readStream(gen, response.body);

    } catch (err: unknown) {
      if (gen !== this.generation) return;
      if (err instanceof DOMException && err.name === 'AbortError') return;

      this.connectionState.set('degraded');
      this.scheduleReconnect(gen);
    }
  }

  private async readStream(gen: number, body: ReadableStream<Uint8Array>): Promise<void> {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    try {
      while (gen === this.generation) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // SSE format: "data:" lines separated by blank lines
        const parts = buffer.split('\n\n');
        buffer = parts.pop() ?? '';

        for (const part of parts) {
          this.processSSEBlock(part);
        }
      }
    } catch (err: unknown) {
      if (gen !== this.generation) return;
      if (err instanceof DOMException && err.name === 'AbortError') return;
    } finally {
      reader.releaseLock();
    }

    if (gen === this.generation) {
      this.connectionState.set('closed');
      this.scheduleReconnect(gen);
    }
  }

  private processSSEBlock(block: string): void {
    for (const line of block.split('\n')) {
      // Skip SSE comments (keep-alive)
      if (line.startsWith(':')) continue;

      if (line.startsWith('data:')) {
        const json = line.substring(5).trim();
        if (!json) continue;

        try {
          const envelope: StreamEventEnvelope = JSON.parse(json);
          this.handleEvent(envelope);
        } catch {
          // Malformed JSON — skip
        }
      }
    }
  }

  private handleEvent(envelope: StreamEventEnvelope): void {
    switch (envelope.eventType) {
      case 'Session': {
        const sid = (envelope.payload as { sessionId: string })?.sessionId;
        if (sid) this.sessionId.set(sid);
        break;
      }
      case 'SESSION_EXPIRED':
        this.expireSession();
        break;
      default:
        this.eventsSubject.next(envelope);
    }
  }

  private scheduleReconnect(gen: number): void {
    if (gen !== this.generation) return;

    const delay = Math.min(
      1000 * Math.pow(2, this.reconnectAttempt),
      this.maxReconnectDelay
    );
    this.reconnectAttempt++;

    this.reconnectTimer = setTimeout(() => {
      if (gen === this.generation) {
        this.startConnection(gen);
      }
    }, delay);
  }

  /**
   * Tear down the connection after the server reports the auth session expired.
   * Like disconnect(), but lands on 'expired' (no auto-retry) so the shell can
   * prompt the user to sign in again, and logs the user out.
   */
  private expireSession(): void {
    this.generation++;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
    }
    this.liveSubscriptions.clear();
    this.sessionId.set(null);
    this.connectionState.set('expired');
    this.authService.logout();
  }

  private async closeServerConnection(sid: string): Promise<void> {
    const token = this.authService.token();
    try {
      await fetch(`${environment.apiBaseUrl}/events/stream/connection`, {
        method: 'DELETE',
        headers: {
          'X-Session-Id': sid,
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });
    } catch {
      // Best-effort — server will clean up on timeout
    }
  }
}
