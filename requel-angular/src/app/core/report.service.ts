import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ReportGeneratorDto } from '../models/report';
import { CommandService } from './command.service';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly base = '/api/projects';

  constructor(
    private http: HttpClient,
    private commandService: CommandService,
    private authService: AuthService
  ) {}

  listReports(projectName: string): Promise<ReportGeneratorDto[]> {
    return firstValueFrom(this.http.get<ReportGeneratorDto[]>(`${this.base}/${projectName}/reports`));
  }

  getReport(projectName: string, reportId: number): Promise<ReportGeneratorDto> {
    return firstValueFrom(this.http.get<ReportGeneratorDto>(`${this.base}/${projectName}/reports/${reportId}`));
  }

  saveReport(projectName: string, reportId: number | null, name: string, text: string | null) {
    return this.commandService.execute('EditReportGenerator', { projectName, reportId, name, text });
  }

  deleteReport(projectName: string, reportId: number) {
    return this.commandService.execute('DeleteReportGenerator', { projectName, reportId });
  }

  /**
   * Trigger a browser download of the generated report.
   * Uses native fetch to include the Bearer token, then creates a Blob URL.
   */
  async downloadReport(projectName: string, reportId: number, reportName: string): Promise<void> {
    const token = this.authService.token();
    const url = `${environment.apiBaseUrl}/projects/${projectName}/reports/${reportId}/run`;
    const response = await fetch(url, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) {
      throw new Error(`Report generation failed: ${response.status}`);
    }
    const blob = await response.blob();
    const blobUrl = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = blobUrl;
    a.download = reportName.replace(/[^a-zA-Z0-9._-]/g, '_') + '.html';
    a.click();
    URL.revokeObjectURL(blobUrl);
  }
}
