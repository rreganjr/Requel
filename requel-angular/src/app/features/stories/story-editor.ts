import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
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
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';

@Component({
  selector: 'app-story-editor',
  standalone: true,
  imports: [FormsModule, ButtonModule, InputText, TextareaModule, SelectModule,
            TableModule, MessageModule, ConfirmDialogModule, EntitySelectorDialogComponent],
  providers: [ConfirmationService],
  template: `
    <div class="story-editor">
      <div class="page-header">
        <h2>{{ isNew() ? 'New Story' : storyName() }}</h2>
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
        <input id="name" pInputText [(ngModel)]="name" placeholder="Story name"
               (ngModelChange)="trackChanges()" />

        <label for="type">Type</label>
        <p-select id="type" [(ngModel)]="storyType" [options]="storyTypeOptions"
                  optionLabel="label" optionValue="value"
                  (ngModelChange)="trackChanges()" />

        <label for="text">Text</label>
        <textarea id="text" pTextarea [(ngModel)]="text" rows="8"
                  placeholder="Story text"
                  (ngModelChange)="trackChanges()"></textarea>
      </div>

      <div class="form-actions">
        <p-button label="Save" icon="pi pi-check" (onClick)="onSave()" [loading]="saving()"
                  [disabled]="!isNew() && !hasChanges()" />
      </div>

      <!-- Goals sub-table -->
      @if (!isNew() && story()) {
        <div class="section">
          <div class="section-header">
            <h3>Goals</h3>
            @if (canEdit()) {
              <p-button label="Add Goal" icon="pi pi-plus" size="small"
                        (onClick)="showGoalSelector = true" />
            }
          </div>

          @if (story()!.goals?.length) {
            <p-table [value]="story()!.goals!" [rows]="10">
              <ng-template #header>
                <tr>
                  <th>Name</th>
                  @if (canEdit()) { <th style="width: 60px"></th> }
                </tr>
              </ng-template>
              <ng-template #body let-g>
                <tr>
                  <td><a class="entity-link" (click)="navigateToGoal(g.id)">{{ g.name }}</a></td>
                  @if (canEdit()) {
                    <td><p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                                  (onClick)="onRemoveGoal(g)" /></td>
                  }
                </tr>
              </ng-template>
            </p-table>
          } @else {
            <p class="empty-text">No goals associated.</p>
          }
        </div>

        <!-- Actors sub-table (read-only for now — actors are managed in Phase 5) -->
        @if (story()!.actors?.length) {
          <div class="section">
            <h3>Actors</h3>
            <p-table [value]="story()!.actors!" [rows]="10">
              <ng-template #header>
                <tr><th>Name</th></tr>
              </ng-template>
              <ng-template #body let-a>
                <tr><td>{{ a.name }}</td></tr>
              </ng-template>
            </p-table>
          </div>
        }
      }

      <app-entity-selector-dialog
        [visible]="showGoalSelector"
        [projectName]="projectName"
        entityType="Goal"
        [excludeIds]="existingGoalIds()"
        (selected)="onGoalSelected($event)"
        (closed)="showGoalSelector = false" />

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
export class StoryEditorComponent implements OnInit, OnDestroy {
  isNew = signal(true);
  storyName = signal('');
  story = signal<StoryDto | null>(null);
  errorMessage = signal<string | null>(null);
  saving = signal(false);
  canEdit = signal(false);
  canDelete = signal(false);

  name = '';
  text = '';
  storyType = 'Success';
  showGoalSelector = false;
  hasChanges = signal(false);
  storyTypeOptions = [
    { label: 'Success', value: 'Success' },
    { label: 'Exception', value: 'Exception' }
  ];

  projectName = '';
  private storyId: number | null = null;
  private version: number | null = null;
  private originalName = '';
  private originalText = '';
  private originalStoryType = 'Success';
  private paramSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private storyService: StoryService,
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
      this.canEdit.set(this.permissionService.canEdit('Story'));
      this.canDelete.set(this.permissionService.canDelete('Story'));

      const idParam = params.get('storyId') ?? '';
      if (idParam === 'new') {
        this.isNew.set(true);
        this.story.set(null);
        this.name = '';
        this.text = '';
        this.storyType = 'Success';
        this.version = null;
      } else {
        this.isNew.set(false);
        this.storyId = +idParam;
        this.loadStory();
      }
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  private async loadStory(): Promise<void> {
    try {
      const s = await this.storyService.getStory(this.projectName, this.storyId!);
      this.story.set(s);
      this.storyName.set(s.name);
      this.name = s.name;
      this.text = s.text;
      this.storyType = s.storyType;
      this.version = s.version;
      this.originalName = s.name;
      this.originalText = s.text;
      this.originalStoryType = s.storyType;
      this.hasChanges.set(false);
    } catch {
      this.errorMessage.set('Failed to load story.');
    }
  }

  trackChanges(): void {
    this.hasChanges.set(
      this.name !== this.originalName ||
      this.text !== this.originalText ||
      this.storyType !== this.originalStoryType
    );
  }

  existingGoalIds(): number[] {
    return (this.story()?.goals ?? [])
      .filter(g => g.id != null)
      .map(g => g.id!);
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
      };
      if (this.version != null) input['version'] = this.version;
      const result = await this.commandService.execute('EditStory', input);
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Story saved.' });
        if (this.isNew()) {
          this.projectService.notifyTreeChanged();
          if (result.entity) {
            const saved = result.entity as StoryDto;
            this.router.navigate(['/projects', this.projectName, 'stories', saved.id]);
          }
        } else {
          this.originalName = this.name;
          this.originalText = this.text;
          this.originalStoryType = this.storyType;
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

  navigateToGoal(goalId: number): void {
    this.router.navigate(['/projects', this.projectName, 'goals', goalId]);
  }

  onBack(): void {
    this.router.navigate(['/projects', this.projectName, 'stories']);
  }
}
