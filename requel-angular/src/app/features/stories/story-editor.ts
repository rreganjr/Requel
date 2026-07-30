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
import { PageHeaderComponent } from '../../shared/page-header';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { StoryDto } from '../../models/story';
import { EntityReferenceDto } from '../../models/entity-reference';
import { StoryService } from '../../core/story.service';
import { ActorService } from '../../core/actor.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';

@Component({
  selector: 'app-story-editor',
  standalone: true,
  imports: [PageHeaderComponent, RouterLink, FormsModule, ButtonModule, InputText, TextareaModule, SelectModule,
            TableModule, MessageModule, ConfirmDialogModule, EntitySelectorDialogComponent,
            AnnotationsSectionComponent],
  providers: [ConfirmationService],
  template: `
    <div class="story-editor" data-testid="story-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Story' : storyName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary"
                    data-testid="story-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary"
                        data-testid="story-copy"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete()) {
              <p-button label="Delete" icon="pi pi-trash" severity="danger"
                        data-testid="story-delete"
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
        <input id="name" pInputText [(ngModel)]="name" placeholder="Story name"
               (ngModelChange)="trackChanges()" />

        <label for="type">Type</label>
        <p-select id="type" inputId="storyTypeInput" data-testid="story-type"
                  [(ngModel)]="storyType" [options]="storyTypeOptions"
                  optionLabel="label" optionValue="value"
                  (ngModelChange)="trackChanges()" />

        <label for="primaryActor">Primary Actor</label>
        <p-select id="primaryActor" inputId="storyPrimaryActorInput"
                  data-testid="story-primary-actor"
                  [(ngModel)]="primaryActorName"
                  [options]="actorOptions()"
                  optionLabel="label"
                  optionValue="value"
                  [showClear]="true"
                  [pt]="{ clearIcon: { 'data-testid': 'story-primary-actor-clear' } }"
                  placeholder="Select primary actor"
                  (ngModelChange)="trackChanges()"
                  styleClass="w-full" />

        <label for="text">Text</label>
        <textarea id="text" pTextarea [(ngModel)]="text" rows="8"
                  placeholder="Story text"
                  (ngModelChange)="trackChanges()"></textarea>
      </div>

      <div class="form-actions">
        <p-button label="Save" icon="pi pi-check" data-testid="story-save"
                  (onClick)="onSave()" [loading]="saving()"
                  [disabled]="!isNew() && !hasChanges()" />
      </div>

      <!-- Goals sub-table -->
      @if (!isNew() && story()) {
        <div class="section">
          <div class="section-header">
            <h3>Goals</h3>
            @if (canEdit()) {
              <p-button label="Add Goal" icon="pi pi-plus" size="small"
                        data-testid="story-add-goal"
                        (onClick)="showGoalSelector = true" />
            }
          </div>

          @if (story()!.goals?.length) {
            <p-table [value]="story()!.goals!" [rows]="10" data-testid="story-goals-table">
              <ng-template #header>
                <tr>
                  <th>Name</th>
                  @if (canEdit()) { <th style="width: 60px"></th> }
                </tr>
              </ng-template>
              <ng-template #body let-g>
                <tr data-testid="story-goal-row">
                  <td><a class="entity-link" data-testid="story-goal-link" [routerLink]="['/projects', projectName, 'goals', g.id]">{{ g.name }}</a></td>
                  @if (canEdit()) {
                    <td><p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                                  data-testid="story-remove-goal" [ariaLabel]="'Remove goal ' + g.name"
                                  (onClick)="onRemoveGoal(g)" /></td>
                  }
                </tr>
              </ng-template>
            </p-table>
          } @else {
            <p class="empty-text">No goals associated.</p>
          }
        </div>

        <!-- Additional Actors sub-table -->
        <div class="section">
          <div class="section-header">
            <h3>Additional Actors</h3>
            @if (canEdit()) {
              <p-button label="Add Actor" icon="pi pi-plus" size="small"
                        data-testid="story-add-actor"
                        (onClick)="showActorSelector = true" />
            }
          </div>
          @if (story()!.actors?.length) {
            <p-table [value]="story()!.actors!" [rows]="10" data-testid="story-additional-actors-table">
              <ng-template #header>
                <tr>
                  <th>Name</th>
                  @if (canEdit()) { <th style="width: 60px"></th> }
                </tr>
              </ng-template>
              <ng-template #body let-a>
                <tr data-testid="story-additional-actor-row">
                  <td><a class="entity-link" data-testid="story-additional-actor-link" [routerLink]="['/projects', projectName, 'actors', a.id]">{{ a.name }}</a></td>
                  @if (canEdit()) {
                    <td><p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                                  data-testid="story-remove-additional-actor" [ariaLabel]="'Remove actor ' + a.name"
                                  (onClick)="onRemoveActor(a)" /></td>
                  }
                </tr>
              </ng-template>
            </p-table>
          } @else {
            <p class="empty-text">No actors associated.</p>
          }
        </div>
      }

      <app-entity-selector-dialog
        [visible]="showGoalSelector"
        [projectName]="projectName"
        entityType="Goal"
        [excludeIds]="existingGoalIds()"
        (selected)="onGoalSelected($event)"
        (closed)="showGoalSelector = false" />

      <app-entity-selector-dialog
        [visible]="showActorSelector"
        [projectName]="projectName"
        entityType="Actor"
        [excludeIds]="existingActorIds()"
        (selected)="onActorSelected($event)"
        (closed)="showActorSelector = false" />

      <app-annotations-section
        [projectName]="projectName"
        entityType="Story"
        [entityId]="storyId"
        [canEdit]="canEdit()" />

      <p-confirmDialog />
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-grid { display: grid; grid-template-columns: 120px 1fr; gap: 0.75rem 1rem; align-items: start; max-width: 700px; }
    .form-actions { margin-top: 1rem; }
    .section { margin-top: 1.5rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .section-header h3 { margin: 0; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
    .entity-link { cursor: pointer; color: var(--p-primary-color); text-decoration: underline; }
  `]
})
export class StoryEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  isNew = signal(true);
  storyName = signal('');
  story = signal<StoryDto | null>(null);
  errorMessage = signal<string | null>(null);
  saving = signal(false);
  canEdit = signal(false);
  canDelete = signal(false);

  actorOptions = signal<{label: string, value: string}[]>([]);

  name = '';
  text = '';
  storyType = 'Success';
  primaryActorName = '';
  showGoalSelector = false;
  showActorSelector = false;
  hasChanges = signal(false);
  storyTypeOptions = [
    { label: 'Success', value: 'Success' },
    { label: 'Exception', value: 'Exception' }
  ];

  projectName = '';
  storyId: number | null = null;
  private version: number | null = null;
  private originalName = '';
  private originalText = '';
  private originalStoryType = 'Success';
  private originalPrimaryActorName = '';
  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private storyService: StoryService,
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
      const idParam = params.get('storyId') ?? '';
      const newIsNew = idParam === 'new';

      // Reset the form synchronously for the new-story path BEFORE the two
      // awaits below (loadForProject and listActors). Otherwise typing during
      // either yield would later be clobbered by the reset and Angular
      // change-detection would clear the input. See term-editor.ts for the
      // same pattern.
      if (newIsNew) {
        this.isNew.set(true);
        this.story.set(null);
        this.name = '';
        this.text = '';
        this.storyType = 'Success';
        this.version = null;
      }

      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('Story'));
      this.canDelete.set(this.permissionService.canDelete('Story'));

      const actors = await this.actorService.listActors(this.projectName);
      this.actorOptions.set(actors.map(a => ({ label: a.name, value: a.name })));

      if (!newIsNew) {
        this.isNew.set(false);
        this.storyId = +idParam;
        this.loadStory();
      }
    });
  }

  hasUnsavedChanges(): boolean {
    return this.hasChanges();
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
    if (this.storyId) {
      void this.eventStreamService.removeSubscription('Story', this.storyId);
    }
    this.sseSub?.unsubscribe();
  }

  private async loadStory(): Promise<void> {
    try {
      const s = await this.storyService.getStory(this.projectName, this.storyId!);
      this.story.set(s);
      this.storyName.set(s.name);
      this.name = s.name;
      this.text = s.text;
      this.storyType = s.storyType;
      this.primaryActorName = s.primaryActorName ?? '';
      this.version = s.version;
      this.originalName = s.name;
      this.originalText = s.text;
      this.originalStoryType = s.storyType;
      this.originalPrimaryActorName = s.primaryActorName ?? '';
      this.hasChanges.set(false);
    } catch {
      this.errorMessage.set('Failed to load story.');
    }
    if (this.storyId && !this.sseSub) {
      void this.eventStreamService.addSubscription('Story', this.storyId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'Story' && envelope.targetId === this.storyId) {
          void this.loadStory();
        }
      });
    }
  }

  trackChanges(): void {
    this.hasChanges.set(
      this.name !== this.originalName ||
      this.text !== this.originalText ||
      this.storyType !== this.originalStoryType ||
      this.primaryActorName !== this.originalPrimaryActorName
    );
  }

  existingGoalIds(): number[] {
    return (this.story()?.goals ?? [])
      .filter(g => g.id != null)
      .map(g => g.id!);
  }

  existingActorIds(): number[] {
    return (this.story()?.actors ?? [])
      .filter(a => a.id != null)
      .map(a => a.id!);
  }

  async onSave(): Promise<void> {
    this.saving.set(true);
    this.errorMessage.set(null);
    try {
      const input: Record<string, unknown> = {
        projectName: this.projectName,
        name: this.name,
        text: this.text,
        storyTypeName: this.storyType,
        primaryActorName: this.primaryActorName || null,
      };
      if (this.storyId != null) input['storyId'] = this.storyId;
      if (this.version != null) input['version'] = this.version;
      const result = await this.commandService.execute('EditStory', input);
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Story saved.' });
        if (this.isNew()) {
          this.projectService.notifyTreeChanged();
          if (result.entity) {
            const saved = result.entity as StoryDto;
            this.hasChanges.set(false);
            this.router.navigate(['/projects', this.projectName, 'stories', saved.id]);
          }
        } else {
          this.originalName = this.name;
          this.originalText = this.text;
          this.originalStoryType = this.storyType;
          this.originalPrimaryActorName = this.primaryActorName;
          this.hasChanges.set(false);
          await this.loadStory();
        }
      } else {
        this.errorMessage.set(result.error ?? 'Save failed.');
      }
    } catch {
      this.errorMessage.set('An unexpected error occurred.');
    } finally {
      this.saving.set(false);
    }
  }

  onCopy(): void {
    this.confirmationService.confirm({
      message: 'Create a copy of this story?',
      accept: async () => {
        const result = await this.commandService.execute('CopyStory', {
          projectName: this.projectName,
          storyId: this.storyId
        });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as StoryDto;
          this.router.navigate(['/projects', this.projectName, 'stories', copy.id]);
        } else {
          this.errorMessage.set(result.error ?? 'Copy failed.');
        }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this story?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteStory', {
          projectName: this.projectName,
          storyId: this.storyId,
          version: this.version
        });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          this.router.navigate(['/projects', this.projectName, 'stories']);
        } else {
          this.errorMessage.set(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  async onGoalSelected(ref: EntityReferenceDto): Promise<void> {
    this.showGoalSelector = false;
    try {
      const result = await this.commandService.execute('AddGoalToGoalContainer', {
        projectName: this.projectName,
        goalContainerId: this.storyId,
        goalId: ref.id,
        containerType: 'Story'
      });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Goal added', detail: 'Goal added.' });
        this.story.update(s => s ? {
          ...s,
          goals: [...(s.goals ?? []), ref].sort((a, b) => a.name.localeCompare(b.name))
        } : s);
      } else {
        this.errorMessage.set(result.error ?? 'Failed to add goal.');
      }
    } catch {
      this.errorMessage.set('Failed to add goal.');
    }
  }

  async onRemoveGoal(goalRef: EntityReferenceDto): Promise<void> {
    try {
      const result = await this.commandService.execute('RemoveGoalFromGoalContainer', {
        projectName: this.projectName,
        goalContainerId: this.storyId,
        goalId: goalRef.id,
        containerType: 'Story'
      });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Goal removed', detail: 'Goal removed.' });
        this.story.update(s => s ? {
          ...s,
          goals: (s.goals ?? []).filter(g => g.id !== goalRef.id)
        } : s);
      } else {
        this.errorMessage.set(result.error ?? 'Failed to remove goal.');
      }
    } catch {
      this.errorMessage.set('Failed to remove goal.');
    }
  }

  async onActorSelected(ref: EntityReferenceDto): Promise<void> {
    this.showActorSelector = false;
    try {
      const result = await this.commandService.execute('AddActorToActorContainer', {
        projectName: this.projectName,
        actorContainerId: this.storyId,
        actorId: ref.id
      });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Actor added', detail: 'Actor added.' });
        this.story.update(s => s ? {
          ...s,
          actors: [...(s.actors ?? []), ref].sort((a, b) => a.name.localeCompare(b.name))
        } : s);
      } else {
        this.errorMessage.set(result.error ?? 'Failed to add actor.');
      }
    } catch {
      this.errorMessage.set('Failed to add actor.');
    }
  }

  async onRemoveActor(actorRef: EntityReferenceDto): Promise<void> {
    try {
      const result = await this.commandService.execute('RemoveActorFromActorContainer', {
        projectName: this.projectName,
        actorContainerId: this.storyId,
        actorId: actorRef.id
      });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Actor removed', detail: 'Actor removed.' });
        this.story.update(s => s ? {
          ...s,
          actors: (s.actors ?? []).filter(a => a.id !== actorRef.id)
        } : s);
      } else {
        this.errorMessage.set(result.error ?? 'Failed to remove actor.');
      }
    } catch {
      this.errorMessage.set('Failed to remove actor.');
    }
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'stories']);
  }
}
