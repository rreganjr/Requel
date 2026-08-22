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
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { CommandResult, ErrorResponse } from '../models/command';
import { EventStreamService } from './event-stream.service';

/**
 * Service for dispatching commands via the CQRS command endpoint.
 * All mutations go through POST /api/commands/{commandType}.
 */
@Injectable({ providedIn: 'root' })
export class CommandService {

  constructor(private http: HttpClient, private eventStreamService: EventStreamService) {}

  /**
   * Execute a command with a JSON body and return the result.
   */
  async execute<T = unknown>(commandType: string, input: Record<string, unknown> = {}): Promise<CommandResult<T>> {
    try {
      return await firstValueFrom(
        this.http.post<CommandResult<T>>(
          `${environment.apiBaseUrl}/commands/${commandType}`,
          input,
          this.sessionHeaders()
        )
      );
    } catch (err) {
      return this.handleError(err, commandType);
    }
  }

  /**
   * Execute a command with a file upload (multipart/form-data).
   * The input DTO is sent as a JSON part ("input"), the file as a separate part ("file").
   */
  async executeWithFile<T = unknown>(commandType: string, input: Record<string, unknown>, file: File): Promise<CommandResult<T>> {
    const formData = new FormData();
    formData.append('input', new Blob([JSON.stringify(input)], { type: 'application/json' }));
    formData.append('file', file);
    try {
      return await firstValueFrom(
        this.http.post<CommandResult<T>>(
          `${environment.apiBaseUrl}/commands/${commandType}`,
          formData,
          this.sessionHeaders()
        )
      );
    } catch (err) {
      return this.handleError(err, commandType);
    }
  }

  /**
   * Build the request options carrying the caller's SSE session id in an `X-Session-Id` header,
   * so the server can exclude this session from the targeted refresh events it fires for the
   * entity being changed (issue #178) — the editor should not reload the form it just saved. The
   * header is omitted entirely when no stream is open yet (`sessionId()` is null), which the server
   * treats as "exclude nobody".
   */
  private sessionHeaders(): { headers?: HttpHeaders } {
    const sid = this.eventStreamService.sessionId();
    return sid ? { headers: new HttpHeaders({ 'X-Session-Id': sid }) } : {};
  }

  /**
   * Normalises a failed request into a `CommandResult`, carrying the HTTP `status`
   * through so callers can distinguish a **409** optimistic-lock conflict from an
   * ordinary failure. A body that is already a `CommandResult` keeps its own fields
   * and only gains `status`.
   */
  private handleError<T>(err: unknown, commandType: string): CommandResult<T> {
    if (err instanceof HttpErrorResponse) {
      const body = err.error as CommandResult<T> | ErrorResponse;
      if (body && typeof body === 'object' && 'success' in body) {
        return { ...(body as CommandResult<T>), status: err.status };
      }
      return {
        success: false,
        entityType: commandType,
        error: (body as ErrorResponse)?.message ?? err.message,
        entity: null,
        violations: null,
        status: err.status
      };
    }
    return {
      success: false,
      entityType: commandType,
      error: 'Network error',
      entity: null,
      violations: null,
      status: 0
    };
  }
}

/**
 * True when a command failed because no HTTP response was received — a transport /
 * network failure. `CommandService` reports these with `status === 0` (Angular's
 * convention for a request that never reached the server), which is the one failure
 * class worth offering the user a Retry for. Server-produced failures (validation,
 * conflict, other 4xx/5xx) all carry their own status and are not retryable this way.
 *
 * Pairs with `app-submit-error`'s `retryable` input (issue #133): a host sets
 * `[retryable]="isNetworkError(result)"` and re-runs its own action on `(retry)`.
 */
export function isNetworkError(result: CommandResult): boolean {
  return result.status === 0;
}
