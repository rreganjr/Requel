import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

export class ScenarioListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(projectName: string): Promise<void> {
    await this.gotoList(`/projects/${encodeURIComponent(projectName)}/scenarios`, '/scenarios');
  }

  async clickNewScenario(): Promise<void> {
    await this.clickNewButton('New Scenario', '**/scenarios/new');
  }

  async clickScenario(name: string): Promise<void> {
    await this.clickTableRow(name, /\/scenarios\/\d+/);
  }

  async expectScenarioInTable(name: string): Promise<void> {
    await this.expectTableRowVisible(name);
  }

  async expectScenarioNotInTable(name: string): Promise<void> {
    await this.expectTableRowNotVisible(name);
  }
}

export class ScenarioEditorPage {
  constructor(private page: Page) {}

  private saveButton() {
    return this.page.getByTestId('scenario-save');
  }

  private stepRows() {
    return this.page.getByTestId('scenario-step-row');
  }

  private waitForScenarioDetailReload(): Promise<unknown> {
    return this.page.waitForResponse(response => {
      if (response.request().method() !== 'GET') {
        return false;
      }
      const pathname = new URL(response.url()).pathname;
      return /\/api\/projects\/[^/]+\/scenarios\/\d+$/.test(pathname);
    }, { timeout: 5000 }).catch(() => null);
  }

  async fillName(name: string): Promise<void> {
    const input = this.page.locator('#name');
    await input.clear();
    await input.fill(name);
  }

  /**
   * Select a scenario type via PrimeNG p-select.
   */
  async selectType(type: 'Primary' | 'PreCondition' | 'Optional' | 'Alternative' | 'Exception'): Promise<void> {
    await this.page.getByTestId('scenario-type').click();
    await this.page.getByRole('option', { name: type }).click();
  }

  async save(): Promise<void> {
    await expect(this.saveButton()).toBeEnabled();
    const scenarioReloadPromise = this.waitForScenarioDetailReload();
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditScenario')),
      this.saveButton().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`EditScenario failed: ${response.status()} ${await response.text()}`);
    }
    const result = await response.json() as { success?: boolean; error?: string };
    if (result.success === false) {
      throw new Error(`EditScenario failed: ${result.error ?? 'command returned success=false'}`);
    }
    await scenarioReloadPromise;
  }

  async copy(): Promise<void> {
    await this.page.getByTestId('scenario-copy').click();
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
    await this.page.getByTestId('scenario-delete').click();
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByTestId('scenario-back').click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/scenarios`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }

  async expectTypeValue(type: string): Promise<void> {
    await expect(this.page.getByTestId('scenario-type')).toContainText(type);
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
    await this.page.getByTestId('scenario-add-step-bottom').dispatchEvent('click');
  }

  async expectStepCount(n: number): Promise<void> {
    await expect(this.stepRows()).toHaveCount(n);
  }

  async fillStepName(index: number, name: string): Promise<void> {
    const input = this.stepRows().nth(index).getByTestId('scenario-step-name');
    await input.clear();
    await input.fill(name);
    // Tab out to trigger (blur)="onStepNameChange()" → stepsSaveNeeded = true
    await input.press('Tab');
  }

  async expectStepName(index: number, name: string): Promise<void> {
    await expect(
      this.stepRows().nth(index).getByTestId('scenario-step-name')
    ).toHaveValue(name);
  }

  /**
   * Assert that the step at `index` is a sub-scenario row (rendered as an
   * <a class="entity-link step-name"> link, not an <input>) with the
   * expected display text. Use this for steps where `isScenario === true`;
   * regular text steps should still use `expectStepName()`, which asserts
   * on the input value.
   */
  async expectSubScenarioStepLink(index: number, name: string): Promise<void> {
    await expect(
      this.stepRows().nth(index).getByTestId('scenario-step-link')
    ).toHaveText(name);
  }

  async removeStep(index: number): Promise<void> {
    const rows = this.stepRows();
    const initialCount = await rows.count();
    await rows.nth(index).getByTestId('scenario-step-remove').click();
    await expect(rows).toHaveCount(initialCount - 1);
  }

  async openStepEdit(index: number): Promise<void> {
    await this.stepRows().nth(index).getByTestId('scenario-step-edit').click();
  }

  async fillStepEditName(name: string): Promise<void> {
    const input = this.page.getByTestId('scenario-step-edit-name');
    await input.clear();
    await input.fill(name);
  }

  async fillStepEditText(text: string): Promise<void> {
    const textarea = this.page.getByTestId('scenario-step-edit-text');
    await textarea.clear();
    await textarea.fill(text);
  }

  async applyStepEdit(): Promise<void> {
    await this.page.getByTestId('scenario-step-edit-apply').click();
    await expect(this.page.getByTestId('scenario-step-edit-overlay')).not.toBeVisible();
  }

  // ── Sub-scenario selector dialog ─────────────────────────────────────────

  /** Locator for the dialog content. PrimeNG renders the dialog into <body>
   * via appendTo="body", so this can't be scoped to the editor's DOM. */
  private selectorDialog() {
    return this.page.locator('.scenario-selector-dialog');
  }

  /** Click the "Add Sub-scenario" button on the steps section. */
  async clickAddSubScenario(): Promise<void> {
    await this.page.getByTestId('scenario-add-sub').click();
  }

  /** Wait for the selector dialog to be open and visible. */
  async expectSelectorDialogOpen(): Promise<void> {
    await expect(this.selectorDialog()).toBeVisible();
  }

  /** Wait for the selector dialog to close. */
  async expectSelectorDialogClosed(): Promise<void> {
    await expect(this.selectorDialog()).not.toBeVisible();
  }

  /**
   * Pick an existing scenario from the selector's table by name. Filters the
   * table via the search input first to keep the click target unique even
   * when the project has many scenarios.
   */
  async pickScenarioInDialog(name: string): Promise<void> {
    await this.page.getByTestId('scenario-selector-search').fill(name);
    await this.selectorDialog()
      .getByTestId('scenario-selector-row')
      .filter({ hasText: name })
      .first()
      .click();
  }

  /** Toggle the inline "New Scenario" create form inside the selector dialog. */
  async openNewScenarioFormInDialog(): Promise<void> {
    await this.page.getByTestId('scenario-selector-new-button').click();
    await expect(this.page.getByTestId('scenario-selector-create-form')).toBeVisible();
  }

  /** Fill the inline new-scenario name input. */
  async fillNewScenarioName(name: string): Promise<void> {
    const input = this.page.getByTestId('scenario-selector-name-input');
    await input.clear();
    await input.fill(name);
  }

  /**
   * Click the inline form's "Create & Add" button and wait for the
   * EditScenario command response so the test can synchronously act on the
   * new step that the dialog appends to the parent scenario.
   */
  async clickCreateAndAddInDialog(): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditScenario')),
      this.page.getByTestId('scenario-selector-create-add').click(),
    ]);
    if (!response.ok()) {
      throw new Error(`EditScenario failed inside selector dialog: ${response.status()} ${await response.text()}`);
    }
  }

  /**
   * Dismiss the selector dialog by pressing Escape — exercises PrimeNG's
   * built-in Esc-to-close path which fires (onHide) → the dialog's onHide()
   * handler. We don't expose an explicit close button on the dialog header,
   * so Esc is the cleanest way to drive cancel.
   */
  async cancelSelectorDialog(): Promise<void> {
    await this.page.keyboard.press('Escape');
    await this.expectSelectorDialogClosed();
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
    const handle = this.stepRows().nth(fromIndex).getByTestId('scenario-step-drag-handle');

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
