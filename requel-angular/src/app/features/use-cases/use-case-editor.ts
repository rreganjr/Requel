import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { AutoCompleteModule, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { ConfirmationService, MessageService } from 'primeng/api';
import { UseCaseDto } from '../../models/use-case';
import { GoalDto } from '../../models/goal';
import { ActorDto } from '../../models/actor';
import { StoryDto } from '../../models/story';
import { ScenarioDto } from '../../models/scenario';
import { EntityReferenceDto } from '../../models/entity-reference';
import { UseCaseService } from '../../core/use-case.service';
import { ActorService } from '../../core/actor.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';

@Component({
  selector: 'app-use-case-editor',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputText, TextareaModule, MessageModule,
            ConfirmDialogModule, TableModule, TooltipModule, AutoCompleteModule,
            EntitySelectorDialogComponent],
  providers: [ConfirmationService],
  template: `
    <div class="use-case-editor">
      <div class="page-header">
        <h2>{{ isNew() ? 'New Use Case' : useCaseName() }}</h2>
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
        <p-autoComplete id="primaryActor"
                        [(ngModel)]="primaryActorName"
                        [suggestions]="actorSuggestions()"
                        (completeMethod)="filterActors($event)"
                        (ngModelChange)="trackChanges()"
                        [forceSelection]="false"
                        placeholder="Type actor name (creates if new)"
                        styleClass="w-full" />

        <label for="text">Description</label>
        <textarea id="text" pTextarea [(ngModel)]="text" rows="4"
                  placeholder="Use case description"
                  (ngModelChange)="trackChanges()"></textarea>
      </div>

      <div class="form-actions">
        <p-button label="Save" icon="pi pi-check" (onClick)="onSave()" [loading]="saving()"
                  [disabled]="!isNew() && !hasChanges()" />
      </div>

      <!-- Scenarios section -->
      @if (!isNew()) {
        <div class="section">
          <div class="section-header">
            <h3>Scenarios</h3>
            @if (canEdit()) {
              <p-button label="Add Scenario" icon="pi pi-plus" size="small"
                        severity="secondary" [outlined]="true"
                        (onClick)="showScenarioSelector = true" />
            }
          </div>
          <p-table [value]="allScenarios()" styleClass="p-datatable-sm" [rowHover]="true">
            <ng-template pTemplate="header">
              <tr><th>Name</th><th>Type</th><th style="width:4rem"></th></tr>
            </ng-template>
            <ng-template pTemplate="body" let-s>
              <tr>
                <td>
                  <a class="entity-link" (click)="navigateTo('scenarios', s.id)">{{ s.name }}</a>
                </td>
                <td>{{ s.scenarioType }}</td>
                <td>
                  @if (canEdit() && s.scenarioType !== 'Primary') {
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
                              size="small" pTooltip="Remove scenario"
                              (onClick)="removeScenario(s)" />
                  }
                </td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr><td colspan="3" style="text-align:center">No scenario yet. Save the use case to create one.</td></tr>
            </ng-template>
          </p-table>
        </div>

      }

      <app-entity-selector-dialog
        [visible]="showScenarioSelector"
        [projectName]="projectName"
        entityType="Scenario"
        [excludeIds]="scenarioIds()"
        [excludeTypes]="excludeScenarioTypes()"
        (selected)="addScenario($event)"
        (closed)="showScenarioSelector = false" />

      <!-- Goals sub-table -->
      @if (!isNew()) {
        <div class="section">
          <div class="section-header">
            <h3>Goals</h3>
            @if (canEdit()) {
              <p-button label="Add Goal" icon="pi pi-plus" size="small"
                        severity="secondary" [outlined]="true"
                        (onClick)="showGoalSelector = true" />
            }
          </div>
          <p-table [value]="goals()" styleClass="p-datatable-sm" [rowHover]="canEdit()">
            <ng-template pTemplate="header">
              <tr><th>Name</th><th style="width:4rem"></th></tr>
            </ng-template>
            <ng-template pTemplate="body" let-goal>
              <tr>
                <td>
                  <a class="entity-link"
                     (click)="navigateTo('goals', goal.id)">{{ goal.name }}</a>
                </td>
                <td>
                  @if (canEdit()) {
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
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
                        severity="secondary" [outlined]="true"
                        (onClick)="showStorySelector = true" />
            }
          </div>
          <p-table [value]="stories()" styleClass="p-datatable-sm" [rowHover]="canEdit()">
            <ng-template pTemplate="header">
              <tr><th>Name</th><th>Type</th><th style="width:4rem"></th></tr>
            </ng-template>
            <ng-template pTemplate="body" let-story>
              <tr>
                <td>
                  <a class="entity-link"
                     (click)="navigateTo('stories', story.id)">{{ story.name }}</a>
                </td>
                <td>{{ story.storyType }}</td>
                <td>
                  @if (canEdit()) {
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
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
                        severity="secondary" [outlined]="true"
                        (onClick)="showActorSelector = true" />
            }
          </div>
          <p-table [value]="actors()" styleClass="p-datatable-sm" [rowHover]="canEdit()">
            <ng-template pTemplate="header">
              <tr><th>Name</th><th style="width:4rem"></th></tr>
            </ng-template>
            <ng-template pTemplate="body" let-actor>
              <tr>
                <td>
                  <a class="entity-link"
                     (click)="navigateTo('actors', actor.id)">{{ actor.name }}</a>
                </td>
                <td>
                  @if (canEdit()) {
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
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
  `]
})
export class UseCaseEditorComponent implements OnInit, OnDestroy {
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
  actorSuggestions = signal<string[]>([]);

  // Primary scenario row + additional scenarios combined for the table
  allScenarios = computed<ScenarioDto[]>(() => {
    const uc = this.useCase();
    const primary: ScenarioDto[] = uc?.scenarioId
      ? [{ id: uc.scenarioId, version: 0, name: uc.scenarioName ?? 'Primary Scenario',
           text: null, scenarioType: 'Primary', createdBy: null, steps: null }]
      : [];
    return [...primary, ...this.additionalScenarios()];
  });

  // Derived id sets used as excludeIds for the entity selector dialogs
  goalIds = computed(() => this.goals().map(g => g.id).filter((id): id is number => id != null));
  storyIds = computed(() => this.stories().map(s => s.id).filter((id): id is number => id != null));
  actorIds = computed(() => this.actors().map(a => a.id).filter((id): id is number => id != null));
  // Exclude primary scenario + already-added additional scenarios from the selector
  scenarioIds = computed(() => this.allScenarios().map(s => s.id).filter((id): id is number => id != null));
  // Exclude 'Primary' type from selector once a primary scenario exists
  excludeScenarioTypes = computed(() => this.allScenarios().some(s => s.scenarioType === 'Primary') ? ['Primary'] : []);

  name = '';
  text = '';
  primaryActorName = '';
  showGoalSelector = false;
  showStorySelector = false;
  showActorSelector = false;
  showScenarioSelector = false;

  projectName = '';
  private useCaseId: number | null = null;
  private version: number | null = null;
  private originalName = '';
  private originalText = '';
  private originalPrimaryActorName = '';
  private allActorNames: string[] = [];
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private useCaseService: UseCaseService,
    private actorService: ActorService,
    private commandService: CommandService,
    private projectService: ProjectService,
    private permissionService: PermissionService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      await this.permissionService.loadForProject(this.projectName);
      this.canEdit.set(this.permissionService.canEdit('UseCase'));
      this.canDelete.set(this.permissionService.canDelete('UseCase'));

      // Load all actors once for autocomplete
      const actors = await this.actorService.listActors(this.projectName);
      this.allActorNames = actors.map(a => a.name);

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

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
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
    } catch {
      this.errorMessage.set('Failed to load use case.');
    }
  }

  filterActors(event: AutoCompleteCompleteEvent): void {
    const q = event.query.toLowerCase();
    this.actorSuggestions.set(
      this.allActorNames.filter(n => n.toLowerCase().includes(q))
    );
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
      this.goals.set(uc.goals ?? []);
      this.stories.set(uc.stories ?? []);
      this.actors.set(uc.actors ?? []);
      this.additionalScenarios.set(uc.additionalScenarios ?? []);
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

  navigateToScenario(): void {
    const uc = this.useCase();
    if (uc?.scenarioId) {
      this.router.navigate(['/projects', this.projectName, 'scenarios', uc.scenarioId]);
    }
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
