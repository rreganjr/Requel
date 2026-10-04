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
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject,
  signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { AssistantDefinitionsService } from '../../core/assistant-definitions.service';
import { PermissionService } from '../../core/permission.service';
import { ListPageComponent } from '../../shared/list-page';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import {
  DEFINITION_RISK, DefinitionKind, ProjectDefinitionDto, kindLabel,
} from '../../models/assistant-definition';


/**
 * A project's assistant definitions (issue #264), at /projects/:name/definitions: every one in
 * effect, grouped as on the Assistants panel, with where it came from and what it covers.
 * Customizing a bundled one copies it into the project; reverting drops the copy; the project's
 * own can be deleted. Reading and writing both need AssistantDefinition[Edit].
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-project-definitions',
  standalone: true,
  imports: [ListPageComponent, SubmitErrorComponent, ButtonModule, RouterLink],
  template: `
    <app-list-page title="AI definitions" [showSearch]="false">
      @if (canManage()) {
        <div actions>
          <p-button label="New definition" icon="pi pi-plus" size="small"
                    (onClick)="create()" data-testid="definitions-new" />
        </div>
      }
      @if (!canManage()) {
        <p class="ws-empty" data-testid="definitions-forbidden">
          You don't have permission to manage AI definitions in this project.
        </p>
      } @else {
        <p class="intro" data-testid="definitions-risk">{{ risk }}</p>
        <app-submit-error [message]="errorMessage()" testid="definitions-error"
                          [retryable]="loadFailed()" (retry)="load()" />
        @if (!loading() && definitions().length === 0 && !loadFailed()) {
          <p class="ws-empty">No AI definitions. AI review may be off on this server.</p>
        }
        @for (g of groups(); track g.kind) {
          <h2 class="section-title" [attr.data-testid]="'definitions-group-' + g.kind">{{ g.label }}</h2>
          <ul class="definition-list">
            @for (d of g.definitions; track d.key) {
              <li class="definition-row" [attr.data-testid]="'definition-' + d.key">
                <div class="definition-main">
                  <a [routerLink]="['/projects', projectName, 'definitions', d.key]"
                     [attr.data-testid]="'definition-link-' + d.key">{{ d.displayName }}</a>
                  <span class="source" [attr.data-testid]="'definition-source-' + d.key">
                    {{ sourceLabel(d) }} · v{{ d.version }}{{ d.enabled === false ? ' · off' : '' }}
                  </span>
                  @if (newerBundled(d)) {
                    <span class="newer" [attr.data-testid]="'definition-newer-' + d.key">
                      Bundled v{{ d.bundledVersion }} is newer
                    </span>
                  }
                  <div class="covers">
                    {{ d.inEffectFor.length > 0 ? 'In effect for ' + d.inEffectFor.join(', ')
                       : 'Not in effect: other definitions cover its types' }}
                  </div>
                </div>
                <div class="definition-actions">
                  @if (d.source === 'BUNDLED') {
                    <p-button label="Customize" size="small" [outlined]="true"
                              [loading]="busy() === d.key" (onClick)="fork(d)"
                              [attr.data-testid]="'definition-fork-' + d.key" />
                  } @else if (d.bundledVersion != null) {
                    <p-button label="Revert" size="small" [outlined]="true" severity="secondary"
                              [loading]="busy() === d.key" (onClick)="revert(d)"
                              [attr.data-testid]="'definition-revert-' + d.key" />
                  } @else {
                    <p-button label="Delete" size="small" [outlined]="true" severity="danger"
                              [loading]="busy() === d.key" (onClick)="remove(d)"
                              [attr.data-testid]="'definition-delete-' + d.key" />
                  }
                </div>
              </li>
            }
          </ul>
        }
      }
    </app-list-page>
  `,
  styles: [`
    .intro { margin: 0 0 1rem; color: var(--p-text-muted-color); }
    .ws-empty { color: var(--p-text-secondary-color); }
    .section-title { margin: 1.5rem 0 0.5rem; font-size: 1.1rem; }
    .definition-list { list-style: none; margin: 0; padding: 0; display: flex;
      flex-direction: column; gap: 0.75rem; }
    .definition-row { display: flex; justify-content: space-between; gap: 1rem;
      align-items: flex-start; }
    .definition-main { display: flex; flex-wrap: wrap; gap: 0.25rem 0.75rem; align-items: baseline; }
    .source, .covers { color: var(--p-text-muted-color); font-size: 0.875rem; }
    .covers { flex-basis: 100%; }
    .newer { color: var(--p-orange-600, #c2410c); font-size: 0.875rem; }
  `]
})
export class ProjectDefinitionsComponent implements OnInit {
  readonly definitions = signal<ProjectDefinitionDto[]>([]);
  readonly loading = signal(true);
  readonly loadFailed = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly busy = signal<string | null>(null);
  readonly canManage = computed(() => this.permissionService.canEdit('AssistantDefinition'));
  readonly risk = DEFINITION_RISK;

  /** By kind, in the Assistants panel's order. */
  readonly groups = computed(() => {
    const order: DefinitionKind[] = ['REVIEW', 'POLICY', 'CORPUS'];
    return order
      .map(kind => ({ kind, label: kindLabel(kind),
        definitions: this.definitions().filter(d => d.kind === kind) }))
      .filter(g => g.definitions.length > 0);
  });

  protected projectName = '';
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(AssistantDefinitionsService);
  private readonly permissionService = inject(PermissionService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        if (this.canManage()) {
          await this.load();
        } else {
          this.loading.set(false);
        }
      }
    });
  }

  async load(): Promise<void> {
    this.loading.set(true);
    try {
      this.definitions.set(await this.service.list(this.projectName));
      if (this.loadFailed()) {
        this.loadFailed.set(false);
        this.errorMessage.set(null);
      }
    } catch {
      this.loadFailed.set(true);
      this.errorMessage.set('Failed to load the AI definitions.');
    } finally {
      this.loading.set(false);
    }
  }

  sourceLabel(d: ProjectDefinitionDto): string {
    if (d.source === 'BUNDLED') return 'Bundled';
    return d.bundledVersion != null ? 'Customized' : 'This project\'s';
  }

  newerBundled(d: ProjectDefinitionDto): boolean {
    return d.source === 'PROJECT' && d.bundledVersion != null && d.forkedFromVersion != null
      && d.bundledVersion > d.forkedFromVersion;
  }

  create(): void {
    this.router.navigate(['/projects', this.projectName, 'definitions', 'new']);
  }

  /** Copy a bundled definition into the project and open the copy. */
  async fork(d: ProjectDefinitionDto): Promise<void> {
    await this.run(d, async () => {
      const result = await this.service.fork(this.projectName, d.key);
      if (result.success) {
        this.router.navigate(['/projects', this.projectName, 'definitions', d.key]);
      }
      return result;
    }, `Customizing "${d.displayName}"`);
  }

  async revert(d: ProjectDefinitionDto): Promise<void> {
    await this.run(d, () => this.service.revert(this.projectName, d.key, d.lockVersion),
      `Reverted "${d.displayName}" to the bundled definition`);
  }

  async remove(d: ProjectDefinitionDto): Promise<void> {
    await this.run(d, () => this.service.delete(this.projectName, d.key, d.lockVersion),
      `Deleted "${d.displayName}"`);
  }

  private async run(d: ProjectDefinitionDto,
                    write: () => Promise<{ success: boolean; error: string | null; status?: number }>,
                    done: string): Promise<void> {
    this.busy.set(d.key);
    this.errorMessage.set(null);
    try {
      const result = await write();
      if (result.success) {
        this.messageService.add({ severity: 'success', summary: done, life: 3000 });
      } else {
        this.errorMessage.set(result.status === 409
          ? 'Someone else changed this definition. The list has been reloaded.'
          : result.error ?? 'The change failed.');
      }
    } finally {
      this.busy.set(null);
    }
    await this.load();
  }
}
