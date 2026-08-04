import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';
import { FormWizardPage } from './FormWizardPage';

export class StoryListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(projectName: string): Promise<void> {
    const encodedProjectName = encodeURIComponent(projectName);
    await this.gotoList(
      `/projects/${encodedProjectName}/stories`,
      response => new URL(response.url()).pathname === `/api/projects/${encodedProjectName}/stories`
    );
  }

  async searchFor(name: string): Promise<void> {
    await this.searchWithPlaceholder('Search stories...', name);
  }

  async clickNewStory(): Promise<void> {
    await this.clickNewButton('New Story', '**/stories/new');
  }

  async clickStory(name: string): Promise<void> {
    await this.searchFor(name);
    // BaseListPage's readySelector defaults to '#name', which the story editor no longer
    // has — since #158 its form is app-field rows with generated ids.
    await this.clickTableRow(name, /\/stories\/\d+/, '[data-testid="story-name"]');
  }

  async expectStoryInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowVisible(name);
  }

  async expectStoryNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowNotVisible(name);
  }

  async countStoryRows(nameSubstring: string): Promise<number> {
    await this.searchFor(nameSubstring);
    return this.countTableRows(nameSubstring);
  }
}

/**
 * Page object for the story editor.
 *
 * Since #158 the create route (`/stories/new`) renders a 3-step `app-form-wizard`
 * (Details → Goals → Additional Actors) while the edit route keeps a single card.
 * The form controls are `app-field` rows whose ids are generated for plain inputs,
 * so those are located by `data-testid` rather than the old static `#name` / `#text`.
 * The two `p-select`s keep their explicit `inputId`s (`storyTypeInput`,
 * `storyPrimaryActorInput`), which is what `app-field`'s `controlId` binds the label to.
 */
export class StoryEditorPage {
  readonly wizard: FormWizardPage;

  constructor(private page: Page) {
    this.wizard = new FormWizardPage(page);
  }

  private goalRows(name: string) {
    return this.page.getByTestId('story-goal-row').filter({ hasText: name });
  }

  private storyTypeSelect() {
    return this.page.getByTestId('story-type');
  }

  private primaryActorSelect() {
    return this.page.getByTestId('story-primary-actor');
  }

  private nameInput() {
    return this.page.getByTestId('story-name');
  }

  private textInput() {
    return this.page.getByTestId('story-text');
  }

  async fillName(name: string): Promise<void> {
    const input = this.nameInput();
    await input.clear();
    await input.fill(name);
  }

  async fillText(text: string): Promise<void> {
    const ta = this.textInput();
    await ta.clear();
    await ta.fill(text);
  }

  /**
   * Select a story type via PrimeNG p-select.
   * Clicks the dropdown trigger, then selects the matching option.
   */
  async selectStoryType(type: 'Success' | 'Exception'): Promise<void> {
    await this.storyTypeSelect().click();
    await this.page.getByRole('option', { name: type }).click();
  }

  /**
   * Select a primary actor by name, or "(none)" to clear.
   */
  async selectPrimaryActor(actorName: string): Promise<void> {
    await this.primaryActorSelect().click();
    await this.page.getByRole('option', { name: actorName }).click();
  }

  /**
   * Persist the story and land on it.
   *
   * On the edit route this is the Save button. On the create route there is no Save —
   * the wizard commits Details on Continue, so this commits, skips the two optional
   * steps and presses Done, which navigates to the saved story. That reproduces the
   * pre-#158 contract of "fill the fields, call save(), end up on /stories/<id>", so
   * existing specs keep working unchanged.
   *
   * Use `commitDetails()` / `wizard` directly when a test needs the individual steps.
   */
  async save(): Promise<void> {
    if (await this.wizard.isPresent()) {
      await this.commitDetails();
      await this.wizard.skipToStep('actors');
      await this.wizard.finish(/\/stories\/\d+/);
      return;
    }
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditStory')),
      this.page.getByTestId('story-save').click(),
    ]);
    if (!response.ok()) {
      throw new Error(`EditStory command failed: ${response.status()} ${await response.text()}`);
    }
  }

  /** Commit the wizard's Details step, which creates (or updates) the story. */
  async commitDetails(): Promise<void> {
    await this.wizard.commitStep('EditStory');
  }

  /** The edit-route Save button, for asserting the disable policy. */
  saveButton() {
    return this.page.getByTestId('story-save').locator('button');
  }

  /** The inline error rendered by `app-field` under an invalid control. */
  fieldError() {
    return this.page.getByTestId('field-error');
  }

  /**
   * Assert both `p-select` rows point their label at the input PrimeNG renders inside
   * the wrapper — the wrapper case `app-field`'s `controlId` exists for.
   */
  async expectSelectsAccessiblyLabelled(): Promise<void> {
    await expect(this.page.locator('label[for="storyTypeInput"]')).toContainText('Type');
    await expect(this.page.locator('#storyTypeInput')).toHaveCount(1);
    await expect(this.page.locator('label[for="storyPrimaryActorInput"]')).toContainText(
      'Primary Actor'
    );
    await expect(this.page.locator('#storyPrimaryActorInput')).toHaveCount(1);
  }

  async clearPrimaryActor(): Promise<void> {
    await this.page.getByTestId('story-primary-actor-clear').click();
  }

  async expectPrimaryActorValue(actorName: string): Promise<void> {
    await expect(this.primaryActorSelect()).toContainText(actorName);
  }

  async expectNoPrimaryActor(): Promise<void> {
    // p-select shows the placeholder when no value is selected
    await expect(this.primaryActorSelect()).toContainText('Select primary actor');
  }

  /**
   * Open the primary-actor p-select and assert that the named actor is offered
   * as one of the visible options, then dismiss the dropdown via Escape so it
   * doesn't intercept later interactions on the page. Used by actors.e2e.ts to
   * verify a newly-created actor appears in the editor's actor list without
   * actually selecting it. Mirrors the same-named helper on UseCaseEditorPage.
   */
  async expectActorInPrimaryActorDropdown(actorName: string): Promise<void> {
    await this.primaryActorSelect().click();
    await expect(this.page.getByRole('option', { name: actorName })).toBeVisible();
    await this.page.keyboard.press('Escape');
  }

  async delete(): Promise<void> {
    await this.page.getByTestId('story-delete').click();
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/DeleteStory')),
      this.page.getByRole('button', { name: 'Yes' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`DeleteStory command failed: ${response.status()} ${await response.text()}`);
    }
  }

  async copy(): Promise<number> {
    await this.page.getByTestId('story-copy').click();
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/CopyStory')),
      this.page.getByRole('button', { name: 'Yes' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`CopyStory failed: ${response.status()} ${await response.text()}`);
    }
    const body = await response.json() as { entity?: { id?: number } };
    const copiedId = body.entity?.id;
    if (copiedId == null) {
      throw new Error('CopyStory succeeded but did not return an entity id');
    }
    return copiedId;
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByTestId('story-back').click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/stories`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.nameInput()).toHaveValue(name);
  }

  async expectTextValue(text: string): Promise<void> {
    await expect(this.textInput()).toHaveValue(text);
  }

  async expectStoryTypeValue(type: string): Promise<void> {
    // p-select renders the selected value in a span inside the component
    await expect(this.storyTypeSelect()).toContainText(type);
  }

  async addAdditionalActor(actorName: string): Promise<void> {
    await this.page.getByTestId('story-add-actor').click();
    const dialog = this.page.getByRole('dialog', { name: 'Select Actor' });
    await dialog.waitFor({ state: 'visible' });
    await dialog.getByTestId('entity-selector-search').fill(actorName);
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/AddActorToActorContainer')),
      dialog.getByTestId('entity-selector-row').filter({ hasText: actorName }).first().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`AddActorToActorContainer failed: ${response.status()} ${await response.text()}`);
    }
  }

  async removeAdditionalActor(actorName: string): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/RemoveActorFromActorContainer')),
      this.page.getByTestId('story-additional-actor-row').filter({ hasText: actorName })
               .getByTestId('story-remove-additional-actor')
               .first()
               .click(),
    ]);
    if (!response.ok()) {
      throw new Error(`RemoveActorFromActorContainer failed: ${response.status()} ${await response.text()}`);
    }
  }

  async expectAdditionalActorInTable(actorName: string): Promise<void> {
    await expect(
      this.page.getByTestId('story-additional-actor-row').filter({ hasText: actorName }).first()
    ).toBeVisible();
  }

  async expectAdditionalActorNotInTable(actorName: string): Promise<void> {
    await expect(
      this.page.getByTestId('story-additional-actor-row').filter({ hasText: actorName })
    ).toHaveCount(0);
  }

  async clickAdditionalActor(actorName: string): Promise<void> {
    await this.page.getByTestId('story-additional-actor-row').filter({ hasText: actorName })
      .getByTestId('story-additional-actor-link')
      .first()
      .click();
  }

  async addGoal(goalName: string): Promise<void> {
    await this.page.getByTestId('story-add-goal').click();
    const dialog = this.page.getByRole('dialog', { name: 'Select Goal' });
    await dialog.waitFor({ state: 'visible' });
    await dialog.getByTestId('entity-selector-search').fill(goalName);
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/AddGoalToGoalContainer')),
      dialog.getByTestId('entity-selector-row').filter({ hasText: goalName }).first().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`AddGoalToGoalContainer failed: ${response.status()} ${await response.text()}`);
    }
  }

  async removeGoal(goalName: string): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/RemoveGoalFromGoalContainer')),
      this.goalRows(goalName).getByTestId('story-remove-goal').first().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`RemoveGoalFromGoalContainer failed: ${response.status()} ${await response.text()}`);
    }
  }

  async clickGoal(goalName: string): Promise<void> {
    await this.goalRows(goalName).getByTestId('story-goal-link').first().click();
  }

  async expectGoalInTable(goalName: string): Promise<void> {
    await expect(this.goalRows(goalName).first()).toBeVisible();
  }

  async expectGoalNotInTable(goalName: string): Promise<void> {
    await expect(this.goalRows(goalName)).toHaveCount(0);
  }
}
