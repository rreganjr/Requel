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
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TokenService } from '../../core/token.service';
import { ApiTokenDto } from '../../models/api-token';

/**
 * Personal access token management (#73, Slice 6): list, create (showing the one-time plaintext),
 * and revoke the current user's tokens. Hosted as a section of the Settings page, rendered only for
 * users with the ProjectUserRole (gated by the host).
 */
@Component({
  selector: 'app-api-tokens',
  standalone: true,
  imports: [DatePipe, FormsModule, ButtonModule, DialogModule, InputTextModule, MessageModule,
    SelectModule],
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

      @if (errorMessage()) { <p-message severity="error" [text]="errorMessage()" /> }
      @if (successMessage()) { <p-message severity="success" [text]="successMessage()" /> }

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
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      }

      <p-dialog header="New personal access token" [(visible)]="createDialogVisible"
                [modal]="true" [style]="{ width: '32rem' }" (onHide)="onDialogHide()">
        @if (createdToken() === null) {
          <div class="field">
            <label for="patName">Name</label>
            <input id="patName" pInputText [(ngModel)]="newName" data-testid="pat-name"
                   placeholder="e.g. claude-desktop" />
          </div>
          <div class="field">
            <label for="patExpiry">Expires in</label>
            <p-select inputId="patExpiry" [options]="expiryOptions" optionLabel="label"
                      optionValue="value" [(ngModel)]="newExpiresInDays" data-testid="pat-expiry" />
          </div>
          <div class="dialog-actions">
            <p-button label="Create" icon="pi pi-check" data-testid="pat-create"
                      [loading]="creating()" [disabled]="!newName.trim()" (onClick)="submitCreate()" />
          </div>
        } @else {
          <p-message severity="warn"
                     text="Copy this token now — it will not be shown again." />
          <div class="token-display">
            <code data-testid="pat-plaintext">{{ createdToken() }}</code>
            <p-button label="Copy" icon="pi pi-copy" [text]="true" (onClick)="copy()" />
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

  /** Fixed expiry choices (days); no "never" option for tokens minted from the UI. */
  readonly expiryOptions = [
    { label: '30 days', value: 30 },
    { label: '90 days', value: 90 },
    { label: '180 days', value: 180 },
    { label: '365 days', value: 365 }
  ];

  createDialogVisible = false;
  newName = '';
  newExpiresInDays = 90;

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
    this.newName = '';
    this.newExpiresInDays = 90;
    this.createdToken.set(null);
    this.successMessage.set('');
    this.createDialogVisible = true;
  }

  async submitCreate(): Promise<void> {
    if (!this.newName.trim()) {
      return;
    }
    this.creating.set(true);
    this.errorMessage.set('');
    try {
      const response = await this.tokenService.create({
        name: this.newName.trim(),
        expiresInDays: this.newExpiresInDays
      });
      this.createdToken.set(response.token);
      await this.load();
    } catch {
      this.errorMessage.set('Failed to create token.');
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

  async copy(): Promise<void> {
    const token = this.createdToken();
    if (token) {
      try {
        await navigator.clipboard.writeText(token);
      } catch {
        // clipboard may be unavailable (non-secure context); the token stays visible to copy manually
      }
    }
  }
}
