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
import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { TableModule } from 'primeng/table';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ActorDto } from '../../models/actor';
import { EntityReferenceDto } from '../../models/entity-reference';
import { ActorService } from '../../core/actor.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';

@Component({
  selector: 'app-actor-editor',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputText, TextareaModule, TableModule,
            MessageModule, ConfirmDialogModule, EntitySelectorDialogComponent,
            AnnotationsSectionComponent],
  providers: [ConfirmationService],
  template: `
    <div class="actor-editor" data-testid="actor-editor">
      <div class="page-header">
        <h2>{{ isNew() ? 'New Actor' : actorName() }}</h2>
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" data-testid="actor-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary" data-testid="actor-copy"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete()) {
              <p-button label="Delete" icon="pi pi-trash" severity="danger" data-testid="actor-delete"
                        [outlined]="true" (onClick)="onDelete()" />
            }
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <div class="form-grid">
        <label for="name">Name</label>
        <input id="name" pInputText [(ngModel)]="name"
               placeholder="Actor name" [disabled]="!canEdit()"
               (ngModelChange)="trackChanges()" />

        <label for="text">Description</label>
        <textarea id="text" pTextarea [(ngModel)]="text"
                  placeholder="Actor description" [rows]="4"
                  [disabled]="!canEdit()"
                  (ngModelChange)="trackChanges()"></textarea>
      </div>

      @if (canEdit()) {
        <div class="form-actions">
          <p-button label="{{ isNew() ? 'Create' : 'Save' }}" icon="pi pi-check"
                    [attr.data-testid]="isNew() ? 'actor-create' : 'actor-save'"
                    [disabled]="!isNew() && !hasChanges()"
                    (onClick)="onSave()" />
        </div>
      }

      @if (!isNew()) {
        <div class="goals-section">
          <div class="section-header">
            <h3>Goals</h3>
            @if (canEdit()) {
              <p-button label="Add Goal" icon="pi pi-plus" severity="secondary"
                        [outlined]="true" (onClick)="showGoalSelector = true" />
            }
          </div>
          <p-table [value]="goals()" [rows]="10">
            <ng-template #header>
              <tr>
                <th>Name</th>
                @if (canEdit()) { <th style="width: 4rem"></th> }
              </tr>
            </ng-template>
            <ng-template #body let-g>
              <tr>
                <td><a class="entity-link" (click)="onGoalClick(g.id)">{{ g.name }}</a></td>
                @if (canEdit()) {
                  <td>
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
                              [rounded]="true" (onClick)="onRemoveGoal(g)" />
                  </td>
                }
              </tr>
            </ng-template>
            <ng-template #emptymessage>
              <tr><td colspan="2" class="empty-text">No goals associated.</td></tr>
            </ng-template>
          </p-table>
        </div>

        <app-entity-selector-dialog
          entityType="Goal"
          [projectName]="projectName"
          [excludeIds]="goalIds()"
          [visible]="showGoalSelector"
          (selected)="onGoalSelected($event)"
          (closed)="showGoalSelector = false" />

        <!-- Referenced By -->
        <div class="goals-section">
          <div class="section-header">
            <h3>Referenced By</h3>
          </div>
          @if (referencedByUseCases().length === 0 && referencedByStories().length === 0) {
            <p class="empty-text">Not referenced by any use case or story.</p>
          }
          @if (referencedByUseCases().length > 0) {
            <p class="ref-label">Use Cases</p>
            <p-table [value]="referencedByUseCases()" styleClass="p-datatable-sm">
              <ng-template pTemplate="body" let-ref>
                <tr>
                  <td><a class="entity-link" (click)="navigate('use-cases', ref.id)">{{ ref.name }}</a></td>
                </tr>
              </ng-template>
            </p-table>
          }
          @if (referencedByStories().length > 0) {
            <p class="ref-label">Stories</p>
            <p-table [value]="referencedByStories()" styleClass="p-datatable-sm">
              <ng-template pTemplate="body" let-ref>
                <tr>
                  <td><a class="entity-link" (click)="navigate('stories', ref.id)">{{ ref.name }}</a></td>
                </tr>
              </ng-template>
            </p-table>
          }
        </div>
      }
    </div>

    <app-annotations-section
      [projectName]="projectName"
      entityType="Actor"
      [entityId]="actorId"
      [canEdit]="canEdit()" />

    <p-confirmDialog />
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-grid { display: grid; grid-template-columns: 150px 1fr; gap: 0.75rem 1rem; align-items: start; max-width: 700px; }
    .form-actions { margin-top: 1rem; max-width: 700px; }
    .goals-section { margin-top: 2rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .section-header h3 { margin: 0; }
    .entity-link { color: var(--p-primary-color); cursor: pointer; text-decoration: underline; }
    .empty-text { color: var(--p-text-muted-color); font-style: italic; }
    .ref-label { font-weight: 600; font-size: 0.85rem; margin: 0.5rem 0 0.25rem; color: var(--p-text-secondary-color); }
  `]
})
export class ActorEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  actor = signal<ActorDto | null>(null);
  actorName = signal('');
  isNew = signal(false);
  canEdit = signal(false);
  canDelete = signal(false);
  errorMessage = signal<string | null>(null);
  goals = signal<EntityReferenceDto[]>([]);
  goalIds = computed(() => this.goals().map(g => g.id).filter((id): id is number => id !== null));
  referencedByUseCases = signal<EntityReferenceDto[]>([]);
  referencedByStories = signal<EntityReferenceDto[]>([]);
  showGoalSelector = false;

  name = '';
  text = '';
  version: number | null = null;
  projectName = '';

  private originalName = '';
  private originalText = '';
  hasChanges = signal(false);

  actorId: number | null = null;
  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private actorService: ActorService,
    private commandService: CommandService,
    private projectService: ProjectService,
    private permissionService: PermissionService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private eventStreamService: EventStreamService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('Actor'));
      this.canDelete.set(this.permissionService.canDelete('Actor'));

      const idParam = params.get('actorId') ?? '';
      if (idParam === 'new') {
        this.isNew.set(true);
        this.actor.set(null);
        this.name = '';
        this.text = '';
        this.version = null;
      } else {
        this.isNew.set(false);
        this.actorId = +idParam;
        this.loadActor();
      }
    });
  }

  hasUnsavedChanges(): boolean {
    return this.hasChanges();
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
    if (this.actorId) {
      void this.eventStreamService.removeSubscription('Actor', this.actorId);
    }
    this.sseSub?.unsubscribe();
  }

  private async loadActor(fromSSE = false): Promise<void> {
    try {
      const a = await this.actorService.getActor(this.projectName, this.actorId!);
      // Don't overwrite unsaved user edits when called from an SSE notification.
      if (fromSSE && this.hasChanges()) {
        return;
      }
      this.actor.set(a);
      this.actorName.set(a.name);
      this.name = a.name;
      this.text = a.text ?? '';
      this.version = a.version;
      this.goals.set(a.goals ?? []);
      this.referencedByUseCases.set(a.referencedByUseCases ?? []);
      this.referencedByStories.set(a.referencedByStories ?? []);
      this.originalName = a.name;
      this.originalText = a.text ?? '';
      this.hasChanges.set(false);
    } catch {
      this.errorMessage.set('Failed to load actor.');
    }
    if (this.actorId && !this.sseSub) {
      void this.eventStreamService.addSubscription('Actor', this.actorId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'Actor' && envelope.targetId === this.actorId) {
          void this.loadActor(true);
        }
      });
    }
  }

  trackChanges(): void {
    this.hasChanges.set(this.name !== this.originalName || this.text !== this.originalText);
  }

  async onSave(): Promise<void> {
    this.errorMessage.set(null);
    try {
      const result = await this.commandService.execute('EditActor', {
        projectName: this.projectName,
        actorId: this.isNew() ? null : this.actorId,
        name: this.name,
        description: this.text || null,
        version: this.version
      });
      if (result.success) {
        if (this.isNew()) {
          this.projectService.notifyTreeChanged();
          this.hasChanges.set(false);
          this.router.navigate(['/projects', this.projectName, 'actors', (result.entity as ActorDto).id]);
        } else {
          this.actorName.set(this.name);
          this.version = (result.entity as ActorDto).version;
          this.originalName = this.name;
          this.originalText = this.text;
          this.hasChanges.set(false);
          this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Actor saved.' });
        }
      } else {
        this.errorMessage.set(result.error ?? 'Save failed.');
      }
    } catch {
      this.errorMessage.set('Save failed.');
    }
  }

  onCopy(): void {
    this.confirmationService.confirm({
      message: 'Create a copy of this actor?',
      accept: async () => {
        const result = await this.commandService.execute('CopyActor', {
          projectName: this.projectName,
          actorId: this.actorId
        });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as ActorDto;
          this.router.navigate(['/projects', this.projectName, 'actors', copy.id]);
        } else {
          this.errorMessage.set(result.error ?? 'Copy failed.');
        }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this actor?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteActor', {
          projectName: this.projectName,
          actorId: this.actorId,
          version: this.version
        });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          this.router.navigate(['/projects', this.projectName, 'actors']);
        } else {
          this.errorMessage.set(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  async onGoalSelected(goal: EntityReferenceDto): Promise<void> {
    this.showGoalSelector = false;
    try {
      const result = await this.commandService.execute('AddGoalToGoalContainer', {
        projectName: this.projectName,
        goalContainerId: this.actorId,
        goalId: goal.id,
        containerType: 'Actor'
      });
      if (result.success) {
        this.goals.update(list => [...list, goal].sort((a, b) => a.name.localeCompare(b.name)));
        this.messageService.add({ severity: 'success', summary: 'Goal added', detail: `"${goal.name}" added successfully.` });
      } else {
        this.errorMessage.set(result.error ?? 'Failed to add goal.');
      }
    } catch {
      this.errorMessage.set('Failed to add goal.');
    }
  }

  async onRemoveGoal(goal: EntityReferenceDto): Promise<void> {
    try {
      const result = await this.commandService.execute('RemoveGoalFromGoalContainer', {
        projectName: this.projectName,
        goalContainerId: this.actorId,
        goalId: goal.id,
        containerType: 'Actor'
      });
      if (result.success) {
        this.goals.update(list => list.filter(g => g.id !== goal.id));
        this.messageService.add({ severity: 'info', summary: 'Goal removed', detail: `"${goal.name}" removed.` });
      } else {
        this.errorMessage.set(result.error ?? 'Failed to remove goal.');
      }
    } catch {
      this.errorMessage.set('Failed to remove goal.');
    }
  }

  onGoalClick(goalId: number): void {
    this.router.navigate(['/projects', this.projectName, 'goals', goalId]);
  }

  navigate(type: string, id: number): void {
    this.router.navigate(['/projects', this.projectName, type, id]);
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'actors']);
  }
}
