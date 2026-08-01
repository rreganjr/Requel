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
import { Component, computed, ElementRef, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
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
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { GoalDto, GoalRelationDto } from '../../models/goal';
import { EntityReferenceDto } from '../../models/entity-reference';
import { GoalService } from '../../core/goal.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { TagSelectorComponent } from '../../shared/tag-selector';

@Component({
  selector: 'app-goal-editor',
  standalone: true,
  imports: [PageHeaderComponent, RouterLink, FormsModule, ButtonModule, InputText, TextareaModule, SelectModule,
            TableModule, MessageModule, DialogModule, ConfirmDialogModule, EntitySelectorDialogComponent,
            AnnotationsSectionComponent, TagSelectorComponent],
  providers: [ConfirmationService],
  template: `
    <div class="goal-editor" data-testid="goal-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Goal' : goalName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" data-testid="goal-back"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary" data-testid="goal-copy"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete()) {
              <p-button label="Delete" icon="pi pi-trash" severity="danger" data-testid="goal-delete"
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
        <input id="name" pInputText [(ngModel)]="name" placeholder="Goal name" />

        <label for="text">Description</label>
        <textarea id="text" pTextarea [(ngModel)]="text" rows="6"
                  placeholder="Goal description"></textarea>
      </div>

      <div class="form-actions">
        <p-button label="Save" icon="pi pi-check" data-testid="goal-save" (onClick)="onSave()" [loading]="saving()" />
      </div>

      <!-- Relations: outgoing (this goal supports/conflicts with...) -->
      @if (!isNew() && goal()) {
        <div class="section">
          <div class="section-header">
            <h3>This Goal's Relations</h3>
            @if (canEdit()) {
              <p-button #addRelationBtn label="Add Relation" icon="pi pi-plus" size="small" data-testid="goal-add-relation"
                        (onClick)="showRelationSelector = true" />
            }
          </div>

          @if (goal()!.relationsFromThisGoal?.length) {
            <p-table [value]="goal()!.relationsFromThisGoal!" [rows]="10">
              <ng-template #header>
                <tr>
                  <th>Goal</th>
                  <th>Type</th>
                  @if (canEdit()) { <th class="col-actions"></th> }
                </tr>
              </ng-template>
              <ng-template #body let-r>
                <tr data-testid="goal-relation-row">
                  <td><a class="entity-link" [routerLink]="['/projects', projectName, 'goals', r.goalId]">{{ r.goalName }}</a></td>
                  <td>{{ r.relationType }}</td>
                  @if (canEdit()) {
                    <td><p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                                  data-testid="goal-remove-relation" [ariaLabel]="'Remove relation to ' + r.goalName"
                                  (onClick)="onDeleteRelation(r)" /></td>
                  }
                </tr>
              </ng-template>
            </p-table>
          } @else {
            <p class="empty-text">No relations defined.</p>
          }

          <!-- Incoming relations (other goals relate to this one) -->
          @if (goal()!.relationsToThisGoal?.length) {
            <h4>Related To This Goal</h4>
            <p-table [value]="goal()!.relationsToThisGoal!" [rows]="10">
              <ng-template #header>
                <tr>
                  <th>Goal</th>
                  <th>Type</th>
                </tr>
              </ng-template>
              <ng-template #body let-r>
                <tr>
                  <td><a class="entity-link" [routerLink]="['/projects', projectName, 'goals', r.goalId]">{{ r.goalName }}</a></td>
                  <td>{{ r.relationType }}</td>
                </tr>
              </ng-template>
            </p-table>
          }
        </div>

        <!-- Referenced By -->
        @if (goal()!.referencedBy?.length) {
          <div class="section">
            <h3>Referenced By</h3>
            <p-table [value]="goal()!.referencedBy!" [rows]="10">
              <ng-template #header>
                <tr>
                  <th>Type</th>
                  <th>Name</th>
                </tr>
              </ng-template>
              <ng-template #body let-ref>
                <tr>
                  <td>{{ ref.entityType }}</td>
                  <td>{{ ref.name }}</td>
                </tr>
              </ng-template>
            </p-table>
          </div>
        }
      }

      <!-- Add Relation Dialog -->
      <app-entity-selector-dialog
        [visible]="showRelationSelector"
        [projectName]="projectName"
        entityType="Goal"
        [excludeIds]="excludeGoalIds()"
        (selected)="onRelationGoalSelected($event)"
        (closed)="showRelationSelector = false" />

      <!-- Relation Type Dialog -->
      <p-dialog [visible]="relationDialogVisible()" (visibleChange)="relationDialogVisible.set($event)"
                (onHide)="onRelationDialogHide()" [modal]="true" [focusOnShow]="true" [dismissableMask]="true"
                closeAriaLabel="Close" [style]="{ width: '25rem' }" appendTo="body" [header]="relationDialogHeader()"
                data-testid="goal-relation-type-dialog">
        <div class="dialog-body">
          <p-select [(ngModel)]="newRelationType" data-testid="goal-relation-type-select" [options]="relationTypeOptions"
                    placeholder="Select relation type" appendTo="body" />
          <div class="dialog-actions">
            <p-button label="Add" icon="pi pi-check" data-testid="goal-relation-add" (onClick)="onConfirmRelation()" />
            <p-button label="Cancel" severity="secondary" [outlined]="true"
                      (onClick)="relationDialogVisible.set(false)" />
          </div>
        </div>
      </p-dialog>

      @if (!isNew()) {
        <app-tag-selector
          [projectName]="projectName"
          entityType="Goal"
          [entityId]="goalId"
          [canEdit]="canEdit()" />
      }

      <app-annotations-section
        [projectName]="projectName"
        entityType="Goal"
        [entityId]="goalId"
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
    h4 { margin: 1rem 0 0.5rem; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
    .entity-link { cursor: pointer; color: var(--p-primary-color); text-decoration: underline; }
    .dialog-body { display: flex; flex-direction: column; }
    .dialog-actions { display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 1rem; }
  `]
})
export class GoalEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  isNew = signal(true);
  goalName = signal('');
  goal = signal<GoalDto | null>(null);
  errorMessage = signal<string | null>(null);
  saving = signal(false);
  canEdit = signal(false);
  canDelete = signal(false);
  pendingRelationGoal = signal<EntityReferenceDto | null>(null);
  relationDialogVisible = signal(false);
  relationDialogHeader = computed(() => {
    const ref = this.pendingRelationGoal();
    return ref ? `Relation to "${ref.name}"` : '';
  });

  @ViewChild('addRelationBtn', { read: ElementRef }) addRelationBtn?: ElementRef<HTMLElement>;

  name = '';
  text = '';
  private originalName = '';
  private originalText = '';
  showRelationSelector = false;
  newRelationType = 'Supports';
  relationTypeOptions = [
    { label: 'Supports', value: 'Supports' },
    { label: 'Conflicts', value: 'Conflicts' }
  ];

  projectName = '';
  goalId: number | null = null;
  private version: number | null = null;
  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private goalService: GoalService,
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
      const idParam = params.get('goalId') ?? '';
      const newIsNew = idParam === 'new';

      // Reset the form synchronously for the new-goal path BEFORE the
      // loadForProject await. Otherwise typing during the yield would later
      // be clobbered by the reset and Angular change-detection would clear
      // the input. See term-editor.ts for the same pattern.
      if (newIsNew) {
        this.isNew.set(true);
        this.goal.set(null);
        this.name = '';
        this.text = '';
        this.originalName = '';
        this.originalText = '';
        this.version = null;
      }

      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('Goal'));
      this.canDelete.set(this.permissionService.canDelete('Goal'));

      if (!newIsNew) {
        this.isNew.set(false);
        this.goalId = +idParam;
        this.loadGoal();
      }
    });
  }

  hasUnsavedChanges(): boolean {
    return this.name !== this.originalName || this.text !== this.originalText;
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
    if (this.goalId) {
      void this.eventStreamService.removeSubscription('Goal', this.goalId);
    }
    this.sseSub?.unsubscribe();
  }

  private async loadGoal(fromSSE = false): Promise<void> {
    try {
      const g = await this.goalService.getGoal(this.projectName, this.goalId!);
      // Don't overwrite unsaved user edits when called from an SSE notification.
      if (fromSSE && this.hasUnsavedChanges()) {
        return;
      }
      this.goal.set(g);
      this.goalName.set(g.name);
      this.name = g.name;
      this.text = g.text;
      this.originalName = g.name;
      this.originalText = g.text;
      this.version = g.version;
    } catch {
      this.errorMessage.set('Failed to load goal.');
    }
    if (this.goalId && !this.sseSub) {
      void this.eventStreamService.addSubscription('Goal', this.goalId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'Goal' && envelope.targetId === this.goalId) {
          void this.loadGoal(true);
        }
      });
    }
  }

  excludeGoalIds(): number[] {
    const ids: number[] = [];
    if (this.goalId) ids.push(this.goalId);
    const g = this.goal();
    if (g?.relationsFromThisGoal) {
      ids.push(...g.relationsFromThisGoal.map(r => r.goalId));
    }
    return ids;
  }

  async onSave(): Promise<void> {
    this.saving.set(true);
    this.errorMessage.set(null);
    try {
      const input: Record<string, unknown> = {
        projectName: this.projectName,
        name: this.name,
        text: this.text,
      };
      if (this.goalId != null) input['goalId'] = this.goalId;
      if (this.version != null) input['version'] = this.version;
      const result = await this.commandService.execute('EditGoal', input);
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Goal saved.' });
        if (this.isNew()) {
          this.projectService.notifyTreeChanged();
          if (result.entity) {
            const saved = result.entity as GoalDto;
            // Reset originals before navigation so CanDeactivate guard doesn't fire
            this.originalName = this.name;
            this.originalText = this.text;
            this.router.navigate(['/projects', this.projectName, 'goals', saved.id]);
          }
        } else {
          await this.loadGoal(); // resets originalName/originalText
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
      message: 'Create a copy of this goal?',
      accept: async () => {
        const result = await this.commandService.execute('CopyGoal', {
          projectName: this.projectName,
          goalId: this.goalId
        });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as GoalDto;
          this.router.navigate(['/projects', this.projectName, 'goals', copy.id]);
        } else {
          this.errorMessage.set(result.error ?? 'Copy failed.');
        }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this goal?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteGoal', {
          projectName: this.projectName,
          goalId: this.goalId,
          version: this.version
        });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          this.router.navigate(['/projects', this.projectName, 'goals']);
        } else {
          this.errorMessage.set(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  onRelationGoalSelected(ref: EntityReferenceDto): void {
    this.pendingRelationGoal.set(ref);
    this.newRelationType = 'Supports';
    this.relationDialogVisible.set(true);
  }

  /**
   * Called after the relation-type dialog closes by any path (Add, Cancel, Escape, mask).
   * Clears the pending goal and restores focus to the "Add Relation" opener — the dialog is
   * opened programmatically after the entity selector closes, so PrimeNG's default
   * restore-to-last-focused-element has no stable target here.
   */
  onRelationDialogHide(): void {
    this.pendingRelationGoal.set(null);
    this.addRelationBtn?.nativeElement.querySelector('button')?.focus();
  }

  async onConfirmRelation(): Promise<void> {
    const ref = this.pendingRelationGoal();
    if (!ref) return;
    this.relationDialogVisible.set(false);

    const result = await this.commandService.execute('EditGoalRelation', {
      projectName: this.projectName,
      fromGoalName: this.name,
      toGoalName: ref.name,
      relationType: this.newRelationType
    });
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Relation added', detail: 'Goal relation added.' });
      await this.loadGoal();
    } else {
      this.errorMessage.set(result.error ?? 'Failed to add relation.');
    }
  }

  async onDeleteRelation(relation: GoalRelationDto): Promise<void> {
    const result = await this.commandService.execute('DeleteGoalRelation', {
      projectName: this.projectName,
      goalRelationId: relation.id,
      version: relation.version
    });
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: 'Relation removed', detail: 'Goal relation removed.' });
      await this.loadGoal();
    } else {
      this.errorMessage.set(result.error ?? 'Failed to delete relation.');
    }
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'goals']);
  }
}
