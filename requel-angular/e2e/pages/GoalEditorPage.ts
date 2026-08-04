import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';
import { FormWizardPage } from './FormWizardPage';

export class GoalListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(projectName: string): Promise<void> {
    await this.gotoList(`/projects/${encodeURIComponent(projectName)}/goals`, '/goals');
  }

  async searchFor(name: string): Promise<void> {
    await this.searchWithPlaceholder('Search goals...', name);
  }

  async clickNewGoal(): Promise<void> {
    await this.clickNewButton('New Goal', '**/goals/new');
  }

  async clickGoal(name: string): Promise<void> {
    await this.searchFor(name);
    // BaseListPage's readySelector defaults to '#name', which the goal editor no longer
    // has — since #158 its form is app-field rows with generated ids.
    await this.clickTableRow(name, /\/goals\/\d+/, '[data-testid="goal-name"]');
  }

  async expectGoalInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowVisible(name);
  }

  async expectGoalNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowNotVisible(name);
  }
}

/**
 * Page object for the goal editor.
 *
 * Since #158 the create route (`/goals/new`) renders a 3-step `app-form-wizard`
 * (Details → Tags → Relations) while the edit route keeps a single card. The form
 * controls are `app-field` rows whose ids are generated, so everything here locates
 * by `data-testid` rather than the old static `#name` / `#text`.
 */
export class GoalEditorPage {
  readonly wizard: FormWizardPage;

  constructor(private page: Page) {
    this.wizard = new FormWizardPage(page);
  }

  private relationRows(name: string) {
    return this.page.getByTestId('goal-relation-row').filter({ hasText: name });
  }

  private nameInput() {
    return this.page.getByTestId('goal-name');
  }

  private descriptionInput() {
    return this.page.getByTestId('goal-text');
  }

  async fillName(name: string): Promise<void> {
    const input = this.nameInput();
    await input.clear();
    await input.fill(name);
  }

  async fillDescription(text: string): Promise<void> {
    const ta = this.descriptionInput();
    await ta.clear();
    await ta.fill(text);
  }

  /**
   * Persist the goal and land on it.
   *
   * On the edit route this is the Save button. On the create route there is no Save —
   * the wizard commits Details on Continue, so this commits, skips the two optional
   * steps and presses Done, which navigates to the saved goal. That reproduces the
   * pre-#158 contract of "fill the fields, call save(), end up on /goals/<id>", so
   * existing specs keep working unchanged.
   *
   * Use `commitDetails()` / `wizard` directly when a test needs the individual steps.
   */
  async save(): Promise<void> {
    if (await this.wizard.isPresent()) {
      await this.commitDetails();
      await this.wizard.skipToStep('relations');
      await this.wizard.finish(/\/goals\/\d+/);
      return;
    }
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditGoal')),
      this.page.getByTestId('goal-save').click(),
    ]);
    if (!response.ok()) {
      throw new Error(`EditGoal failed: ${response.status()} ${await response.text()}`);
    }
  }

  /** Commit the wizard's Details step, which creates (or updates) the goal. */
  async commitDetails(): Promise<void> {
    await this.wizard.commitStep('EditGoal');
  }

  /** The edit-route Save button, for asserting the disable policy. */
  saveButton() {
    return this.page.getByTestId('goal-save').locator('button');
  }

  async delete(): Promise<void> {
    await this.page.getByTestId('goal-delete').click();
    // PrimeNG p-confirmDialog accept button
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async copy(): Promise<void> {
    await this.page.getByTestId('goal-copy').click();
    // Register the response waiter before clicking Yes — the API call fires immediately on confirm.
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/CopyGoal')),
      this.page.getByRole('button', { name: 'Yes' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`CopyGoal failed: ${response.status()} ${await response.text()}`);
    }
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByTestId('goal-back').click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/goals`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.nameInput()).toHaveValue(name);
  }

  async expectDescriptionValue(text: string): Promise<void> {
    await expect(this.descriptionInput()).toHaveValue(text);
  }

  /** The inline error rendered by `app-field` under an invalid control. */
  fieldError() {
    return this.page.getByTestId('field-error');
  }

  /**
   * Blur the Name input. `app-field` only shows an error once the control is touched
   * (or a save has been attempted), so a test that clears the field has to leave it
   * before asserting on the message.
   */
  async nameBlur(): Promise<void> {
    await this.nameInput().blur();
  }

  /**
   * Assert the Name row wires label, error and required state to the input, which is
   * what makes the label/error association structural rather than per-editor (#138).
   */
  async expectNameAccessiblyLabelled(): Promise<void> {
    const input = this.nameInput();
    const id = await input.getAttribute('id');
    expect(id).toBeTruthy();
    await expect(this.page.locator(`label[for="${id}"]`)).toContainText('Name');
    await expect(input).toHaveAttribute('aria-required', 'true');
  }

  /**
   * Click "Add Relation", pick the target goal in the entity-selector dialog,
   * optionally change the relation type, then confirm.
   */
  async addRelation(toGoalName: string, relationType: 'Supports' | 'Conflicts' = 'Supports'): Promise<void> {
    await this.page.getByTestId('goal-add-relation').click();
    // p-dialog with appendTo="body" renders at document root; use role+name to distinguish
    // from p-confirmDialog (which renders as alertdialog).
    const dialog = this.page.getByRole('dialog', { name: 'Select Goal' });
    await dialog.waitFor({ state: 'visible' });
    await dialog.getByTestId('entity-selector-search').fill(toGoalName);
    await dialog.getByTestId('entity-selector-row').filter({ hasText: toGoalName }).first().click();
    // Relation-type dialog appears after the goal is selected. Like the selector above,
    // it is now a p-dialog with appendTo="body", so the dialog DOM is teleported to the
    // document root and the goal-relation-type-dialog testid sits on an empty, never-visible
    // host element. Match the real dialog by role+name (header is `Relation to "<goal>"`).
    const relationDialog = this.page.getByRole('dialog', { name: `Relation to "${toGoalName}"` });
    await relationDialog.waitFor({ state: 'visible' });
    if (relationType !== 'Supports') {
      await relationDialog.getByTestId('goal-relation-type-select').click();
      await this.page.getByRole('option', { name: relationType }).click();
    }
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditGoalRelation')),
      relationDialog.getByTestId('goal-relation-add').click(),
    ]);
    if (!response.ok()) {
      throw new Error(`EditGoalRelation failed: ${response.status()} ${await response.text()}`);
    }
  }

  async removeRelation(toGoalName: string): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/DeleteGoalRelation')),
      this.relationRows(toGoalName).getByTestId('goal-remove-relation').first().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`DeleteGoalRelation failed: ${response.status()} ${await response.text()}`);
    }
  }

  async expectRelationInTable(toGoalName: string): Promise<void> {
    await expect(this.relationRows(toGoalName).first()).toBeVisible();
  }

  async expectRelationNotInTable(toGoalName: string): Promise<void> {
    await expect(this.relationRows(toGoalName)).toHaveCount(0);
  }
}
