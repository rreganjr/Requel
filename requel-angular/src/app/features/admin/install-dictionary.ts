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
import { ChangeDetectionStrategy, Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { DictionaryWordDto } from '../../models/dictionary';
import { DictionaryService } from '../../core/dictionary.service';
import { PageHeaderComponent } from '../../shared/page-header';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { DictionaryWordListComponent } from '../../shared/dictionary-word-list';

/**
 * The installation-wide spell-check word list (issue #319), at /dictionary behind the
 * adminGuard. A word added here is known in every project at once; the commands require the
 * system administrator role server-side as well. The WordNet corpus and the standard word lists
 * are not shown and cannot be edited.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-install-dictionary',
  standalone: true,
  imports: [PageHeaderComponent, SubmitErrorComponent, DictionaryWordListComponent],
  template: `
    <div class="install-dictionary" data-testid="install-dictionary">
      <div class="page-header"><app-page-header title="Installation Dictionary" /></div>
      <p class="intro">
        Words the spell checker accepts in every project, in addition to the standard
        dictionaries. Project-specific words belong on that project's Dictionary page.
      </p>
      <app-submit-error [message]="errorMessage()" testid="install-dictionary-error"
                        [retryable]="loadFailed()" (retry)="load()" />
      <app-dictionary-word-list #list [words]="words()" [loading]="loading()"
                                [canAdd]="true" [canRemove]="true" [adding]="adding()"
                                testid="install-dictionary"
                                emptyMessage="No installation-wide words have been added."
                                (add)="addWord($event)" (remove)="removeWord($event)" />
    </div>
  `,
  styles: [`
    .install-dictionary { max-width: 800px; }
    .page-header { margin-bottom: 1rem; }
    .intro { margin: 0 0 1rem; color: var(--p-text-muted-color); }
  `]
})
export class InstallDictionaryComponent implements OnInit {
  readonly words = signal<DictionaryWordDto[]>([]);
  readonly loading = signal(true);
  readonly adding = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly loadFailed = signal(false);

  @ViewChild('list') list?: DictionaryWordListComponent;

  private readonly dictionaryService = inject(DictionaryService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    try {
      this.words.set(await this.dictionaryService.listInstallWords());
      // Clear only a load failure: a failed add or remove reloads too, and its message must stay.
      if (this.loadFailed()) {
        this.loadFailed.set(false);
        this.errorMessage.set(null);
      }
    } catch {
      this.loadFailed.set(true);
      this.errorMessage.set('Failed to load the installation dictionary.');
    } finally {
      this.loading.set(false);
    }
  }

  async addWord(lemma: string): Promise<void> {
    this.adding.set(true);
    this.errorMessage.set(null);
    try {
      const result = await this.dictionaryService.addInstallWord(lemma);
      if (result.success) {
        this.list?.clearAdd();
        this.messageService.add({ severity: 'success', summary: `Added "${lemma}"`, life: 3000 });
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
    const result = await this.dictionaryService.removeInstallWord(word.id);
    if (result.success) {
      this.messageService.add({ severity: 'success', summary: `Removed "${word.lemma}"`, life: 3000 });
    } else {
      this.errorMessage.set(result.error ?? 'Failed to remove the word.');
    }
    await this.load();
  }
}
