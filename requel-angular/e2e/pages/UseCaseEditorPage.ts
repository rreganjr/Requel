import { Page, expect } from '@playwright/test';

export class UseCaseListPage {
  constructor(private page: Page) {}

  async goto(projectName: string): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/use-cases') && r.status() === 200),
      this.page.goto(`/projects/${encodeURIComponent(projectName)}/use-cases`),
    ]);
  }

  async clickNewUseCase(): Promise<void> {
    await this.page.locator('app-list-page').getByRole('button', { name: 'New Use Case' }).click();
    await this.page.waitForURL('**/use-cases/new');
  }

  async clickUseCase(name: string): Promise<void> {
    await this.page.locator('p-table td', { hasText: name }).first().click();
    await this.page.waitForURL(/\/use-cases\/\d+/);
    await expect(this.page.locator('#name')).not.toHaveValue('');
  }

  async expectUseCaseInTable(name: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: name })).toBeVisible();
  }

  async expectUseCaseNotInTable(name: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: name })).not.toBeVisible();
  }
}

export class UseCaseEditorPage {
  constructor(private page: Page) {}

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
    await this.page.locator('#primaryActor').click();
    await this.page.getByRole('option', { name: actorName }).click();
  }

  async save(): Promise<void> {
    await this.page.getByRole('button', { name: 'Save' }).click();
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
    buttonLabel: string,
    dialogName: string,
    entityName: string,
    commandUrlFragment: string
  ): Promise<void> {
    await this.page.getByRole('button', { name: buttonLabel }).click();
    const dialog = this.page.getByRole('dialog', { name: dialogName });
    await dialog.waitFor({ state: 'visible' });
    await dialog.locator('[aria-label="Search"]').fill(entityName);
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes(commandUrlFragment)),
      dialog.locator('p-table tr', { hasText: entityName }).first().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`${commandUrlFragment} failed: ${response.status()} ${await response.text()}`);
    }
  }

  private async removeFromTable(entityName: string, commandUrlFragment: string): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes(commandUrlFragment)),
      this.page.locator('p-table tr', { hasText: entityName })
               .locator('p-button')
               .click(),
    ]);
    if (!response.ok()) {
      throw new Error(`${commandUrlFragment} failed: ${response.status()} ${await response.text()}`);
    }
  }

  async addGoal(goalName: string): Promise<void> {
    await this.addViaSelector('Add Goal', 'Select Goal', goalName, '/api/commands/AddGoalToGoalContainer');
  }

  async removeGoal(goalName: string): Promise<void> {
    await this.removeFromTable(goalName, '/api/commands/RemoveGoalFromGoalContainer');
  }

  async addStory(storyName: string): Promise<void> {
    await this.addViaSelector('Add Story', 'Select Story', storyName, '/api/commands/AddStoryToStoryContainer');
  }

  async removeStory(storyName: string): Promise<void> {
    await this.removeFromTable(storyName, '/api/commands/RemoveStoryFromStoryContainer');
  }

  async addAdditionalActor(actorName: string): Promise<void> {
    await this.addViaSelector('Add Actor', 'Select Actor', actorName, '/api/commands/AddActorToActorContainer');
  }

  async removeAdditionalActor(actorName: string): Promise<void> {
    await this.removeFromTable(actorName, '/api/commands/RemoveActorFromActorContainer');
  }

  async addAdditionalScenario(scenarioName: string): Promise<void> {
    await this.addViaSelector('Add Scenario', 'Select Scenario', scenarioName, '/api/commands/AddScenarioToUseCase');
  }

  async removeAdditionalScenario(scenarioName: string): Promise<void> {
    await this.removeFromTable(scenarioName, '/api/commands/RemoveScenarioFromUseCase');
  }

  async expectInTable(name: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: name }).first()).toBeVisible();
  }

  async expectNotInTable(name: string): Promise<void> {
    await expect(this.page.locator('p-table td', { hasText: name }).first()).not.toBeVisible();
  }

  // ── Primary scenario helpers ──

  async openPrimaryScenarioInEditor(): Promise<void> {
    await this.page.getByRole('button', { name: 'Open in Editor' }).click();
    await this.page.waitForURL(/\/scenarios\/\d+/);
  }

  async expectPrimaryScenarioName(name: string): Promise<void> {
    await expect(this.page.locator('.primary-scenario-name')).toContainText(name);
  }
}
