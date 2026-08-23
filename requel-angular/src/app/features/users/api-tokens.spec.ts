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
import { ApiTokensComponent } from './api-tokens';
import { TokenService } from '../../core/token.service';

/**
 * Regression coverage for the "Copy" button on the new personal access token dialog (#73).
 * The original implementation called navigator.clipboard.writeText() unconditionally and
 * swallowed the failure, so the token was never copied on plain-HTTP (non-secure) deployments
 * where navigator.clipboard is undefined. These tests verify the secure-context path, the
 * execCommand fallback, and that the copied indicator only flips on success.
 */
describe('ApiTokensComponent.copy', () => {

  let component: ApiTokensComponent;
  const tokenServiceStub = {} as TokenService;

  const originalClipboard = Object.getOwnPropertyDescriptor(navigator, 'clipboard');
  const originalIsSecureContext = Object.getOwnPropertyDescriptor(window, 'isSecureContext');

  function setClipboard(value: unknown): void {
    Object.defineProperty(navigator, 'clipboard', { value, configurable: true });
  }

  function setSecureContext(value: boolean): void {
    Object.defineProperty(window, 'isSecureContext', { value, configurable: true });
  }

  beforeEach(() => {
    component = new ApiTokensComponent(tokenServiceStub);
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    if (originalClipboard) {
      Object.defineProperty(navigator, 'clipboard', originalClipboard);
    }
    if (originalIsSecureContext) {
      Object.defineProperty(window, 'isSecureContext', originalIsSecureContext);
    }
  });

  it('does nothing when there is no created token', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    setClipboard({ writeText });
    setSecureContext(true);

    component.createdToken.set(null);
    await component.copy();

    expect(writeText).not.toHaveBeenCalled();
    expect(component.copied()).toBe(false);
  });

  it('uses navigator.clipboard in a secure context and flips the copied indicator', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    setClipboard({ writeText });
    setSecureContext(true);

    component.createdToken.set('secret-token');
    await component.copy();

    expect(writeText).toHaveBeenCalledWith('secret-token');
    expect(component.copied()).toBe(true);

    // The indicator resets after the 2s timeout.
    vi.advanceTimersByTime(2000);
    expect(component.copied()).toBe(false);
  });

  it('falls back to execCommand when not in a secure context (plain HTTP)', async () => {
    setClipboard(undefined);
    setSecureContext(false);
    const execCommand = vi.fn().mockReturnValue(true);
    // jsdom does not implement execCommand; define it for this test.
    Object.defineProperty(document, 'execCommand', { value: execCommand, configurable: true });

    component.createdToken.set('secret-token');
    await component.copy();

    expect(execCommand).toHaveBeenCalledWith('copy');
    expect(component.copied()).toBe(true);
  });

  it('falls back to execCommand when navigator.clipboard.writeText rejects', async () => {
    const writeText = vi.fn().mockRejectedValue(new Error('denied'));
    setClipboard({ writeText });
    setSecureContext(true);
    const execCommand = vi.fn().mockReturnValue(true);
    Object.defineProperty(document, 'execCommand', { value: execCommand, configurable: true });

    component.createdToken.set('secret-token');
    await component.copy();

    expect(writeText).toHaveBeenCalled();
    expect(execCommand).toHaveBeenCalledWith('copy');
    expect(component.copied()).toBe(true);
  });

  it('leaves the copied indicator false when the fallback copy fails', async () => {
    setClipboard(undefined);
    setSecureContext(false);
    const execCommand = vi.fn().mockReturnValue(false);
    Object.defineProperty(document, 'execCommand', { value: execCommand, configurable: true });

    component.createdToken.set('secret-token');
    await component.copy();

    expect(execCommand).toHaveBeenCalledWith('copy');
    expect(component.copied()).toBe(false);
  });
});

/**
 * Coverage for the hard-delete handler (#87): a revoked/expired row's Delete button removes the
 * token via TokenService.delete(), then refreshes the list.
 */
describe('ApiTokensComponent.deleteToken', () => {

  const revokedToken = {
    id: 7, name: 'old', createdAt: '', lastUsedAt: null, expiresAt: null, status: 'REVOKED' as const
  };

  it('deletes the token, shows a success message, and reloads the list', async () => {
    const del = vi.fn().mockResolvedValue(undefined);
    const list = vi.fn().mockResolvedValue([]);
    const component = new ApiTokensComponent({ delete: del, list } as unknown as TokenService);

    await component.deleteToken(revokedToken);

    expect(del).toHaveBeenCalledWith(7);
    expect(list).toHaveBeenCalled();
    expect(component.successMessage()).toBe('Token deleted.');
    expect(component.errorMessage()).toBe('');
  });

  it('shows an error message when the delete fails', async () => {
    const del = vi.fn().mockRejectedValue(new Error('409'));
    const list = vi.fn().mockResolvedValue([revokedToken]);
    const component = new ApiTokensComponent({ delete: del, list } as unknown as TokenService);

    await component.deleteToken(revokedToken);

    expect(del).toHaveBeenCalledWith(7);
    expect(component.errorMessage()).toBe('Failed to delete token.');
    expect(component.successMessage()).toBe('');
  });
});


/**
 * Coverage for the create flow (#134): the name field is required (validate-on-submit,
 * inline message) and a create failure surfaces inside the dialog via `createError`,
 * not the page-level `errorMessage` slot.
 */
describe('ApiTokensComponent.submitCreate', () => {
  it('shows the required error and does not call create when the name is blank', async () => {
    const create = vi.fn();
    const list = vi.fn().mockResolvedValue([]);
    const component = new ApiTokensComponent({ create, list } as unknown as TokenService);
    component.createForm.controls.name.setValue('   ');
    await component.submitCreate();
    expect(create).not.toHaveBeenCalled();
    expect(component.submitted()).toBe(true);
    expect(component.createForm.controls.name.invalid).toBe(true);
    expect(component.createForm.controls.name.touched).toBe(true);
  });

  it('surfaces a create failure inside the dialog (createError), not the page slot', async () => {
    const create = vi.fn().mockRejectedValue(new Error('boom'));
    const list = vi.fn().mockResolvedValue([]);
    const component = new ApiTokensComponent({ create, list } as unknown as TokenService);
    component.createForm.controls.name.setValue('claude-desktop');
    await component.submitCreate();
    expect(create).toHaveBeenCalledWith({ name: 'claude-desktop', expiresInDays: 90 });
    expect(component.createError()).toBe('Failed to create token.');
    expect(component.errorMessage()).toBe('');
  });

  it('stores the one-time plaintext token on success', async () => {
    const create = vi.fn().mockResolvedValue({ token: 'plaintext-abc' });
    const list = vi.fn().mockResolvedValue([]);
    const component = new ApiTokensComponent({ create, list } as unknown as TokenService);
    component.createForm.controls.name.setValue('claude-desktop');
    await component.submitCreate();
    expect(component.createdToken()).toBe('plaintext-abc');
    expect(component.createError()).toBe('');
  });
});
