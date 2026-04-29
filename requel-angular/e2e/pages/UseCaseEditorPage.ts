import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

export class UseCaseListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(projectName: string): Promise<void> {
    await this.gotoList(`/projects/${encodeURIComponent(projectName)}/use-cases`, '/use-cases');
  }

  async clickNewUseCase(): Promise<void> {
    await this.clickNewButton('New Use Case', '**/use-cases/new');
  }

  async clickUseCase(name: string): Promise<void> {
    await this.clickTableRow(name, /\/use-cases\/\d+/);
  }

  async expectUseCaseInTable(name: string): Promise<void> {
    await this.expectTableRowVisible(name);
  }

  async expectUseCaseNotInTable(name: string): Promise<void> {
    await this.expectTableRowNotVisible(name);
  }

  async countUseCaseRows(name: string): Promise<number> {
    return this.countTableRows(name);
  }
}

export class UseCaseEditorPage {
  constructor(private page: Page) {}

  private primaryActorSelect() {
    return this.page.getByTestId('use-case-primary-actor');
  }

  private tableRows(testId: string, name: string) {
    return this.page.getByTestId(testId).filter({ hasText: name });
  }

  async fillName(name: string): Promise<void> {
    const input = this.page.locator('#name');
    await input.clear();
    await input.fill(name);
  }

  async fillDescription(text: string): Promise<void> {
    const ta = this.page.locator('#text');
    await ta.clear();
    await ta.fill(text);
  }

  async selectPrimaryActor(actorName: string): Promise<void> {
    await this.primaryActorSelect().click();
    await this.page.getByRole('option', { name: actorName }).click();
  }

  async clearPrimaryActor(): Promise<void> {
    await this.page.getByTestId('use-case-primary-actor-clear').click();
  }

  async save(): Promise<void> {
    await this.page.getByTestId('use-case-save').click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async copy(): Promise<void> {
    await this.page.getByRole('button', { name: 'Copy' }).click();
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/CopyUseCase')),
      this.page.getByRole('button', { name: 'Yes' }).click(),
    ]);
    if (!response.ok()) {
      throw new Error(`CopyUseCase failed: ${response.status()} ${await response.text()}`);
    }
  }

  async delete(): Promise<void> {
    await this.page.getByRole('button', { name: 'Delete' }).click();
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByRole('button', { name: 'Back' }).click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/use-cases`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }

  // ── Sub-table helpers (Goals, Stories, Additional Actors, Additional Scenarios) ──

  private async addViaSelector(
    buttonTestId: string,
    dialogName: string,
    entityName: string,
    commandUrlFragment: string
  ): Promise<void> {
    await this.page.getByTestId(buttonTestId).click();
    const dialog = this.page.getByRole('dialog', { name: dialogName });
    await dialog.waitFor({ state: 'visible' });
    await dialog.getByTestId('entity-selector-search').fill(entityName);
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes(commandUrlFragment)),
      dialog.getByTestId('entity-selector-row').filter({ hasText: entityName }).first().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`${commandUrlFragment} failed: ${response.status()} ${await response.text()}`);
    }
  }

  private async removeFromTable(rowTestId: string, removeButtonTestId: string, entityName: string, commandUrlFragment: string): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes(commandUrlFragment)),
      this.tableRows(rowTestId, entityName)
               .getByTestId(removeButtonTestId)
               .click(),
    ]);
    if (!response.ok()) {
      throw new Error(`${commandUrlFragment} failed: ${response.status()} ${await response.text()}`);
    }
  }

  async addGoal(goalName: string): Promise<void> {
    await this.addViaSelector('use-case-add-goal', 'Select Goal', goalName, '/api/commands/AddGoalToGoalContainer');
  }

  async removeGoal(goalName: string): Promise<void> {
    await this.removeFromTable('use-case-goal-row', 'use-case-remove-goal', goalName, '/api/commands/RemoveGoalFromGoalContainer');
  }

  async addStory(storyName: string): Promise<void> {
    await this.addViaSelector('use-case-add-story', 'Select Story', storyName, '/api/commands/AddStoryToStoryContainer');
  }

  async removeStory(storyName: string): Promise<void> {
    await this.removeFromTable('use-case-story-row', 'use-case-remove-story', storyName, '/api/commands/RemoveStoryFromStoryContainer');
  }

  async addAdditionalActor(actorName: string): Promise<void> {
    await this.addViaSelector('use-case-add-actor', 'Select Actor', actorName, '/api/commands/AddActorToActorContainer');
  }

  async removeAdditionalActor(actorName: string): Promise<void> {
    await this.removeFromTable('use-case-actor-row', 'use-case-remove-actor', actorName, '/api/commands/RemoveActorFromActorContainer');
  }

  async addAdditionalScenario(scenarioName: string): Promise<void> {
    await this.addViaSelector('use-case-add-scenario', 'Select Scenario', scenarioName, '/api/commands/AddScenarioToUseCase');
  }

  async removeAdditionalScenario(scenarioName: string): Promise<void> {
    await this.removeFromTable('use-case-scenario-row', 'use-case-remove-scenario', scenarioName, '/api/commands/RemoveScenarioFromUseCase');
  }

  async expectInTable(name: string): Promise<void> {
    await expect(
      this.page.locator('[data-testid$="-row"]', { hasText: name }).first()
    ).toBeVisible();
  }

  async expectNotInTable(name: string): Promise<void> {
    await expect(
      this.page.locator('[data-testid$="-row"]', { hasText: name })
    ).toHaveCount(0);
  }

  // ── Primary scenario helpers ──

  async openPrimaryScenarioInEditor(): Promise<void> {
    await this.page.getByTestId('use-case-open-primary-scenario').click();
    await this.page.waitForURL(/\/scenarios\/\d+/);
  }

  async expectPrimaryScenarioName(name: string): Promise<void> {
    await expect(this.page.getByTestId('use-case-primary-scenario-name')).toContainText(name);
  }
}
