import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { CommandResult, ErrorResponse } from '../models/command';

/**
 * Service for dispatching commands via the CQRS command endpoint.
 * All mutations go through POST /api/commands/{commandType}.
 */
@Injectable({ providedIn: 'root' })
export class CommandService {

  constructor(private http: HttpClient) {}

  /**
   * Execute a command and return the result.
   * The input object is serialized as JSON in the request body.
   */
  async execute<T = unknown>(commandType: string, input: Record<string, unknown> = {}): Promise<CommandResult<T>> {
    try {
      return await firstValueFrom(
        this.http.post<CommandResult<T>>(
          `${environment.apiBaseUrl}/commands/${commandType}`,
          input
        )
      );
    } catch (err) {
      if (err instanceof HttpErrorResponse) {
        const body = err.error as CommandResult<T> | ErrorResponse;
        if ('success' in body) {
          return body as CommandResult<T>;
        }
        return {
          success: false,
          commandType,
          message: (body as ErrorResponse).message ?? err.message,
          data: null,
          violations: null
        };
      }
      return {
        success: false,
        commandType,
        message: 'Network error',
        data: null,
        violations: null
      };
    }
  }
}
