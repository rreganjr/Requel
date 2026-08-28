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
import { DatePipe } from '@angular/common';
import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormGroup, FormControl, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SubmitErrorComponent } from '../../shared/app-submit-error';
import { InlineErrorComponent } from '../../shared/app-inline-error';
import { notBlank } from '../../shared/form-errors';
import { SelectModule } from 'primeng/select';
import { TokenService } from '../../core/token.service';
import { ApiTokenDto } from '../../models/api-token';

/**
 * Personal access token management (#73, Slice 6): list, create (showing the one-time plaintext),
 * and revoke the current user's tokens. Hosted as a section of the Settings page, rendered only for
 * users with the ProjectUserRole (gated by the host).
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-api-tokens',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, ButtonModule, DialogModule, InputTextModule, MessageModule, SubmitErrorComponent,
    SelectModule, InlineErrorComponent],
  template: `
    <div class="api-tokens" data-testid="api-tokens">
      <div class="section-header">
        <h3>Personal Access Tokens</h3>
        <p-button label="New token" icon="pi pi-plus" data-testid="pat-new"
                  (onClick)="openCreate()" />
      </div>
      <p class="hint">
        Use a token as a bearer credential for MCP/API clients (e.g. mcp-remote). It acts as you and
        can be revoked at any time.
      </p>

      <div class="message-slot">
        <app-submit-error [message]="errorMessage()" testid="api-tokens-error" />
        <div role="status" aria-live="polite">@if (successMessage()) { <p-message severity="success" [text]="successMessage()" /> }</div>
      </div>

      @if (loading()) {
        <p>Loading…</p>
      } @else if (tokens().length === 0) {
        <p class="empty">No tokens yet.</p>
      } @else {
        <table class="pat-table" data-testid="pat-table">
          <thead>
            <tr><th>Name</th><th>Status</th><th>Created</th><th>Last used</th><th>Expires</th><th></th></tr>
          </thead>
          <tbody>
            @for (t of tokens(); track t.id) {
              <tr>
                <td>{{ t.name }}</td>
                <td><span class="status status-{{ t.status.toLowerCase() }}">{{ t.status }}</span></td>
                <td>{{ t.createdAt | date:'short' }}</td>
                <td>{{ t.lastUsedAt ? (t.lastUsedAt | date:'short') : '—' }}</td>
                <td>{{ t.expiresAt ? (t.expiresAt | date:'short') : 'Never' }}</td>
                <td>
                  @if (t.status === 'ACTIVE') {
                    <p-button label="Revoke" severity="danger" [text]="true"
                              (onClick)="revoke(t)" />
                  } @else {
                    <p-button label="Delete" icon="pi pi-trash" severity="danger" [text]="true"
                              data-testid="pat-delete" (onClick)="deleteToken(t)" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      }

      <p-dialog header="New personal access token" [(visible)]="createDialogVisible"
                [modal]="true" [focusOnShow]="true" closeAriaLabel="Close"
                [style]="{ width: '32rem' }" (onHide)="onDialogHide()">
        @if (createdToken() === null) {
          <div [formGroup]="createForm">
            <app-submit-error [message]="createError()" testid="pat-create-error" />
            <div class="field">
              <label for="patName">Name</label>
              <input id="patName" pInputText formControlName="name" data-testid="pat-name"
                     placeholder="e.g. claude-desktop"
                     [attr.aria-invalid]="nameErr.message() ? 'true' : null"
                     [attr.aria-describedby]="nameErr.message() ? 'pat-name-error' : null" />
              <app-inline-error #nameErr [control]="createForm.controls.name" id="pat-name-error"
                                [submitted]="submitted()" [overrides]="{ required: 'Name is required.' }"
                                testid="pat-name-error" />
            </div>
            <div class="field">
              <label for="patExpiry">Expires in</label>
              <p-select inputId="patExpiry" [options]="expiryOptions" optionLabel="label"
                        optionValue="value" formControlName="expiresInDays" data-testid="pat-expiry" />
            </div>
            <div class="dialog-actions">
              <p-button label="Create" icon="pi pi-check" data-testid="pat-create"
                        [loading]="creating()" (onClick)="submitCreate()" />
            </div>
          </div>
        } @else {
          <p-message severity="warn"
                     text="Copy this token now — it will not be shown again." />
          <div class="token-display">
            <code data-testid="pat-plaintext">{{ createdToken() }}</code>
            <p-button [label]="copied() ? 'Copied' : 'Copy'"
                      [icon]="copied() ? 'pi pi-check' : 'pi pi-copy'"
                      [text]="true" (onClick)="copy()" />
          </div>
          <div class="dialog-actions">
            <p-button label="Done" data-testid="pat-done" (onClick)="closeCreated()" />
          </div>
        }
      </p-dialog>
    </div>
  `,
  styles: [`
    .api-tokens { margin-top: 2rem; }
    .section-header { display: flex; align-items: center; justify-content: space-between; }
    .section-header h3 { margin: 0; }
    .hint { color: var(--p-text-muted-color); margin: 0.25rem 0 1rem; }
    /* Reserve fixed space (a single message line ≈ 2.13rem) so showing/clearing a
       status message never shifts the table below it. */
    .message-slot { min-height: 2.25rem; margin-bottom: 0.5rem; display: flex; align-items: center; }
    .message-slot p-message { width: 100%; }
    .pat-table { width: 100%; border-collapse: collapse; }
    .pat-table th, .pat-table td { text-align: left; padding: 0.5rem; border-bottom: 1px solid var(--p-content-border-color); }
    .status { font-size: 0.8rem; font-weight: 600; }
    .status-active { color: var(--p-green-600, #16a34a); }
    .status-expired { color: var(--p-text-muted-color); }
    .status-revoked { color: var(--p-red-600, #dc2626); }
    .field { display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 1rem; }
    .field label { font-weight: 600; }
    .token-display { display: flex; align-items: center; gap: 0.5rem; margin: 0.75rem 0; }
    .token-display code { flex: 1; padding: 0.5rem; background: var(--p-content-hover-background, #f1f5f9); border-radius: 4px; word-break: break-all; }
    .dialog-actions { display: flex; justify-content: flex-end; margin-top: 0.5rem; }
    .empty { color: var(--p-text-muted-color); }
  `]
})
export class ApiTokensComponent implements OnInit {

  readonly tokens = signal<ApiTokenDto[]>([]);
  readonly loading = signal(false);
  readonly creating = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  /** The one-time plaintext after a successful create; null while filling the form. */
  readonly createdToken = signal<string | null>(null);
  /** True briefly after a successful copy, to confirm to the user. */
  readonly copied = signal(false);

  /** Fixed expiry choices (days); no "never" option for tokens minted from the UI. */
  readonly expiryOptions = [
    { label: '30 days', value: 30 },
    { label: '90 days', value: 90 },
    { label: '180 days', value: 180 },
    { label: '365 days', value: 365 }
  ];

  createDialogVisible = false;
  /** Create-failure message, shown by app-submit-error INSIDE the dialog (not the page-level slot). */
  readonly createError = signal('');
  readonly submitted = signal(false);
  readonly createForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [notBlank()] }),
    expiresInDays: new FormControl(90, { nonNullable: true }),
  });

  constructor(private tokenService: TokenService) {}

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.errorMessage.set('');
    try {
      this.tokens.set(await this.tokenService.list());
    } catch {
      this.errorMessage.set('Failed to load tokens.');
    } finally {
      this.loading.set(false);
    }
  }

  openCreate(): void {
    this.createForm.reset({ name: '', expiresInDays: 90 });
    this.submitted.set(false);
    this.createError.set('');
    this.createdToken.set(null);
    this.copied.set(false);
    this.successMessage.set('');
    this.createDialogVisible = true;
  }

  async submitCreate(): Promise<void> {
    this.submitted.set(true);
    if (this.createForm.invalid) {
      this.createForm.controls.name.markAsTouched();
      return;
    }
    this.creating.set(true);
    this.createError.set('');
    try {
      const raw = this.createForm.getRawValue();
      const response = await this.tokenService.create({
        name: raw.name.trim(),
        expiresInDays: raw.expiresInDays
      });
      this.createdToken.set(response.token);
      await this.load();
    } catch {
      // Surface the failure inside the dialog, where the user's focus is.
      this.createError.set('Failed to create token.');
    } finally {
      this.creating.set(false);
    }
  }

  closeCreated(): void {
    this.createDialogVisible = false;
    this.createdToken.set(null);
    this.successMessage.set('Token created.');
  }

  onDialogHide(): void {
    this.createdToken.set(null);
    this.createError.set('');
    this.submitted.set(false);
  }

  async revoke(token: ApiTokenDto): Promise<void> {
    this.errorMessage.set('');
    this.successMessage.set('');
    try {
      await this.tokenService.revoke(token.id);
      this.successMessage.set('Token revoked.');
      await this.load();
    } catch {
      this.errorMessage.set('Failed to revoke token.');
    }
  }

  /** Permanently remove a revoked or expired token from the list (#87). */
  async deleteToken(token: ApiTokenDto): Promise<void> {
    this.errorMessage.set('');
    this.successMessage.set('');
    try {
      await this.tokenService.delete(token.id);
      this.successMessage.set('Token deleted.');
      await this.load();
    } catch {
      this.errorMessage.set('Failed to delete token.');
    }
  }

  async copy(): Promise<void> {
    const token = this.createdToken();
    if (!token) {
      return;
    }
    let success = false;
    // navigator.clipboard exists only in secure contexts (HTTPS or localhost);
    // on plain HTTP it is undefined, so fall back to execCommand.
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(token);
        success = true;
      } else {
        success = this.legacyCopy(token);
      }
    } catch {
      success = this.legacyCopy(token);
    }
    if (success) {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    }
  }

  /** Fallback copy for non-secure contexts where navigator.clipboard is unavailable. */
  private legacyCopy(text: string): boolean {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', '');
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    let success = false;
    try {
      success = document.execCommand('copy');
    } catch {
      success = false;
    }
    document.body.removeChild(textarea);
    return success;
  }
}
