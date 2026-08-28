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
import { Component, EventEmitter, Input, Output, ViewChild, ElementRef, ChangeDetectionStrategy } from '@angular/core';
import { ButtonModule } from 'primeng/button';

type ButtonSeverity =
  | 'success' | 'info' | 'warn' | 'danger' | 'help' | 'primary' | 'secondary' | 'contrast';
type ButtonSize = 'small' | 'large';

/**
 * Shared file-picker button. Owns the hidden <input type="file">, the trigger
 * button, its accessible name, and the input reset, so feature templates no
 * longer duplicate ad-hoc hidden inputs with inline `display:none` styles
 * (issue #126). The button keeps focus; picking a file emits the selected
 * `File` via `fileSelected` and clears the input so the same file can be
 * chosen again.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-file-upload-button',
  standalone: true,
  imports: [ButtonModule],
  template: `
    <p-button [label]="label"
              [icon]="icon"
              [severity]="severity"
              [outlined]="outlined"
              [text]="text"
              [size]="size"
              [loading]="loading"
              [ariaLabel]="ariaLabel || label"
              [attr.data-testid]="buttonTestid"
              (onClick)="fileInput.click()" />
    <input #fileInput
           type="file"
           class="file-input-hidden"
           [accept]="accept"
           [attr.data-testid]="inputTestid"
           (change)="onChange()" />
  `,
  styles: [`
    .file-input-hidden { display: none; }
  `]
})
export class FileUploadButtonComponent {
  @Input() label = 'Upload';
  @Input() icon = 'pi pi-upload';
  @Input() accept = '';
  @Input() severity: ButtonSeverity = 'secondary';
  @Input() outlined = false;
  @Input() text = false;
  @Input() size: ButtonSize | undefined;
  @Input() loading = false;
  @Input() ariaLabel = '';
  @Input() buttonTestid: string | null = null;
  @Input() inputTestid: string | null = null;

  /** Emits the picked file. The input is cleared afterwards so re-picking the same file still fires. */
  @Output() fileSelected = new EventEmitter<File>();

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  onChange(): void {
    const input = this.fileInput.nativeElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      this.fileSelected.emit(file);
    }
  }
}
