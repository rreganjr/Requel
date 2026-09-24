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
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, ViewChild, computed, inject,
  signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { MessageService } from 'primeng/api';
import { DictionaryWordDto } from '../../models/dictionary';
import { DictionaryService } from '../../core/dictionary.service';
import { PermissionService } from '../../core/permission.service';
import { ProjectService } from '../../core/project.service';
import { ListPageComponent } from '../../shared/list-page';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { DictionaryWordListComponent } from '../../shared/dictionary-word-list';

/**
 * A project's own spell-check word list (issue #319), at /projects/:name/dictionary.
 * <p>
 * Every stakeholder can see it; adding and removing need Project[Edit], which the server
 * enforces too. Words added by resolving an "Add to Dictionary" issue land here as well.
 * Removing a word does not re-check text already analyzed.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-project-dictionary',
  standalone: true,
  imports: [ListPageComponent, SubmitErrorComponent, DictionaryWordListComponent],
  template: `
    <app-list-page title="Dictionary" [showSearch]="false">
      <p class="intro" data-testid="project-dictionary-intro">
        Words the spell checker accepts in this project only, in addition to the standard
        dictionaries.
      </p>
      <app-submit-error [message]="errorMessage()" testid="project-dictionary-error"
                        [retryable]="loadFailed()" (retry)="load()" />
      <app-dictionary-word-list #list [words]="words()" [loading]="loading()"
                                [canAdd]="canEdit()" [canRemove]="canEdit()" [adding]="adding()"
                                testid="project-dictionary"
                                emptyMessage="Words added here, or by resolving an 'Add to Dictionary' issue, appear in this list."
                                (add)="addWord($event)" (remove)="removeWord($event)" />
    </app-list-page>
  `,
  styles: [`
    .intro { margin: 0 0 1rem; color: var(--p-text-muted-color); }
  `]
})
export class ProjectDictionaryComponent implements OnInit {
  readonly words = signal<DictionaryWordDto[]>([]);
  readonly loading = signal(true);
  readonly adding = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly loadFailed = signal(false);
  readonly canEdit = computed(() => this.permissionService.canEdit('Project'));

  @ViewChild('list') list?: DictionaryWordListComponent;

  private projectName = '';
  private readonly route = inject(ActivatedRoute);
  private readonly dictionaryService = inject(DictionaryService);
  private readonly permissionService = inject(PermissionService);
  private readonly projectService = inject(ProjectService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(async params => {
      const name = params.get('name') ?? '';
      if (name !== this.projectName) {
        this.projectName = name;
        await this.permissionService.loadForProject(name);
        await this.load();
      }
    });
  }

  async load(): Promise<void> {
    this.loading.set(true);
    try {
      this.words.set(await this.dictionaryService.listProjectWords(this.projectName));
      // Clear only a load failure: a failed add or remove reloads too, and its message must stay.
      if (this.loadFailed()) {
        this.loadFailed.set(false);
        this.errorMessage.set(null);
      }
    } catch {
      this.loadFailed.set(true);
      this.errorMessage.set('Failed to load the project dictionary.');
    } finally {
      this.loading.set(false);
    }
  }

  async addWord(lemma: string): Promise<void> {
    this.adding.set(true);
    this.errorMessage.set(null);
    try {
      const result = await this.dictionaryService.addProjectWord(this.projectName, lemma);
      if (result.success) {
        this.list?.clearAdd();
        this.messageService.add({ severity: 'success', summary: `Added "${lemma}"`, life: 3000 });
        this.projectService.notifyTreeChanged();
        await this.load();
      } else {
        const unresolved = this.list?.showAddErrors(result.violations) ?? [];
        const message = unresolved.join(' ') || (result.violations?.length ? null : result.error);
        this.errorMessage.set(message ?? null);
      }
    } finally {
      this.adding.set(false);
    }
  }

  async removeWord(word: DictionaryWordDto): Promise<void> {
    this.errorMessage.set(null);
    const result = await this.dictionaryService.removeProjectWord(this.projectName, word.id);
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: `Removed "${word.lemma}"`, life: 3000 });
      this.projectService.notifyTreeChanged();
    } else {
      this.errorMessage.set(result.error ?? 'Failed to remove the word.');
    }
    await this.load();
  }
}
