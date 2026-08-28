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
import { Component, OnInit, computed, signal, ChangeDetectionStrategy, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { BadgeModule } from 'primeng/badge';
import { PageHeaderComponent } from '../../shared/page-header';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { ProjectService } from '../../core/project.service';
import { ProjectDto } from '../../models/project';
import { projectApiUrl } from '../../core/api-url';

interface CountCard {
  label: string;
  segment: string;
  count: number;
  icon: string;
}

interface NextAction {
  label: string;
  link: (string | number)[];
}

/**
 * Project workspace overview (#154, the former #128 IA scope). The landing page
 * for a project at `/projects/:name`: artifact counts (each a link into that
 * section), an open-issues summary, and derived next actions. The project
 * editor moved to `/projects/:name/edit`.
 *
 * "Recent changes" from the original #128 wish list is intentionally omitted —
 * there is no history/audit source to back it, and a faked feed would mislead.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-project-workspace',
  standalone: true,
  imports: [RouterLink, ButtonModule, BadgeModule, PageHeaderComponent, SubmitErrorComponent],
  template: `
    <div class="project-workspace" data-testid="project-workspace">
      <div class="ws-header">
        <app-page-header [title]="projectName" eyebrow="Project" />
        <a class="p-button p-button-outlined" [routerLink]="['/projects', projectName, 'edit']"
           data-testid="workspace-edit">
          <i class="pi pi-pencil" aria-hidden="true"></i> Edit project
        </a>
      </div>

      <app-submit-error [message]="errorMessage()" testid="workspace-error"
                        [retryable]="true" (retry)="load()" />

      @if (loading()) {
        <p class="ws-loading" data-testid="workspace-loading">Loading project…</p>
      } @else if (project()) {
        <section aria-label="Artifact counts" class="count-grid">
          @for (c of counts(); track c.segment) {
            <a class="count-card" [routerLink]="['/projects', projectName, c.segment]"
               [attr.data-testid]="'count-' + c.segment">
              <i class="count-icon {{ c.icon }}" aria-hidden="true"></i>
              <span class="count-value">{{ c.count }}</span>
              <span class="count-label">{{ c.label }}</span>
            </a>
          }
        </section>

        <div class="ws-columns">
          <section aria-label="Open issues" class="ws-panel" data-testid="workspace-open-issues">
            <div class="ws-panel-head">
              <h2 class="ws-panel-title">Open issues</h2>
              @if (mustResolveCount() > 0) {
                <p-badge [value]="mustResolveCount().toString()" severity="danger"
                         data-testid="workspace-must-resolve" />
              }
            </div>
            @if (openIssueCount() === 0) {
              <p class="ws-empty">No open issues.</p>
            } @else {
              <p class="ws-count-line">
                {{ openIssueCount() }} open{{ mustResolveCount() > 0 ? ', ' + mustResolveCount() + ' must be resolved' : '' }}.
              </p>
            }
            <a class="ws-panel-link" [routerLink]="['/projects', projectName, 'open-issues']">
              View open issues →
            </a>
          </section>

          <section aria-label="Next actions" class="ws-panel" data-testid="workspace-next-actions">
            <h2 class="ws-panel-title">Next actions</h2>
            @if (nextActions().length === 0) {
              <p class="ws-empty">You're all set — nothing needs attention.</p>
            } @else {
              <ul class="ws-actions">
                @for (a of nextActions(); track a.label) {
                  <li><a [routerLink]="a.link">{{ a.label }}</a></li>
                }
              </ul>
            }
          </section>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .ws-header {
      display: flex; align-items: flex-start; justify-content: space-between;
      gap: var(--rq-space-4); margin-bottom: var(--rq-space-6);
    }
    .count-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(9rem, 1fr));
      gap: var(--rq-space-4); margin-bottom: var(--rq-space-6);
    }
    .count-card {
      display: flex; flex-direction: column; align-items: flex-start; gap: var(--rq-space-1);
      padding: var(--rq-card-pad);
      background: var(--rq-card-bg); border: 1px solid var(--rq-card-border);
      border-radius: var(--rq-card-radius); box-shadow: var(--rq-card-shadow);
      text-decoration: none; color: var(--p-text-color); transition: border-color 0.12s;
    }
    .count-card:hover { border-color: var(--p-primary-color); }
    .count-icon { color: var(--p-primary-color); font-size: 1.1rem; }
    .count-value { font-size: var(--rq-font-size-xl); font-weight: var(--rq-font-weight-bold); }
    .count-label { color: var(--p-text-secondary-color); font-size: var(--rq-font-size-sm); }
    .ws-columns {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
      gap: var(--rq-space-4);
    }
    .ws-panel {
      padding: var(--rq-card-pad); background: var(--rq-card-bg);
      border: 1px solid var(--rq-card-border); border-radius: var(--rq-card-radius);
      box-shadow: var(--rq-card-shadow);
    }
    .ws-panel-head { display: flex; align-items: center; gap: var(--rq-space-2); }
    .ws-panel-title { margin: 0 0 var(--rq-space-2); font-size: var(--rq-font-size-lg); }
    .ws-empty, .ws-count-line { color: var(--p-text-secondary-color); margin: 0 0 var(--rq-space-2); }
    .ws-actions { margin: 0; padding-left: var(--rq-space-4); display: flex; flex-direction: column; gap: var(--rq-space-1); }
    .ws-panel-link { text-decoration: none; color: var(--p-primary-color); font-size: var(--rq-font-size-sm); }
    .ws-loading { color: var(--p-text-secondary-color); }
  `]
})
export class ProjectWorkspaceComponent implements OnInit {
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly project = signal<ProjectDto | null>(null);
  readonly openIssueCount = signal(0);
  readonly mustResolveCount = signal(0);
  projectName = '';

  private readonly destroyRef = inject(DestroyRef);

  readonly counts = computed<CountCard[]>(() => {
    const p = this.project();
    if (!p) return [];
    return [
      { label: 'Stakeholders', segment: 'stakeholders', count: p.stakeholderCount, icon: 'pi pi-users' },
      { label: 'Goals', segment: 'goals', count: p.goalCount, icon: 'pi pi-flag' },
      { label: 'Stories', segment: 'stories', count: p.storyCount, icon: 'pi pi-book' },
      { label: 'Actors', segment: 'actors', count: p.actorCount, icon: 'pi pi-user' },
      { label: 'Scenarios', segment: 'scenarios', count: p.scenarioCount, icon: 'pi pi-list-check' },
      { label: 'Use cases', segment: 'use-cases', count: p.useCaseCount, icon: 'pi pi-sitemap' },
      { label: 'Glossary', segment: 'terms', count: p.glossaryTermCount, icon: 'pi pi-list' },
      { label: 'Reports', segment: 'reports', count: p.reportGeneratorCount, icon: 'pi pi-file' },
    ];
  });

  readonly nextActions = computed<NextAction[]>(() => {
    const p = this.project();
    if (!p) return [];
    const actions: NextAction[] = [];
    if (this.mustResolveCount() > 0) {
      actions.push({
        label: `Resolve ${this.mustResolveCount()} blocking issue${this.mustResolveCount() === 1 ? '' : 's'}`,
        link: ['/projects', this.projectName, 'open-issues'],
      });
    }
    if (p.goalCount === 0) {
      actions.push({ label: 'Add your first goal', link: ['/projects', this.projectName, 'goals'] });
    }
    if (p.stakeholderCount === 0) {
      actions.push({ label: 'Add a stakeholder', link: ['/projects', this.projectName, 'stakeholders'] });
    }
    return actions;
  });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly projectService: ProjectService,
    private readonly http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.projectName = params.get('name') ?? '';
      void this.load();
    });
  }

  async load(): Promise<void> {
    if (!this.projectName) return;
    this.loading.set(true);
    this.errorMessage.set(null);
    try {
      const [project, issues] = await Promise.all([
        this.projectService.getProject(this.projectName),
        this.loadOpenIssues(),
      ]);
      this.project.set(project);
      this.openIssueCount.set(issues.length);
      this.mustResolveCount.set(issues.filter(i => i.mustBeResolved).length);
    } catch {
      this.errorMessage.set('Failed to load the project workspace.');
    } finally {
      this.loading.set(false);
    }
  }

  private async loadOpenIssues(): Promise<{ mustBeResolved: boolean }[]> {
    try {
      return await firstValueFrom(
        this.http.get<{ mustBeResolved: boolean }[]>(projectApiUrl(this.projectName, 'open-issues')),
      );
    } catch {
      // Open issues are a secondary panel; a failure there shouldn't blank the
      // whole workspace. Treat as none and let the counts still render.
      return [];
    }
  }
}
