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
   * Execute a command with a JSON body and return the result.
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
          formData
        )
      );
    } catch (err) {
      return this.handleError(err, commandType);
    }
  }

  private handleError<T>(err: unknown, commandType: string): CommandResult<T> {
    if (err instanceof HttpErrorResponse) {
      const body = err.error as CommandResult<T> | ErrorResponse;
      if ('success' in body) {
        return body as CommandResult<T>;
      }
      return {
        success: false,
        entityType: commandType,
        error: (body as ErrorResponse).message ?? err.message,
        entity: null,
        violations: null
      };
    }
    return {
      success: false,
      entityType: commandType,
      error: 'Network error',
      entity: null,
      violations: null
    };
  }
}
