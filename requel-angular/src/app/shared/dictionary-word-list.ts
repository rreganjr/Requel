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
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output, TemplateRef,
  ViewChild, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DictionaryWordDto } from '../models/dictionary';
import { FieldViolation } from '../models/command';
import { AppDataTableComponent, DataTableColumn } from './app-data-table';
import { InlineErrorComponent } from './app-inline-error';
import { applyCommandErrors, clearServerErrors, notBlank } from './form-errors';

/** The width of the server's lemma columns; the command refuses anything longer. */
export const MAX_DICTIONARY_WORD_LENGTH = 80;

/** One token: the spell checker only ever asks about single words (issue #319). */
export function singleWord(): ValidatorFn {
  return control => {
    const v = typeof control.value === 'string' ? control.value.trim() : '';
    return /\s/.test(v) ? { singleWord: true } : null;
  };
}

/**
 * A spell-check word list with an add row and per-word remove (issue #319), shared by a
 * project's Dictionary page and the admin Installation Dictionary page.
 * <p>
 * Presentational: it validates and emits, and the page runs the command. On success the page
 * calls {@link clearAdd}; on failure {@link showAddErrors}, which routes a `lemma` violation to
 * the field and returns anything else for the page-level error. Remove asks first.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dictionary-word-list',
  standalone: true,
  imports: [ReactiveFormsModule, AppDataTableComponent, ButtonModule, InputText,
            InlineErrorComponent, ConfirmDialogModule],
  providers: [ConfirmationService],
  template: `
    @if (canAdd) {
      <fieldset class="rq-fieldset" [attr.data-testid]="testid + '-add-form'" [formGroup]="addForm">
        <legend>Add word</legend>
        <div class="add-row">
          <input pInputText formControlName="lemma" placeholder="word"
                 aria-label="Word to add" [attr.data-testid]="testid + '-word'" class="word-input"
                 [attr.aria-invalid]="lemmaErr.message() ? 'true' : null"
                 [attr.aria-describedby]="lemmaErr.message() ? errorId : null"
                 (keyup.enter)="submit()" />
          <p-button label="Add Word" icon="pi pi-plus" [attr.data-testid]="testid + '-add'"
                    [loading]="adding" (onClick)="submit()" />
          <app-inline-error #lemmaErr [control]="addForm.controls.lemma" [id]="errorId"
                            [submitted]="submitted()" [overrides]="messages"
                            [testid]="testid + '-word-error'" />
        </div>
      </fieldset>
    }

    <app-data-table [value]="words" [columns]="columns" [loading]="loading"
                    [showToolbar]="false" [rowClickable]="false" [defaultActions]="false"
                    [testid]="testid" [rowTestid]="testid + '-row'"
                    emptyTitle="No words yet" [emptyMessage]="emptyMessage" emptyIcon="pi-language">
      @if (canRemove) {
        <ng-template #rowActions let-w>
          <p-button icon="pi pi-trash" severity="danger" [text]="true" size="small"
                    [attr.data-testid]="testid + '-remove'" [ariaLabel]="'Remove ' + w.lemma"
                    (onClick)="confirmRemove(w)" />
        </ng-template>
      }
    </app-data-table>

    <ng-template #lemmaCell let-w><span [attr.data-testid]="testid + '-lemma'">{{ w.lemma }}</span></ng-template>
    <p-confirmDialog />
  `,
  styles: [`
    .add-row { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem; flex-wrap: wrap; }
    .word-input { max-width: 260px; }
    .add-row .rq-field-error { flex-basis: 100%; margin: 0; }
  `]
})
export class DictionaryWordListComponent implements OnInit {
  @Input() words: DictionaryWordDto[] = [];
  @Input() loading = false;
  @Input() canAdd = false;
  @Input() canRemove = false;
  /** data-testid stem for every element this renders. */
  @Input() testid = 'dictionary';
  @Input() emptyMessage = '';
  /** True while the page's add command is in flight. */
  @Input() adding = false;

  /** The trimmed word, once it passes the client-side rule. */
  @Output() add = new EventEmitter<string>();
  /** The word to remove, once the user confirmed. */
  @Output() remove = new EventEmitter<DictionaryWordDto>();

  readonly addForm = new FormGroup({
    lemma: new FormControl('', { nonNullable: true, validators: [
      notBlank(), singleWord(), Validators.maxLength(MAX_DICTIONARY_WORD_LENGTH)] }),
  });
  protected readonly submitted = signal(false);
  protected readonly messages = {
    required: 'Enter a word.',
    singleWord: 'One word only, with no spaces.',
    maxlength: `At most ${MAX_DICTIONARY_WORD_LENGTH} characters.`,
  };

  @ViewChild('lemmaCell', { static: true }) lemmaCell!: TemplateRef<{ $implicit: DictionaryWordDto }>;
  columns: DataTableColumn<DictionaryWordDto>[] = [];

  private readonly confirmationService = inject(ConfirmationService);

  get errorId(): string {
    return this.testid + '-word-error';
  }

  ngOnInit(): void {
    this.columns = [{ field: 'lemma', header: 'Word', sortable: true, cellTemplate: this.lemmaCell }];
  }

  submit(): void {
    this.submitted.set(true);
    clearServerErrors(this.addForm);
    const control = this.addForm.controls.lemma;
    // maxLength counts the raw value; the rule is about the trimmed word.
    control.setValue(control.value.trim());
    if (this.addForm.invalid) {
      control.markAsTouched();
      return;
    }
    this.add.emit(control.value);
  }

  /** The page's add succeeded: empty the field for the next word. */
  clearAdd(): void {
    this.addForm.reset({ lemma: '' });
    this.submitted.set(false);
  }

  /** The page's add failed: show a `lemma` violation on the field, return the rest. */
  showAddErrors(violations: FieldViolation[] | null | undefined): string[] {
    return applyCommandErrors(this.addForm, violations);
  }

  confirmRemove(word: DictionaryWordDto): void {
    this.confirmationService.confirm({
      message: `Remove "${word.lemma}"? Text that uses it will be flagged again the next time it is analyzed.`,
      header: 'Remove Word',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Remove',
      rejectLabel: 'Cancel',
      accept: () => this.remove.emit(word),
    });
  }
}
