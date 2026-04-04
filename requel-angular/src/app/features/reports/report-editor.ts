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
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ReportGeneratorDto } from '../../models/report';
import { ReportService } from '../../core/report.service';
import { PermissionService } from '../../core/permission.service';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';

@Component({
  selector: 'app-report-editor',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputText, TextareaModule, MessageModule,
            ConfirmDialogModule, AnnotationsSectionComponent],
  providers: [ConfirmationService],
  template: `
    <div class="report-editor">
      <div class="page-header">
        <h2>{{ isNew() ? 'New Document' : reportName() }}</h2>
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew() && canDelete()) {
            <p-button label="Delete" icon="pi pi-trash" severity="danger"
                      [outlined]="true" (onClick)="onDelete()" />
          }
          @if (!isNew()) {
            <p-button label="Run" icon="pi pi-play" severity="success"
                      [outlined]="true" (onClick)="onRun()" [loading]="running()" />
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <div class="form-grid">
        <label for="name">Name</label>
        <input id="name" pInputText [(ngModel)]="name" placeholder="Template name" />

        <label for="text">XSLT Template</label>
        <div class="xslt-field">
          <textarea id="text" pTextarea [(ngModel)]="text" rows="20"
                    placeholder="Paste XSLT stylesheet here..." class="xslt-textarea"></textarea>
          <div class="upload-row">
            <p-button label="Upload XSLT" icon="pi pi-upload" severity="secondary"
                      [outlined]="true" size="small" (onClick)="xsltInput.click()" />
            <input #xsltInput type="file" accept=".xsl,.xslt,.xml"
                   (change)="onFileUpload($event)" style="display:none" />
            <span class="upload-hint">Upload a .xsl/.xslt file to replace the template text.</span>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <p-button label="Save" icon="pi pi-check" (onClick)="onSave()" [loading]="saving()"
                  [disabled]="!isNew() && !isDirty()" />
      </div>

      @if (!isNew()) {
        <app-annotations-section
          [projectName]="projectName"
          entityType="ReportGenerator"
          [entityId]="reportId()"
          [canEdit]="canEdit()" />
      }
    </div>

    <p-confirmDialog />
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-grid { display: grid; grid-template-columns: 140px 1fr; gap: 0.75rem 1rem; align-items: start; max-width: 900px; margin-bottom: 1rem; }
    .form-grid label { font-weight: 600; padding-top: 0.4rem; }
    .xslt-field { display: flex; flex-direction: column; gap: 0.5rem; }
    .xslt-textarea { width: 100%; font-family: monospace; font-size: 12px; }
    .upload-row { display: flex; align-items: center; gap: 0.75rem; }
    .upload-hint { font-size: 12px; color: var(--p-text-secondary-color); }
    .form-actions { margin-bottom: 1.5rem; }
  `]
})
export class ReportEditorComponent implements OnInit, OnDestroy {
  report = signal<ReportGeneratorDto | null>(null);
  reportName = signal('');
  reportId = signal<number | null>(null);
  saving = signal(false);
  running = signal(false);
  errorMessage = signal<string | null>(null);

  name = '';
  text = '';

  private originalName = '';
  private originalText = '';

  projectName = '';
  canEdit = signal(false);
  canDelete = signal(false);

  isDirty(): boolean {
    return this.name !== this.originalName || this.text !== this.originalText;
  }

  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private reportService: ReportService,
    private permissionService: PermissionService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  isNew(): boolean {
    return this.reportId() === null;
  }

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      const idParam = params.get('reportId');
      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('ReportGenerator'));
      this.canDelete.set(this.permissionService.canDelete('ReportGenerator'));

      if (idParam && idParam !== 'new') {
        await this.loadReport(Number(idParam));
      } else {
        this.reportId.set(null);
        this.name = '';
        this.text = '';
        this.originalName = '';
        this.originalText = '';
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  private async loadReport(id: number): Promise<void> {
    try {
      const r = await this.reportService.getReport(this.projectName, id);
      this.report.set(r);
      this.reportId.set(r.id);
      this.reportName.set(r.name);
      this.name = r.name;
      this.text = r.text ?? '';
      this.originalName = this.name;
      this.originalText = this.text;
    } catch {
      this.errorMessage.set('Failed to load document.');
    }
  }

  async onSave(): Promise<void> {
    if (!this.name.trim()) {
      this.errorMessage.set('Document name is required.');
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    const result = await this.reportService.saveReport(
      this.projectName, this.reportId(), this.name.trim(), this.text || null
    );
    this.saving.set(false);
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Document saved', life: 3000 });
      const saved = result.entity as ReportGeneratorDto | null;
      if (this.isNew() && saved?.id) {
        this.router.navigate(['/projects', this.projectName, 'reports', saved.id], { replaceUrl: true });
      } else {
        await this.loadReport(this.reportId()!);
      }
    } else {
      this.errorMessage.set(result.error ?? 'Save failed.');
    }
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: `Delete document "${this.reportName()}"? This cannot be undone.`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        const result = await this.reportService.deleteReport(this.projectName, this.reportId()!);
        if (result.success) {
          this.messageService.add({ severity: 'success', summary: 'Document deleted', life: 3000 });
          this.router.navigate(['/projects', this.projectName, 'reports']);
        } else {
          this.errorMessage.set(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  async onRun(): Promise<void> {
    this.running.set(true);
    this.errorMessage.set(null);
    try {
      await this.reportService.downloadReport(this.projectName, this.reportId()!, this.reportName());
    } catch {
      this.errorMessage.set('Failed to generate report.');
    } finally {
      this.running.set(false);
    }
  }

  onFileUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      this.text = reader.result as string;
      if (!this.name) {
        this.name = file.name.replace(/\.[^.]+$/, '');
      }
    };
    reader.readAsText(file);
    input.value = '';
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'reports']);
  }
}
