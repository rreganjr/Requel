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
/**
 * SSE connection lifecycle state.
 * - `idle`: not connected and not attempting (initial, or after an explicit disconnect).
 * - `connecting`: the initial connection attempt is in flight.
 * - `open`: the stream is live and delivering events.
 * - `degraded`: the connection dropped or failed and is being retried with backoff.
 * - `closed`: the server gracefully ended the stream (transient, before a retry).
 * - `expired`: the auth session ended (SESSION_EXPIRED); no automatic retry.
 */
export type StreamConnectionState =
  | 'idle'
  | 'connecting'
  | 'open'
  | 'degraded'
  | 'closed'
  | 'expired';

export type StreamEventType = 'Session' | 'Data' | 'TargetDeleted' | 'SESSION_EXPIRED';

export interface StreamEventEnvelope {
  eventType: StreamEventType;
  targetType?: string;
  targetId?: number;
  payload: unknown;
}
