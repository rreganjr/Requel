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
import { PageHeaderComponent } from '../../shared/page-header';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Location } from '@angular/common';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { SelectModule } from 'primeng/select';
import { ConfirmationService, MessageService } from 'primeng/api';
import { UseCaseDto } from '../../models/use-case';
import { GoalDto } from '../../models/goal';
import { ActorDto } from '../../models/actor';
import { StoryDto } from '../../models/story';
import { ScenarioDto } from '../../models/scenario';
import { EntityReferenceDto } from '../../models/entity-reference';
import { UseCaseService } from '../../core/use-case.service';
import { ActorService } from '../../core/actor.service';
import { ScenarioService } from '../../core/scenario.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';

@Component({
  selector: 'app-use-case-editor',
  standalone: true,
  imports: [PageHeaderComponent, RouterLink, FormsModule, ButtonModule, InputText, TextareaModule, MessageModule,
            ConfirmDialogModule, TableModule, TooltipModule, SelectModule,
            EntitySelectorDialogComponent, AnnotationsSectionComponent],
  providers: [ConfirmationService],
  template: `
    <div class="use-case-editor" data-testid="use-case-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? 'New Use Case' : useCaseName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary"
                    [outlined]="true" (onClick)="onBack()" />
          @if (!isNew()) {
            @if (canEdit()) {
              <p-button label="Copy" icon="pi pi-copy" severity="secondary"
                        [outlined]="true" (onClick)="onCopy()" />
            }
            @if (canDelete()) {
              <p-button label="Delete" icon="pi pi-trash" severity="danger"
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
        <input id="name" pInputText [(ngModel)]="name" placeholder="Use case name"
               (ngModelChange)="trackChanges()" />

        <label for="primaryActor">Primary Actor</label>
        <p-select id="primaryActor" inputId="useCasePrimaryActorInput"
                  data-testid="use-case-primary-actor"
                  [(ngModel)]="primaryActorName"
                  [options]="actorOptions()"
                  optionLabel="label"
                  optionValue="value"
                  [showClear]="true"
                  [pt]="{ clearIcon: { 'data-testid': 'use-case-primary-actor-clear' } }"
                  placeholder="Select primary actor"
                  (ngModelChange)="trackChanges()"
                  styleClass="w-full" />

        <label for="text">Description</label>
        <textarea id="text" pTextarea [(ngModel)]="text" rows="4"
                  placeholder="Use case description"
                  (ngModelChange)="trackChanges()"></textarea>
      </div>

      <div class="form-actions">
        <p-button label="Save" icon="pi pi-check" data-testid="use-case-save"
                  (onClick)="onSave()" [loading]="saving()"
                  [disabled]="!isNew() && !hasChanges()" />
      </div>

      <!-- Primary Scenario section -->
      @if (!isNew()) {
        <div class="section">
          <div class="section-header">
            <h3>Primary Scenario</h3>
          </div>
          @if (!useCase()?.scenarioId) {
            <p class="no-scenario-hint">No primary scenario yet.</p>
            @if (canEdit()) {
              <div class="primary-scenario-actions">
                <p-button label="Create New" icon="pi pi-plus" size="small"
                          pTooltip="Create a primary scenario using the use case name"
                          (onClick)="createPrimaryScenario()" [loading]="saving()" />
                <p-button label="Select Existing" icon="pi pi-search" size="small"
                          severity="secondary" [outlined]="true"
                          pTooltip="Pick an existing Primary-type scenario"
                          (onClick)="showPrimaryScenarioSelector = true" />
              </div>
            }
          } @else {
            <div class="primary-scenario-card" data-testid="use-case-primary-scenario-card">
              <div class="primary-scenario-name" data-testid="use-case-primary-scenario-name">
                <a class="entity-link" data-testid="use-case-primary-scenario-link"
                   [routerLink]="['/projects', projectName, 'scenarios', useCase()!.scenarioId!]">
                  {{ useCase()!.scenarioName ?? 'Primary Scenario' }}
                </a>
                <span class="step-count">({{ useCase()!.scenarioStepCount ?? 0 }} steps)</span>
                <p-button label="Open in Editor" icon="pi pi-arrow-right" size="small"
                          data-testid="use-case-open-primary-scenario"
                          severity="secondary" [outlined]="true"
                          (onClick)="navigateTo('scenarios', useCase()!.scenarioId!)" />
              </div>
              @if (primaryScenario()?.steps?.length) {
                <p-table [value]="primaryScenario()!.steps!" styleClass="p-datatable-sm" [rowHover]="false">
                  <ng-template pTemplate="header">
                    <tr><th style="width:2.5rem">#</th><th>Step</th><th style="width:8rem">Type</th></tr>
                  </ng-template>
                  <ng-template pTemplate="body" let-step let-i="rowIndex">
                    <tr>
                      <td>{{ i + 1 }}</td>
                      <td>{{ step.name }}</td>
                      <td>{{ step.scenarioType }}</td>
                    </tr>
                  </ng-template>
                </p-table>
              } @else {
                <p class="no-scenario-hint">No steps yet — open in editor to add them.</p>
              }
            </div>
          }
        </div>

        <!-- Additional Scenarios section -->
        <div class="section">
          <div class="section-header">
            <h3>Additional Scenarios</h3>
            @if (canEdit()) {
              <p-button label="Add Scenario" icon="pi pi-plus" size="small"
                        data-testid="use-case-add-scenario"
                        severity="secondary" [outlined]="true"
                        (onClick)="showScenarioSelector = true" />
            }
          </div>
          <p-table [value]="additionalScenarios()" styleClass="p-datatable-sm"
                   data-testid="use-case-scenarios-table" [rowHover]="true">
            <ng-template pTemplate="header">
              <tr><th>Name</th><th>Type</th><th style="width:4rem"></th></tr>
            </ng-template>
            <ng-template pTemplate="body" let-s>
              <tr data-testid="use-case-scenario-row">
                <td><a class="entity-link" data-testid="use-case-scenario-link"
                       [routerLink]="['/projects', projectName, 'scenarios', s.id]">{{ s.name }}</a></td>
                <td>{{ s.scenarioType }}</td>
                <td>
                  @if (canEdit()) {
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
                              data-testid="use-case-remove-scenario" [ariaLabel]="'Remove scenario ' + s.name"
                              size="small" pTooltip="Remove scenario"
                              (onClick)="removeScenario(s)" />
                  }
                </td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr><td colspan="3" style="text-align:center">No additional scenarios.</td></tr>
            </ng-template>
          </p-table>
        </div>
      }

      <!-- Primary scenario selector (shows only Primary-type scenarios) -->
      <app-entity-selector-dialog
        [visible]="showPrimaryScenarioSelector"
        [projectName]="projectName"
        entityType="Scenario"
        [excludeIds]="primaryScenarioExcludeIds()"
        [includeTypes]="['Primary']"
        (selected)="selectPrimaryScenario($event)"
        (closed)="showPrimaryScenarioSelector = false" />

      <!-- Additional scenario selector (excludes Primary-type and already-added scenarios) -->
      <app-entity-selector-dialog
        [visible]="showScenarioSelector"
        [projectName]="projectName"
        entityType="Scenario"
        [excludeIds]="additionalScenarioIds()"
        [excludeTypes]="['Primary']"
        (selected)="addScenario($event)"
        (closed)="showScenarioSelector = false" />

      <!-- Goals sub-table -->
      @if (!isNew()) {
        <div class="section">
          <div class="section-header">
            <h3>Goals</h3>
            @if (canEdit()) {
              <p-button label="Add Goal" icon="pi pi-plus" size="small"
                        data-testid="use-case-add-goal"
                        severity="secondary" [outlined]="true"
                        (onClick)="showGoalSelector = true" />
            }
          </div>
          <p-table [value]="goals()" styleClass="p-datatable-sm"
                   data-testid="use-case-goals-table" [rowHover]="canEdit()">
            <ng-template pTemplate="header">
              <tr><th>Name</th><th style="width:4rem"></th></tr>
            </ng-template>
            <ng-template pTemplate="body" let-goal>
              <tr data-testid="use-case-goal-row">
                <td>
                  <a class="entity-link" data-testid="use-case-goal-link"
                     [routerLink]="['/projects', projectName, 'goals', goal.id]">{{ goal.name }}</a>
                </td>
                <td>
                  @if (canEdit()) {
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
                              data-testid="use-case-remove-goal" [ariaLabel]="'Remove goal ' + goal.name"
                              size="small" pTooltip="Remove goal"
                              (onClick)="removeGoal(goal)" />
                  }
                </td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr><td colspan="2" style="text-align:center">No goals.</td></tr>
            </ng-template>
          </p-table>
        </div>

        <!-- Stories sub-table -->
        <div class="section">
          <div class="section-header">
            <h3>Stories</h3>
            @if (canEdit()) {
              <p-button label="Add Story" icon="pi pi-plus" size="small"
                        data-testid="use-case-add-story"
                        severity="secondary" [outlined]="true"
                        (onClick)="showStorySelector = true" />
            }
          </div>
          <p-table [value]="stories()" styleClass="p-datatable-sm"
                   data-testid="use-case-stories-table" [rowHover]="canEdit()">
            <ng-template pTemplate="header">
              <tr><th>Name</th><th>Type</th><th style="width:4rem"></th></tr>
            </ng-template>
            <ng-template pTemplate="body" let-story>
              <tr data-testid="use-case-story-row">
                <td>
                  <a class="entity-link" data-testid="use-case-story-link"
                     [routerLink]="['/projects', projectName, 'stories', story.id]">{{ story.name }}</a>
                </td>
                <td>{{ story.storyType }}</td>
                <td>
                  @if (canEdit()) {
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
                              data-testid="use-case-remove-story" [ariaLabel]="'Remove story ' + story.name"
                              size="small" pTooltip="Remove story"
                              (onClick)="removeStory(story)" />
                  }
                </td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr><td colspan="3" style="text-align:center">No stories.</td></tr>
            </ng-template>
          </p-table>
        </div>

        <!-- Additional actors sub-table -->
        <div class="section">
          <div class="section-header">
            <h3>Additional Actors</h3>
            @if (canEdit()) {
              <p-button label="Add Actor" icon="pi pi-plus" size="small"
                        data-testid="use-case-add-actor"
                        severity="secondary" [outlined]="true"
                        (onClick)="showActorSelector = true" />
            }
          </div>
          <p-table [value]="actors()" styleClass="p-datatable-sm"
                   data-testid="use-case-actors-table" [rowHover]="canEdit()">
            <ng-template pTemplate="header">
              <tr><th>Name</th><th style="width:4rem"></th></tr>
            </ng-template>
            <ng-template pTemplate="body" let-actor>
              <tr data-testid="use-case-actor-row">
                <td>
                  <a class="entity-link" data-testid="use-case-actor-link"
                     [routerLink]="['/projects', projectName, 'actors', actor.id]">{{ actor.name }}</a>
                </td>
                <td>
                  @if (canEdit()) {
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
                              data-testid="use-case-remove-actor" [ariaLabel]="'Remove actor ' + actor.name"
                              size="small" pTooltip="Remove actor"
                              (onClick)="removeActor(actor)" />
                  }
                </td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr><td colspan="2" style="text-align:center">No additional actors.</td></tr>
            </ng-template>
          </p-table>
        </div>
      }

      <!-- Entity selector dialogs -->
      <app-entity-selector-dialog
        [visible]="showGoalSelector"
        [projectName]="projectName"
        entityType="Goal"
        [excludeIds]="goalIds()"
        (selected)="addGoal($event)"
        (closed)="showGoalSelector = false" />

      <app-entity-selector-dialog
        [visible]="showStorySelector"
        [projectName]="projectName"
        entityType="Story"
        [excludeIds]="storyIds()"
        (selected)="addStory($event)"
        (closed)="showStorySelector = false" />

      <app-entity-selector-dialog
        [visible]="showActorSelector"
        [projectName]="projectName"
        entityType="Actor"
        [excludeIds]="actorIds()"
        (selected)="addActorToList($event)"
        (closed)="showActorSelector = false" />

      <app-annotations-section
        [projectName]="projectName"
        entityType="UseCase"
        [entityId]="useCaseId"
        [canEdit]="canEdit()" />

      <p-confirmDialog />
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-grid { display: grid; grid-template-columns: 130px 1fr; gap: 0.75rem 1rem; align-items: start; max-width: 700px; }
    .form-actions { margin-top: 1rem; }
    .section { margin-top: 1.5rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .section-header h3 { margin: 0; }
    .entity-link { cursor: pointer; color: var(--p-primary-color); text-decoration: underline; }
    .no-scenario-hint { color: var(--p-text-muted-color); font-style: italic; margin: 0.5rem 0; }
    .primary-scenario-actions { display: flex; gap: 0.5rem; margin-top: 0.25rem; }
    .primary-scenario-card { display: flex; flex-direction: column; gap: 0.5rem; }
    .primary-scenario-name { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; }
    .step-count { color: var(--p-text-muted-color); font-size: 0.875rem; }
  `]
})
export class UseCaseEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  isNew = signal(true);
  useCaseName = signal('');
  useCase = signal<UseCaseDto | null>(null);
  errorMessage = signal<string | null>(null);
  saving = signal(false);
  canEdit = signal(false);
  canDelete = signal(false);
  hasChanges = signal(false);
  goals = signal<GoalDto[]>([]);
  stories = signal<StoryDto[]>([]);
  actors = signal<ActorDto[]>([]);
  additionalScenarios = signal<ScenarioDto[]>([]);
  primaryScenario = signal<ScenarioDto | null>(null);
  actorOptions = signal<{label: string, value: string}[]>([]);

  // Derived id sets used as excludeIds for the entity selector dialogs
  goalIds = computed(() => this.goals().map(g => g.id).filter((id): id is number => id != null));
  storyIds = computed(() => this.stories().map(s => s.id).filter((id): id is number => id != null));
  actorIds = computed(() => this.actors().map(a => a.id).filter((id): id is number => id != null));
  // Exclude already-selected primary ID from the primary selector
  primaryScenarioExcludeIds = computed(() => {
    const id = this.useCase()?.scenarioId;
    return id != null ? [id] : [];
  });
  // Exclude primary ID + additional scenario IDs from the additional selector
  additionalScenarioIds = computed(() => [
    ...this.primaryScenarioExcludeIds(),
    ...this.additionalScenarios().map(s => s.id).filter((id): id is number => id != null)
  ]);

  name = '';
  text = '';
  primaryActorName = '';
  showGoalSelector = false;
  showStorySelector = false;
  showActorSelector = false;
  showScenarioSelector = false;
  showPrimaryScenarioSelector = false;

  projectName = '';
  useCaseId: number | null = null;
  private version: number | null = null;
  private originalName = '';
  private originalText = '';
  private originalPrimaryActorName = '';
  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private useCaseService: UseCaseService,
    private actorService: ActorService,
    private scenarioService: ScenarioService,
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
      this.canEdit.set(this.permissionService.canEdit('UseCase'));
      this.canDelete.set(this.permissionService.canDelete('UseCase'));

      // Load all actors for the primary actor dropdown
      const actors = await this.actorService.listActors(this.projectName);
      this.actorOptions.set(actors.map(a => ({ label: a.name, value: a.name })));

      const idParam = params.get('useCaseId') ?? '';
      if (idParam === 'new') {
        this.isNew.set(true);
      } else {
        this.isNew.set(false);
        this.useCaseId = +idParam;
        await this.loadUseCase();
      }
    });
  }

  hasUnsavedChanges(): boolean {
    return this.hasChanges();
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
    if (this.useCaseId) {
      void this.eventStreamService.removeSubscription('UseCase', this.useCaseId);
    }
    this.sseSub?.unsubscribe();
  }

  private async loadUseCase(): Promise<void> {
    try {
      const uc = await this.useCaseService.getUseCase(this.projectName, this.useCaseId!);
      this.useCase.set(uc);
      this.useCaseName.set(uc.name);
      this.name = uc.name;
      this.text = uc.text ?? '';
      this.primaryActorName = uc.primaryActorName ?? '';
      this.version = uc.version;
      this.originalName = uc.name;
      this.originalText = uc.text ?? '';
      this.originalPrimaryActorName = uc.primaryActorName ?? '';
      this.hasChanges.set(false);
      this.goals.set(uc.goals ?? []);
      this.stories.set(uc.stories ?? []);
      this.actors.set(uc.actors ?? []);
      this.additionalScenarios.set(uc.additionalScenarios ?? []);
      if (uc.scenarioId) {
        this.primaryScenario.set(await this.scenarioService.getScenario(this.projectName, uc.scenarioId));
      } else {
        this.primaryScenario.set(null);
      }
    } catch {
      this.errorMessage.set('Failed to load use case.');
    }
    if (this.useCaseId && !this.sseSub) {
      void this.eventStreamService.addSubscription('UseCase', this.useCaseId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'UseCase' && envelope.targetId === this.useCaseId) {
          void this.loadUseCase();
        }
      });
    }
  }

  trackChanges(): void {
    this.hasChanges.set(
      this.name !== this.originalName ||
      this.text !== this.originalText ||
      this.primaryActorName !== this.originalPrimaryActorName
    );
  }

  async onSave(): Promise<void> {
    this.saving.set(true);
    this.errorMessage.set(null);
    try {
      const input: Record<string, unknown> = {
        projectName: this.projectName,
        name: this.name,
        text: this.text || null,
        primaryActorName: this.primaryActorName || null,
      };
      if (this.version != null) input['version'] = this.version;
      if (this.useCaseId != null) input['useCaseId'] = this.useCaseId;

      const result = await this.commandService.execute('EditUseCase', input);
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Use case saved.' });
        if (this.isNew()) {
          this.projectService.notifyTreeChanged();
          const saved = result.entity as UseCaseDto;
          this.hasChanges.set(false);
          this.router.navigate(['/projects', this.projectName, 'use-cases', saved.id]);
        } else {
          this.originalName = this.name;
          this.originalText = this.text;
          this.originalPrimaryActorName = this.primaryActorName;
          this.hasChanges.set(false);
          await this.loadUseCase();
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

  /** Reload only the sub-collections without touching unsaved form fields. */
  private async refreshCollections(): Promise<void> {
    try {
      const uc = await this.useCaseService.getUseCase(this.projectName, this.useCaseId!);
      this.useCase.set(uc);
      this.goals.set(uc.goals ?? []);
      this.stories.set(uc.stories ?? []);
      this.actors.set(uc.actors ?? []);
      this.additionalScenarios.set(uc.additionalScenarios ?? []);
      if (uc.scenarioId) {
        this.primaryScenario.set(await this.scenarioService.getScenario(this.projectName, uc.scenarioId));
      } else {
        this.primaryScenario.set(null);
      }
    } catch {
      this.errorMessage.set('Failed to refresh.');
    }
  }

  async addGoal(ref: EntityReferenceDto): Promise<void> {
    this.showGoalSelector = false;
    const result = await this.commandService.execute('AddGoalToGoalContainer', {
      projectName: this.projectName, goalContainerId: this.useCaseId, goalId: ref.id,
      containerType: 'UseCase'
    });
    if (result.success) await this.refreshCollections();
    else this.errorMessage.set(result.error ?? 'Failed to add goal.');
  }

  async removeGoal(goal: GoalDto): Promise<void> {
    const result = await this.commandService.execute('RemoveGoalFromGoalContainer', {
      projectName: this.projectName, goalContainerId: this.useCaseId, goalId: goal.id,
      containerType: 'UseCase'
    });
    if (result.success) await this.refreshCollections();
    else this.errorMessage.set(result.error ?? 'Failed to remove goal.');
  }

  async addStory(ref: EntityReferenceDto): Promise<void> {
    this.showStorySelector = false;
    const result = await this.commandService.execute('AddStoryToStoryContainer', {
      projectName: this.projectName, storyContainerId: this.useCaseId, storyId: ref.id
    });
    if (result.success) await this.refreshCollections();
    else this.errorMessage.set(result.error ?? 'Failed to add story.');
  }

  async removeStory(story: StoryDto): Promise<void> {
    const result = await this.commandService.execute('RemoveStoryFromStoryContainer', {
      projectName: this.projectName, storyContainerId: this.useCaseId, storyId: story.id
    });
    if (result.success) await this.refreshCollections();
    else this.errorMessage.set(result.error ?? 'Failed to remove story.');
  }

  async addActorToList(ref: EntityReferenceDto): Promise<void> {
    this.showActorSelector = false;
    const result = await this.commandService.execute('AddActorToActorContainer', {
      projectName: this.projectName, actorContainerId: this.useCaseId, actorId: ref.id
    });
    if (result.success) await this.refreshCollections();
    else this.errorMessage.set(result.error ?? 'Failed to add actor.');
  }

  async removeActor(actor: ActorDto): Promise<void> {
    const result = await this.commandService.execute('RemoveActorFromActorContainer', {
      projectName: this.projectName, actorContainerId: this.useCaseId, actorId: actor.id
    });
    if (result.success) await this.refreshCollections();
    else this.errorMessage.set(result.error ?? 'Failed to remove actor.');
  }

  /** Save the use case — the backend auto-creates a primary scenario with the use case name. */
  async createPrimaryScenario(): Promise<void> {
    this.saving.set(true);
    this.errorMessage.set(null);
    try {
      const input: Record<string, unknown> = {
        projectName: this.projectName,
        name: this.name,
        text: this.text || null,
        primaryActorName: this.primaryActorName || null,
        useCaseId: this.useCaseId,
        version: this.version
      };
      const result = await this.commandService.execute('EditUseCase', input);
      if (result.success) {
        const saved = result.entity as UseCaseDto;
        await this.refreshCollections();
        if (saved.scenarioId) {
          this.router.navigate(['/projects', this.projectName, 'scenarios', saved.scenarioId]);
        }
      } else {
        this.errorMessage.set(result.error ?? 'Failed to create primary scenario.');
      }
    } catch {
      this.errorMessage.set('An unexpected error occurred.');
    } finally {
      this.saving.set(false);
    }
  }

  async selectPrimaryScenario(ref: EntityReferenceDto): Promise<void> {
    this.showPrimaryScenarioSelector = false;
    const result = await this.commandService.execute('SetPrimaryScenarioOnUseCase', {
      projectName: this.projectName,
      useCaseId: this.useCaseId,
      scenarioId: ref.id
    });
    if (result.success) await this.refreshCollections();
    else this.errorMessage.set(result.error ?? 'Failed to set primary scenario.');
  }

  async addScenario(ref: EntityReferenceDto): Promise<void> {
    this.showScenarioSelector = false;
    const result = await this.commandService.execute('AddScenarioToUseCase', {
      projectName: this.projectName,
      useCaseId: this.useCaseId,
      scenarioId: ref.id
    });
    if (result.success) await this.refreshCollections();
    else this.errorMessage.set(result.error ?? 'Failed to add scenario.');
  }

  async removeScenario(scenario: ScenarioDto): Promise<void> {
    const result = await this.commandService.execute('RemoveScenarioFromUseCase', {
      projectName: this.projectName,
      useCaseId: this.useCaseId,
      scenarioId: scenario.id
    });
    if (result.success) await this.refreshCollections();
    else this.errorMessage.set(result.error ?? 'Failed to remove scenario.');
  }

  navigateTo(type: string, id: number): void {
    this.router.navigate(['/projects', this.projectName, type, id]);
  }

  onCopy(): void {
    this.confirmationService.confirm({
      message: 'Create a copy of this use case?',
      accept: async () => {
        const result = await this.commandService.execute('CopyUseCase', {
          projectName: this.projectName, useCaseId: this.useCaseId
        });
        if (result.success && result.entity) {
          this.projectService.notifyTreeChanged();
          const copy = result.entity as UseCaseDto;
          this.router.navigate(['/projects', this.projectName, 'use-cases', copy.id]);
        } else {
          this.errorMessage.set(result.error ?? 'Copy failed.');
        }
      }
    });
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this use case?',
      accept: async () => {
        const result = await this.commandService.execute('DeleteUseCase', {
          projectName: this.projectName, useCaseId: this.useCaseId, version: this.version
        });
        if (result.success) {
          this.projectService.notifyTreeChanged();
          this.router.navigate(['/projects', this.projectName, 'use-cases']);
        } else {
          this.errorMessage.set(result.error ?? 'Delete failed.');
        }
      }
    });
  }

  onBack(): void {
    this.location.back();
  }
}
