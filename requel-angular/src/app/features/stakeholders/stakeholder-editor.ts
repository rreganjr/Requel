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
import { AppCardComponent } from '../../shared/app-card';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { DirtyCheckable } from '../../core/dirty-check.guard';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { CheckboxModule } from 'primeng/checkbox';
import { TableModule } from 'primeng/table';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { StakeholderDto, StakeholderPermissionDto, UserStakeholderDetails } from '../../models/stakeholder';
import { EntityReferenceDto } from '../../models/entity-reference';
import { StakeholderService } from '../../core/stakeholder.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { UserService } from '../../core/user.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { EntitySelectorDialogComponent } from '../../shared/entity-selector-dialog';

interface PermissionGroup {
  entityType: string;
  permissions: { key: string; type: string; checked: boolean }[];
}

@Component({
  selector: 'app-stakeholder-editor',
  standalone: true,
  imports: [PageHeaderComponent, AppCardComponent, FormsModule, ButtonModule, InputText, TextareaModule, SelectModule,
            CheckboxModule, MessageModule, ConfirmDialogModule, TableModule,
            EntitySelectorDialogComponent],
  providers: [ConfirmationService],
  template: `
    <div class="stakeholder-editor">
      <div class="page-header">
        <app-page-header [title]="isNew() ? (isUserType() ? 'Add User Stakeholder' : 'Add Non-User Stakeholder') : stakeholderName()" />
        <div class="page-actions">
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary"
                    [outlined]="true" (onClick)="onBack()" />
          @if (canDelete() && !isNew()) {
            <p-button label="Delete" icon="pi pi-trash" severity="danger"
                      [outlined]="true" (onClick)="onDelete()" />
          }
        </div>
      </div>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" />
      }

      <app-card>
        <div class="form-grid">
          @if (isUserType()) {
            <label for="username">User</label>
            <p-select id="username" [(ngModel)]="username" [options]="userOptions()"
                      optionLabel="label" optionValue="value"
                      placeholder="Select a user" [filter]="true"
                      [disabled]="!isNew()" />

            @if (loadedUserDetails(); as ud) {
              <label>Email</label>
              <span class="readonly-field">{{ ud.emailAddress || '—' }}</span>

              <label>Phone</label>
              <span class="readonly-field">{{ ud.phoneNumber || '—' }}</span>
            }

            <label for="team">Team</label>
            <input id="team" pInputText [(ngModel)]="teamName" placeholder="Team name"
                   (ngModelChange)="trackChanges()" />
          }

          @if (isUserType() && permissionGroups().length > 0) {
            <div class="permissions-section">
              <h3>Permissions</h3>
              <div class="permission-grid">
                <div class="permission-header"></div>
                <div class="permission-header">Edit</div>
                <div class="permission-header">Delete</div>
                <div class="permission-header">Grant</div>
                @for (group of permissionGroups(); track group.entityType) {
                  <div class="permission-entity">{{ group.entityType }}</div>
                  @for (type of ['Edit', 'Delete', 'Grant']; track type) {
                    <div class="permission-check">
                      @if (getPermission(group, type); as perm) {
                        <p-checkbox [(ngModel)]="perm.checked" [binary]="true"
                                    [name]="perm.key" (onChange)="trackChanges()" />
                      }
                    </div>
                  }
                }
              </div>
            </div>
          }

          @if (!isUserType()) {
            <label for="name">Name</label>
            <input id="name" pInputText [(ngModel)]="stakeholderName" placeholder="Stakeholder name"
                   (ngModelChange)="trackChanges()" />

            <label for="text">Description</label>
            <textarea id="text" pTextarea [(ngModel)]="text" rows="4"
                      placeholder="Description of this stakeholder"
                      (ngModelChange)="trackChanges()"></textarea>
          }
        </div>

        <div class="form-actions">
          <p-button label="Save" icon="pi pi-check" (onClick)="onSave()" [loading]="saving()"
                    [disabled]="!isNew() && !hasChanges()" />
        </div>
      </app-card>

      @if (!isNew()) {
        <div class="section">
          <div class="section-header">
            <h3>Goals</h3>
            @if (canEditGoals()) {
              <p-button label="Add Goal" icon="pi pi-plus" size="small"
                        [text]="true" (onClick)="showGoalSelector = true" />
            }
          </div>

          <p-table [value]="goals()" [rowHover]="true">
            <ng-template #header>
              <tr>
                <th>Name</th>
                @if (canEditGoals()) { <th class="col-actions"></th> }
              </tr>
            </ng-template>
            <ng-template #body let-g>
              <tr>
                <td class="entity-link" (click)="onGoalClick(g)">{{ g.name }}</td>
                @if (canEditGoals()) {
                  <td>
                    <p-button icon="pi pi-times" severity="danger" [text]="true"
                              size="small" (onClick)="onRemoveGoal(g)" />
                  </td>
                }
              </tr>
            </ng-template>
            <ng-template #emptymessage>
              <tr><td [attr.colspan]="canEditGoals() ? 2 : 1" class="empty-text">No goals assigned.</td></tr>
            </ng-template>
          </p-table>
        </div>

        <app-entity-selector-dialog
          [visible]="showGoalSelector"
          [projectName]="projectName"
          entityType="Goal"
          [excludeIds]="goalIds()"
          (selected)="onGoalSelected($event)"
          (closed)="showGoalSelector = false" />
      }

      <p-confirmDialog />
    </div>
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-actions { display: flex; gap: 0.5rem; }
    .form-grid { display: grid; grid-template-columns: 120px 1fr; gap: 0.75rem 1rem; align-items: center; max-width: 600px; }
    .readonly-field { color: var(--p-text-color); padding: 0.5rem 0; }
    .form-actions { margin-top: 1rem; }
    .permissions-section { grid-column: 1 / -1; margin-top: 0.5rem; }
    .permissions-section h3 { margin: 0 0 0.5rem 0; font-size: 1rem; }
    .permission-grid { display: grid; grid-template-columns: 140px repeat(3, 60px); gap: 0.25rem 0.5rem; align-items: center; }
    .permission-header { font-weight: 600; font-size: 0.85rem; text-align: center; }
    .permission-entity { font-size: 0.9rem; }
    .permission-check { text-align: center; }
    .section { margin-top: 1.5rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .section-header h3 { margin: 0; }
    .entity-link { cursor: pointer; color: var(--p-primary-color); text-decoration: underline; }
    .empty-text { color: var(--p-text-secondary-color); font-style: italic; }
  `]
})
export class StakeholderEditorComponent implements OnInit, OnDestroy, DirtyCheckable {
  isNew = signal(true);
  isUserType = signal(true);
  stakeholderName = signal('');
  errorMessage = signal<string | null>(null);
  saving = signal(false);
  canDelete = signal(false);
  userOptions = signal<{ label: string; value: string }[]>([]);
  loadedUserDetails = signal<UserStakeholderDetails | null>(null);
  permissionGroups = signal<PermissionGroup[]>([]);
  goals = signal<EntityReferenceDto[]>([]);
  goalIds = computed(() => this.goals().map(g => g.id).filter((id): id is number => id != null));
  hasChanges = signal(false);

  username = '';
  teamName = '';
  text = '';
  showGoalSelector = false;

  private originalTeamName = '';
  private originalPermissionKeys = '';
  private originalName = '';
  private originalText = '';

  projectName = '';
  private stakeholderId: number | null = null;
  private version: number | null = null;
  private paramSub?: Subscription;
  private sseSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private stakeholderService: StakeholderService,
    private commandService: CommandService,
    private projectService: ProjectService,
    private userService: UserService,
    private permissionService: PermissionService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private eventStreamService: EventStreamService
  ) {}

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(async params => {
      this.projectName = params.get('name') ?? '';
      await this.permissionService.loadForProject(this.projectName);
      this.canDelete.set(this.permissionService.canDelete('Stakeholder'));

      const idParam = params.get('stakeholderId') ?? '';
      if (idParam === 'new-user') {
        this.isNew.set(true);
        this.isUserType.set(true);
        this.loadUsers();
        this.loadPermissions([]);
      } else if (idParam === 'new-nonuser') {
        this.isNew.set(true);
        this.isUserType.set(false);
      } else {
        this.isNew.set(false);
        this.stakeholderId = +idParam;
        this.loadStakeholder();
      }
    });
  }

  hasUnsavedChanges(): boolean {
    return this.hasChanges();
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
    if (this.stakeholderId) {
      void this.eventStreamService.removeSubscription('Stakeholder', this.stakeholderId);
    }
    this.sseSub?.unsubscribe();
  }

  private async loadUsers(): Promise<void> {
    try {
      const users = await this.userService.listUsers();
      this.userOptions.set(users.map(u => ({ label: u.name || u.username, value: u.username })));
    } catch {
      this.errorMessage.set('Failed to load users.');
    }
  }

  private async loadStakeholder(): Promise<void> {
    try {
      const s = await this.stakeholderService.getStakeholder(this.projectName, this.stakeholderId!);
      this.stakeholderName.set(s.name);
      this.version = s.version;
      this.isUserType.set(s.type === 'user');
      this.goals.set(s.goals ?? []);

      if (s.userDetails) {
        this.loadedUserDetails.set(s.userDetails);
        this.username = s.userDetails.username;
        this.teamName = s.userDetails.teamName ?? '';
        this.originalTeamName = this.teamName;
        await this.loadUsers();
        await this.loadPermissions(s.userDetails.permissionKeys);
        this.originalPermissionKeys = this.getSelectedPermissionKeys().sort().join(',');
      } else if (s.nonUserDetails) {
        this.text = s.nonUserDetails.text;
        this.originalName = s.name;
        this.originalText = this.text;
      }
      this.hasChanges.set(false);
    } catch {
      this.errorMessage.set('Failed to load stakeholder.');
    }
    if (this.stakeholderId && !this.sseSub) {
      void this.eventStreamService.addSubscription('Stakeholder', this.stakeholderId);
      this.sseSub = this.eventStreamService.events$.subscribe(envelope => {
        if (envelope.targetType === 'Stakeholder' && envelope.targetId === this.stakeholderId) {
          void this.loadStakeholder();
        }
      });
    }
  }

  trackChanges(): void {
    if (this.isUserType()) {
      const permKeys = this.getSelectedPermissionKeys().sort().join(',');
      this.hasChanges.set(
        this.teamName !== this.originalTeamName ||
        permKeys !== this.originalPermissionKeys
      );
    } else {
      this.hasChanges.set(
        this.stakeholderName() !== this.originalName ||
        this.text !== this.originalText
      );
    }
  }

  canEditGoals(): boolean {
    return this.permissionService.canEdit('Goal');
  }

  async onGoalSelected(goal: EntityReferenceDto): Promise<void> {
    this.showGoalSelector = false;
    try {
      const result = await this.commandService.execute('AddGoalToGoalContainer', {
        projectName: this.projectName,
        goalContainerId: this.stakeholderId,
        goalId: goal.id,
        containerType: 'Stakeholder'
      });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Goal added', detail: 'Goal added.' });
        this.goals.update(list => [...list, goal].sort((a, b) => a.name.localeCompare(b.name)));
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
        goalContainerId: this.stakeholderId,
        goalId: goal.id,
        containerType: 'Stakeholder'
      });
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: 'Goal removed', detail: 'Goal removed.' });
        this.goals.update(list => list.filter(g => g.id !== goal.id));
      } else {
        this.errorMessage.set(result.error ?? 'Failed to remove goal.');
      }
    } catch {
      this.errorMessage.set('Failed to remove goal.');
    }
  }

  onGoalClick(goal: EntityReferenceDto): void {
    this.router.navigate(['/projects', this.projectName, 'goals', goal.id]);
  }

  private async loadPermissions(selectedKeys: string[]): Promise<void> {
    try {
      const available = await this.stakeholderService.getAvailablePermissions();
      const selectedSet = new Set(selectedKeys);

      // Group by entity type
      const groupMap = new Map<string, { key: string; type: string; checked: boolean }[]>();
      for (const p of available) {
        let group = groupMap.get(p.entityType);
        if (!group) {
          group = [];
          groupMap.set(p.entityType, group);
        }
        group.push({ key: p.permissionKey, type: p.permissionType, checked: selectedSet.has(p.permissionKey) });
      }

      const groups: PermissionGroup[] = [...groupMap.entries()]
          .map(([entityType, permissions]) => ({ entityType, permissions }))
          .sort((a, b) => a.entityType.localeCompare(b.entityType));
      this.permissionGroups.set(groups);
    } catch {
      this.errorMessage.set('Failed to load permissions.');
    }
  }

  getPermission(group: PermissionGroup, type: string): { key: string; type: string; checked: boolean } | null {
    return group.permissions.find(p => p.type === type) ?? null;
  }

  getSelectedPermissionKeys(): string[] {
    return this.permissionGroups()
        .flatMap(g => g.permissions)
        .filter(p => p.checked)
        .map(p => p.key);
  }

  async onSave(): Promise<void> {
    this.saving.set(true);
    this.errorMessage.set(null);

    try {
      if (this.isUserType()) {
        const input: Record<string, unknown> = {
          projectName: this.projectName,
          username: this.username,
          teamName: this.teamName || null,
          permissionKeys: this.getSelectedPermissionKeys(),
        };
        if (this.version != null) input['version'] = this.version;
        const result = await this.commandService.execute('EditUserStakeholder', input);
        if (result.success) {
          this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Stakeholder saved.' });
          if (this.isNew()) {
            this.projectService.notifyTreeChanged();
            if (result.entity) {
              const saved = result.entity as StakeholderDto;
              this.hasChanges.set(false);
              this.router.navigate(['..', saved.id], { relativeTo: this.route });
            }
          } else {
            this.originalTeamName = this.teamName;
            this.originalPermissionKeys = this.getSelectedPermissionKeys().sort().join(',');
            this.hasChanges.set(false);
          }
        } else {
          this.errorMessage.set(result.error ?? 'Save failed.');
        }
      } else {
        const input: Record<string, unknown> = {
          projectName: this.projectName,
          name: this.stakeholderName(),
          text: this.text,
        };
        if (this.stakeholderId != null) input['stakeholderId'] = this.stakeholderId;
        if (this.version != null) input['version'] = this.version;
        const result = await this.commandService.execute('EditNonUserStakeholder', input);
        if (result.success) {
          this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Stakeholder saved.' });
          if (this.isNew()) {
            this.projectService.notifyTreeChanged();
            if (result.entity) {
              const saved = result.entity as StakeholderDto;
              this.hasChanges.set(false);
              this.router.navigate(['..', saved.id], { relativeTo: this.route });
            }
          } else {
            this.originalName = this.stakeholderName();
            this.originalText = this.text;
            this.hasChanges.set(false);
          }
        } else {
          this.errorMessage.set(result.error ?? 'Save failed.');
        }
      }
    } catch {
      this.errorMessage.set('An unexpected error occurred.');
    } finally {
      this.saving.set(false);
    }
  }

  onDelete(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this stakeholder?',
      accept: () => this.doDelete()
    });
  }

  private async doDelete(): Promise<void> {
    try {
      const result = await this.commandService.execute('DeleteStakeholder', {
        projectName: this.projectName,
        stakeholderId: this.stakeholderId,
        version: this.version
      });
      if (result.success) {
        this.projectService.notifyTreeChanged();
        this.router.navigate(['..'], { relativeTo: this.route });
      } else {
        this.errorMessage.set(result.error ?? 'Delete failed.');
      }
    } catch {
      this.errorMessage.set('An unexpected error occurred.');
    }
  }

  onBack(): void {
    this.router.navigate(['..'], { relativeTo: this.route });
  }
}
