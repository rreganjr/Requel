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
import { Component, Input, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { CheckboxModule } from 'primeng/checkbox';
import { SelectModule } from 'primeng/select';
import { MessageService } from 'primeng/api';
import { AnnotationsDto, IssueDto, NoteDto, PositionDto, SUPPORT_LEVEL_OPTIONS } from '../models/annotation';
import { AnnotationService } from '../core/annotation.service';

@Component({
  selector: 'app-annotations-section',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputText, TextareaModule, CheckboxModule, SelectModule],
  template: `
    @if (entityId != null) {
      <div class="annotations-section" data-testid="annotations-section">
        <div class="section-header">
          <h3>Annotations</h3>
          @if (canEdit) {
            <div class="action-buttons">
              <p-button label="Add Note" icon="pi pi-comment" size="small" severity="secondary"
                        data-testid="annotation-add-note"
                        [outlined]="true" (onClick)="showNoteForm.set(true)" />
              <p-button label="Add Issue" icon="pi pi-exclamation-triangle" size="small" severity="warn"
                        data-testid="annotation-add-issue"
                        [outlined]="true" (onClick)="showIssueForm.set(true)" />
            </div>
          }
        </div>

        <!-- Add Note form -->
        @if (showNoteForm()) {
          <div class="add-form" data-testid="annotation-note-form">
            <textarea pTextarea [(ngModel)]="newNoteText" rows="2" placeholder="Note text..."
                      aria-label="Note text" class="add-textarea"
                      data-testid="annotation-note-text"></textarea>
            <div class="form-actions">
              <p-button label="Save Note" icon="pi pi-check" size="small"
                        data-testid="annotation-save-note" (onClick)="saveNote()" />
              <p-button label="Cancel" size="small" severity="secondary" [outlined]="true"
                        (onClick)="cancelNote()" />
            </div>
          </div>
        }

        <!-- Add Issue form -->
        @if (showIssueForm()) {
          <div class="add-form" data-testid="annotation-issue-form">
            <textarea pTextarea [(ngModel)]="newIssueText" rows="2" placeholder="Issue text..."
                      aria-label="Issue text" class="add-textarea"
                      data-testid="annotation-issue-text"></textarea>
            <div class="must-resolve-row">
              <p-checkbox [(ngModel)]="newIssueMustResolve" [binary]="true" inputId="mustResolve" />
              <label for="mustResolve">Must be resolved</label>
            </div>
            <div class="form-actions">
              <p-button label="Save Issue" icon="pi pi-check" size="small" severity="warn"
                        data-testid="annotation-save-issue" (onClick)="saveIssue()" />
              <p-button label="Cancel" size="small" severity="secondary" [outlined]="true"
                        (onClick)="cancelIssue()" />
            </div>
          </div>
        }

        <!-- Notes list -->
        @for (note of annotations().notes; track note.id) {
          <div class="annotation note-item">
            <div class="annotation-row">
              <span class="annotation-badge note-badge">Note</span>
              <span class="annotation-text">{{ note.text }}</span>
              <span class="annotation-creator">{{ note.createdBy }}</span>
              @if (canEdit) {
                <p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                          (onClick)="deleteNote(note)" />
              }
            </div>
          </div>
        }

        <!-- Issues list -->
        @for (issue of annotations().issues; track issue.id) {
          <div class="annotation issue-item" data-testid="annotation-issue"
               [attr.data-resolved]="issue.resolved" [class.resolved]="issue.resolved">
            <div class="annotation-row">
              <span class="annotation-badge issue-badge" data-testid="annotation-issue-badge"
                    [class.resolved-badge]="issue.resolved">
                {{ issue.resolved ? 'Resolved' : 'Issue' }}
              </span>
              @if (issue.mustBeResolved && !issue.resolved) {
                <span class="must-resolve-badge">Must Resolve</span>
              }
              <span class="annotation-text">{{ issue.text }}</span>
              <span class="annotation-creator">{{ issue.createdBy }}</span>
              @if (canEdit) {
                <p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                          (onClick)="deleteIssue(issue)" />
              }
            </div>
            @if (issue.resolved && issue.resolvedByPosition) {
              <div class="resolution-row">
                <span class="resolution-label">Resolution:</span>
                <span class="resolution-text">{{ issue.resolvedByPosition }}</span>
                @if (issue.resolvedBy) {
                  <span class="annotation-creator">by {{ issue.resolvedBy }}</span>
                }
              </div>
            }

            <!-- Positions -->
            @for (pos of issue.positions; track pos.id) {
              <div class="position-item" data-testid="annotation-position">
                <div class="annotation-row">
                  <span class="annotation-badge position-badge" data-testid="annotation-position-badge">Position</span>
                  <span class="annotation-text">{{ pos.text }}</span>
                  <span class="annotation-creator">{{ pos.createdBy }}</span>
                  @if (canEdit && !issue.resolved) {
                    <p-button [label]="resolveLabel(pos.positionType)" icon="pi pi-check-circle"
                              data-testid="annotation-resolve-issue"
                              size="small" severity="success" [outlined]="true"
                              (onClick)="resolveIssue(issue, pos)" />
                  }
                  @if (canEdit) {
                    <p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                              (onClick)="deletePosition(pos)" />
                  }
                </div>

                <!-- Arguments -->
                @for (arg of pos.arguments; track arg.id) {
                  <div class="argument-item" data-testid="annotation-argument">
                    <div class="annotation-row">
                      <span class="annotation-badge" [class]="'arg-badge ' + getSupportClass(arg.supportLevel)">
                        {{ formatSupportLevel(arg.supportLevel) }}
                      </span>
                      <span class="annotation-text">{{ arg.text }}</span>
                      <span class="annotation-creator">{{ arg.createdBy }}</span>
                      @if (canEdit) {
                        <p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                                  (onClick)="deleteArgument(pos, arg)" />
                      }
                    </div>
                  </div>
                }

                <!-- Add Argument form -->
                @if (addArgPositionId() === pos.id) {
                  <div class="add-form nested-form" data-testid="annotation-argument-form">
                    <input pInputText [(ngModel)]="newArgText" placeholder="Argument text..."
                           data-testid="annotation-argument-text" class="add-input" />
                    <p-select [(ngModel)]="newArgSupportLevel" [options]="supportLevelOptions"
                              optionLabel="label" optionValue="value" placeholder="Support level" />
                    <div class="form-actions">
                      <p-button label="Save" icon="pi pi-check" size="small"
                                data-testid="annotation-save-argument" (onClick)="saveArgument(pos)" />
                      <p-button label="Cancel" size="small" severity="secondary" [outlined]="true"
                                (onClick)="addArgPositionId.set(null)" />
                    </div>
                  </div>
                }
                @if (canEdit && addArgPositionId() !== pos.id) {
                  <p-button label="Add Argument" icon="pi pi-plus" size="small" [text]="true"
                            (onClick)="startAddArgument(pos)" />
                }
              </div>
            }

            <!-- Add Position form -->
            @if (addPosIssueId() === issue.id) {
              <div class="add-form nested-form" data-testid="annotation-position-form">
                <input pInputText [(ngModel)]="newPosText" placeholder="Position text..."
                       data-testid="annotation-position-text" class="add-input" />
                <div class="form-actions">
                  <p-button label="Save" icon="pi pi-check" size="small"
                            data-testid="annotation-save-position" (onClick)="savePosition(issue)" />
                  <p-button label="Cancel" size="small" severity="secondary" [outlined]="true"
                            (onClick)="addPosIssueId.set(null)" />
                </div>
              </div>
            }
            @if (canEdit && addPosIssueId() !== issue.id) {
              <p-button label="Add Position" icon="pi pi-plus" size="small" [text]="true"
                        (onClick)="startAddPosition(issue)" />
            }
          </div>
        }

        @if (annotations().notes.length === 0 && annotations().issues.length === 0 && !showNoteForm() && !showIssueForm()) {
          <p class="empty-text">No annotations.</p>
        }
      </div>
    }
  `,
  styles: [`
    .annotations-section { margin-top: 1.5rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
    .section-header h3 { margin: 0; }
    .action-buttons { display: flex; gap: 0.5rem; }

    .add-form { background: var(--p-surface-50, #f8f9fa); border: 1px solid var(--p-surface-200); border-radius: 6px; padding: 0.75rem; margin-bottom: 0.75rem; }
    .nested-form { margin-left: 1.5rem; margin-top: 0.5rem; }
    .add-textarea { width: 100%; }
    .add-input { width: 100%; }
    .must-resolve-row { display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0; }
    .form-actions { display: flex; gap: 0.5rem; margin-top: 0.5rem; }

    .annotation { border: 1px solid var(--p-surface-200); border-radius: 6px; padding: 0.5rem 0.75rem; margin-bottom: 0.5rem; }
    .note-item { border-left: 3px solid var(--p-primary-color); }
    .issue-item { border-left: 3px solid var(--p-orange-500, #f97316); }
    .issue-item.resolved { border-left-color: var(--p-green-500, #22c55e); opacity: 0.8; }
    .position-item { margin-left: 1.5rem; border-left: 3px solid var(--p-surface-400); padding: 0.4rem 0.75rem; margin-top: 0.4rem; }
    .argument-item { margin-left: 1.5rem; padding: 0.25rem 0.5rem; margin-top: 0.25rem; }

    .annotation-row { display: flex; align-items: baseline; gap: 0.5rem; flex-wrap: wrap; }
    .annotation-badge { font-size: 0.7rem; font-weight: 600; padding: 0.1rem 0.4rem; border-radius: 3px; text-transform: uppercase; white-space: nowrap; }
    .note-badge { background: var(--p-primary-100, #dbeafe); color: var(--p-primary-700, #1d4ed8); }
    .issue-badge { background: var(--p-orange-100, #ffedd5); color: var(--p-orange-700, #c2410c); }
    .resolved-badge { background: var(--p-green-100, #dcfce7); color: var(--p-green-700, #15803d); }
    .position-badge { background: var(--p-surface-200); color: var(--p-text-secondary-color); }
    .arg-badge { font-size: 0.65rem; }
    .arg-for { background: var(--p-green-100, #dcfce7); color: var(--p-green-700, #15803d); }
    .arg-against { background: var(--p-red-100, #fee2e2); color: var(--p-red-700, #b91c1c); }
    .arg-neutral { background: var(--p-surface-200); color: var(--p-text-secondary-color); }
    .must-resolve-badge { font-size: 0.65rem; background: var(--p-red-100, #fee2e2); color: var(--p-red-700); padding: 0.1rem 0.4rem; border-radius: 3px; }
    .resolution-row { display: flex; align-items: baseline; gap: 0.4rem; margin-top: 0.25rem; font-size: 0.8rem; flex-wrap: wrap; }
    .resolution-label { font-weight: 600; color: var(--p-green-700, #15803d); white-space: nowrap; }
    .resolution-text { color: var(--p-text-color); font-style: italic; }

    .annotation-text { flex: 1; }
    .annotation-creator { font-size: 0.75rem; color: var(--p-text-secondary-color); white-space: nowrap; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
  `]
})
export class AnnotationsSectionComponent implements OnChanges {
  @Input() projectName = '';
  @Input() entityType = '';
  @Input() entityId: number | null = null;
  @Input() canEdit = false;

  private _annotations = signal<AnnotationsDto>({ notes: [], issues: [] });
  annotations = this._annotations.asReadonly();

  showNoteForm = signal(false);
  showIssueForm = signal(false);
  addPosIssueId = signal<number | null>(null);
  addArgPositionId = signal<number | null>(null);

  newNoteText = '';
  newIssueText = '';
  newIssueMustResolve = false;
  newPosText = '';
  newArgText = '';
  newArgSupportLevel = 'For';

  readonly supportLevelOptions = SUPPORT_LEVEL_OPTIONS;

  constructor(
    private annotationService: AnnotationService,
    private messageService: MessageService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.entityId != null && (changes['entityId'] || changes['entityType'] || changes['projectName'])) {
      this.load();
    }
  }

  private async load(): Promise<void> {
    if (!this.entityId) return;
    try {
      const data = await this.annotationService.getAnnotations(this.projectName, this.entityType, this.entityId);
      this._annotations.set(data);
    } catch {
      // Silently ignore load failures — annotations are supplemental
    }
  }

  async saveNote(): Promise<void> {
    if (!this.newNoteText.trim() || !this.entityId) return;
    const result = await this.annotationService.addNote(this.projectName, this.entityType, this.entityId, this.newNoteText.trim());
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Note added', life: 3000 });
      this.cancelNote();
      await this.load();
    } else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: result.error ?? 'Failed to add note.' });
    }
  }

  cancelNote(): void {
    this.showNoteForm.set(false);
    this.newNoteText = '';
  }

  async saveIssue(): Promise<void> {
    if (!this.newIssueText.trim() || !this.entityId) return;
    const result = await this.annotationService.addIssue(this.projectName, this.entityType, this.entityId, this.newIssueText.trim(), this.newIssueMustResolve);
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Issue added', life: 3000 });
      this.cancelIssue();
      await this.load();
    } else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: result.error ?? 'Failed to add issue.' });
    }
  }

  cancelIssue(): void {
    this.showIssueForm.set(false);
    this.newIssueText = '';
    this.newIssueMustResolve = false;
  }

  async deleteNote(note: NoteDto): Promise<void> {
    const result = await this.annotationService.deleteNote(this.projectName, note.id);
    if (result.success) {
      await this.load();
    }
  }

  async deleteIssue(issue: IssueDto): Promise<void> {
    const result = await this.annotationService.deleteIssue(this.projectName, issue.id);
    if (result.success) {
      await this.load();
    }
  }

  startAddPosition(issue: IssueDto): void {
    this.addPosIssueId.set(issue.id);
    this.newPosText = '';
  }

  async savePosition(issue: IssueDto): Promise<void> {
    if (!this.newPosText.trim()) return;
    const result = await this.annotationService.addPosition(this.projectName, issue.id, this.newPosText.trim());
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Position added', life: 3000 });
      this.addPosIssueId.set(null);
      this.newPosText = '';
      await this.load();
    } else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: result.error ?? 'Failed to add position.' });
    }
  }

  async deletePosition(pos: PositionDto): Promise<void> {
    const result = await this.annotationService.deletePosition(this.projectName, pos.id);
    if (result.success) {
      await this.load();
    }
  }

  startAddArgument(pos: PositionDto): void {
    this.addArgPositionId.set(pos.id);
    this.newArgText = '';
    this.newArgSupportLevel = 'For';
  }

  async saveArgument(pos: PositionDto): Promise<void> {
    if (!this.newArgText.trim()) return;
    const result = await this.annotationService.addArgument(this.projectName, pos.id, this.newArgText.trim(), this.newArgSupportLevel);
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Argument added', life: 3000 });
      this.addArgPositionId.set(null);
      this.newArgText = '';
      await this.load();
    } else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: result.error ?? 'Failed to add argument.' });
    }
  }

  async deleteArgument(pos: PositionDto, arg: { id: number }): Promise<void> {
    const result = await this.annotationService.deleteArgument(this.projectName, arg.id);
    if (result.success) {
      await this.load();
    }
  }

  getSupportClass(supportLevel: string): string {
    if (supportLevel === 'StronglyFor' || supportLevel === 'For') return 'arg-for';
    if (supportLevel === 'StronglyAgainst' || supportLevel === 'Against') return 'arg-against';
    return 'arg-neutral';
  }

  formatSupportLevel(level: string): string {
    return SUPPORT_LEVEL_OPTIONS.find(o => o.value === level)?.label ?? level;
  }

  resolveLabel(positionType: string): string {
    switch (positionType) {
      case 'AddWordToDictionaryPosition': return 'Add to Dictionary';
      case 'ChangeSpellingPosition': return 'Fix Spelling';
      case 'AddActorPosition': return 'Add as Actor';
      case 'AddGlossaryTermPosition': return 'Add to Glossary';
      default: return 'Ignore';
    }
  }

  async resolveIssue(issue: IssueDto, pos: PositionDto): Promise<void> {
    if (!this.entityId) return;
    const result = await this.annotationService.resolveIssue(this.projectName, issue.id, pos.id);
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Issue resolved', life: 3000 });
      await this.load();
    } else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: result.error ?? 'Failed to resolve issue.' });
    }
  }
}
