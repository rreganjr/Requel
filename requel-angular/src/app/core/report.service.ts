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
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ReportGeneratorDto } from '../models/report';
import { CommandService } from './command.service';
import { AuthService } from './auth.service';
import { projectApiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class ReportService {
  constructor(
    private http: HttpClient,
    private commandService: CommandService,
    private authService: AuthService
  ) {}

  listReports(projectName: string): Promise<ReportGeneratorDto[]> {
    return firstValueFrom(this.http.get<ReportGeneratorDto[]>(projectApiUrl(projectName, 'reports')));
  }

  getReport(projectName: string, reportId: number): Promise<ReportGeneratorDto> {
    return firstValueFrom(this.http.get<ReportGeneratorDto>(projectApiUrl(projectName, 'reports', reportId)));
  }

  saveReport(projectName: string, reportId: number | null, name: string, text: string) {
    return this.commandService.execute('EditReportGenerator', { projectName, reportId, name, text });
  }

  deleteReport(projectName: string, reportId: number) {
    return this.commandService.execute('DeleteReportGenerator', { projectName, reportId });
  }

  /**
   * Trigger a browser download of the generated report.
   * Uses native fetch to include the Bearer token, then creates a Blob URL.
   *
   * #275: the file name (and so its extension — `.md` for the ticket generator, `.html` for the
   * HTML one) comes from the server's Content-Disposition. A failed run throws an Error carrying
   * the server's message, e.g. the reference a template could not resolve.
   */
  async downloadReport(projectName: string, reportId: number, reportName: string): Promise<void> {
    const token = this.authService.token();
    const url = projectApiUrl(projectName, 'reports', reportId, 'run');
    const response = await fetch(url, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) {
      throw new Error(await ReportService.failureMessage(response));
    }
    const blob = await response.blob();
    const blobUrl = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = blobUrl;
    a.download = ReportService.fileName(response.headers.get('Content-Disposition'), reportName);
    a.click();
    URL.revokeObjectURL(blobUrl);
  }

  /** The Content-Disposition filename, else the report name as .html (the old behaviour). */
  static fileName(contentDisposition: string | null, reportName: string): string {
    const match = contentDisposition ? /filename="([^"]+)"/.exec(contentDisposition) : null;
    return match ? match[1] : reportName.replace(/[^a-zA-Z0-9._-]/g, '_') + '.html';
  }

  /** The server's error message (a 422 REPORT_FAILED names the cause), else the status. */
  static async failureMessage(response: Response): Promise<string> {
    try {
      const body = await response.json();
      if (body && typeof body.message === 'string' && body.message.length > 0) {
        return body.message;
      }
    } catch {
      // not JSON: fall through to the status
    }
    return `Report generation failed: ${response.status}`;
  }
}
