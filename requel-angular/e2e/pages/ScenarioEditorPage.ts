import { Page, expect } from '@playwright/test';

export class ScenarioListPage {
  constructor(private page: Page) {}

  async goto(projectName: string): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/scenarios') && r.status() === 200),
      this.page.goto(`/projects/${encodeURIComponent(projectName)}/scenarios`),
    ]);
  }

  async clickNewScenario(): Promise<void> {
    await this.page.locator('app-list-page').getByRole('button', { name: 'New Scenario' }).click();
    await this.page.waitForURL('**/scenarios/new');
  }

  async clickScenario(name: string): Promise<void> {
    await this.page.locator('p-table td', { hasText: name }).first().click();
    await this.page.waitForURL(/\/scenarios\/\d+/);
    await expect(this.page.locator('#name')).not.toHaveValue('');
  }

  async expectScenarioInTable(name: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: name })).toBeVisible();
  }

  async expectScenarioNotInTable(name: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: name })).not.toBeVisible();
  }
}

export class ScenarioEditorPage {
  constructor(private page: Page) {}

  async fillName(name: string): Promise<void> {
    const input = this.page.locator('#name');
    await input.clear();
    await input.fill(name);
  }

  /**
   * Select a scenario type via PrimeNG p-select.
   */
  async selectType(type: 'Primary' | 'PreCondition' | 'Optional' | 'Alternative' | 'Exception'): Promise<void> {
    await this.page.locator('#type').click();
    await this.page.getByRole('option', { name: type }).click();
  }

  async save(): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditScenario')),
      this.page.getByRole('button', { name: 'Save' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`EditScenario failed: ${response.status()} ${await response.text()}`);
    }
  }

  async copy(): Promise<void> {
    await this.page.getByRole('button', { name: 'Copy' }).click();
    // Register the response waiter before clicking Yes — the API call fires immediately on confirm.
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/CopyScenario')),
      this.page.getByRole('button', { name: 'Yes' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`CopyScenario failed: ${response.status()} ${await response.text()}`);
    }
  }

  async delete(): Promise<void> {
    await this.page.getByRole('button', { name: 'Delete' }).click();
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByRole('button', { name: 'Back' }).click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/scenarios`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }

  async expectTypeValue(type: string): Promise<void> {
    await expect(this.page.locator('#type')).toContainText(type);
  }

  // ── Step methods ──────────────────────────────────────────────────────────

  /**
   * Append a new empty step.
   *
   * Uses dispatchEvent('click') instead of .click() because the .add-step-row
   * sits inside a cdkDropList whose nested-scroll-container parent (.main-content
   * overflow-y:auto inside overflow:hidden .layout-body) causes Playwright's
   * elementFromPoint hit-test to return the host element rather than the div,
   * producing a false "intercepts pointer events" failure. dispatchEvent fires
   * the click event directly on the target element, bypassing hit-testing while
   * still triggering Angular's (click)="addStep()" handler.
   */
  async addStep(): Promise<void> {
    await this.page.locator('.add-step-row').last().dispatchEvent('click');
  }

  async expectStepCount(n: number): Promise<void> {
    await expect(this.page.locator('.step-row')).toHaveCount(n);
  }

  async fillStepName(index: number, name: string): Promise<void> {
    const input = this.page.locator('.step-row').nth(index).locator('.step-name-input');
    await input.clear();
    await input.fill(name);
    // Tab out to trigger (blur)="onStepNameChange()" → stepsSaveNeeded = true
    await input.press('Tab');
  }

  async expectStepName(index: number, name: string): Promise<void> {
    await expect(
      this.page.locator('.step-row').nth(index).locator('.step-name-input')
    ).toHaveValue(name);
  }

  async removeStep(index: number): Promise<void> {
    await this.page.locator('.step-row').nth(index).locator('button:has(.pi-times)').click();
  }

  async openStepEdit(index: number): Promise<void> {
    await this.page.locator('.step-row').nth(index).locator('button:has(.pi-pencil)').click();
  }

  async fillStepEditName(name: string): Promise<void> {
    const input = this.page.locator('.edit-popup-content input');
    await input.clear();
    await input.fill(name);
  }

  async fillStepEditText(text: string): Promise<void> {
    const textarea = this.page.locator('.edit-popup-content textarea');
    await textarea.clear();
    await textarea.fill(text);
  }

  async applyStepEdit(): Promise<void> {
    await this.page.getByRole('button', { name: 'Apply' }).click();
    await expect(this.page.locator('.edit-popup-overlay')).not.toBeVisible();
  }

  /**
   * Reorder a step using CDK DragDrop's keyboard drag mode.
   *
   * Mouse-based drag is unreliable in headless Playwright against CDK 21 because
   * the nested scroll container (.main-content overflow-y:auto) causes coordinate
   * confusion in Chromium's hit-testing. CDK's keyboard drag is fully reliable:
   *
   * CDK adds tabindex="0" to the cdkDragHandle element, making it focusable.
   * Pressing Space on the focused handle calls DragRef._startKeyboardDrag(),
   * which registers a document-level keydown listener. ArrowDown/ArrowUp move
   * the item one slot at a time; a second Space confirms the drop.
   */
  async dragStepTo(fromIndex: number, toIndex: number): Promise<void> {
    const handle = this.page.locator('.step-row').nth(fromIndex).locator('.drag-handle');

    // CDK sets tabindex="0" on cdkDragHandle — focus() works without click
    await handle.focus();

    // Start keyboard drag mode
    await this.page.keyboard.press('Space');

    // Move one slot at a time toward toIndex
    const key = toIndex > fromIndex ? 'ArrowDown' : 'ArrowUp';
    for (let i = 0; i < Math.abs(toIndex - fromIndex); i++) {
      await this.page.keyboard.press(key);
    }

    // Confirm the drop
    await this.page.keyboard.press('Space');
  }
}
