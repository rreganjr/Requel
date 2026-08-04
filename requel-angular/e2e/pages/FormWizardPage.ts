import { Page, expect } from '@playwright/test';

/**
 * Page object for the shared `app-form-wizard` (issue #158).
 *
 * Used by the create flows of the migrated editors (Goal, Story). The wizard's own
 * `data-testid`s are stable: `wizard-step-<key>` on each nav button,
 * `wizard-panel-<key>` on the active panel heading, plus `wizard-continue`,
 * `wizard-cancel` and `wizard-error`.
 *
 * Note the nav buttons carry their testid directly (the nav is hand-rolled), while
 * Continue/Cancel are PrimeNG `p-button`s whose testid lands on the host — hence the
 * extra `button` descendant for those two.
 */
export class FormWizardPage {
  constructor(private page: Page) {}

  root() {
    return this.page.locator('app-form-wizard');
  }

  /** Whether the wizard is on screen at all — i.e. we are on a create route. */
  async isPresent(): Promise<boolean> {
    return (await this.root().count()) > 0;
  }

  stepButton(key: string) {
    return this.page.getByTestId(`wizard-step-${key}`);
  }

  panelHeading(key: string) {
    return this.page.getByTestId(`wizard-panel-${key}`);
  }

  continueButton() {
    return this.page.getByTestId('wizard-continue').locator('button');
  }

  cancelButton() {
    return this.page.getByTestId('wizard-cancel').locator('button');
  }

  error() {
    return this.page.getByTestId('wizard-error');
  }

  async expectActiveStep(key: string): Promise<void> {
    await expect(this.panelHeading(key)).toBeVisible();
    await expect(this.stepButton(key)).toHaveAttribute('aria-current', 'step');
  }

  /** A step that cannot be reached yet: marked aria-disabled, but still focusable. */
  async expectStepLocked(key: string): Promise<void> {
    await expect(this.stepButton(key)).toHaveAttribute('aria-disabled', 'true');
  }

  async expectStepUnlocked(key: string): Promise<void> {
    await expect(this.stepButton(key)).not.toHaveAttribute('aria-disabled', 'true');
  }

  async expectStepComplete(key: string): Promise<void> {
    await expect(this.stepButton(key)).toHaveClass(/is-complete/);
  }

  async expectContinueEnabled(): Promise<void> {
    await expect(this.continueButton()).toBeEnabled();
  }

  async expectContinueDisabled(): Promise<void> {
    await expect(this.continueButton()).toBeDisabled();
  }

  /** The Continue button reads "Done" on the last step. */
  async expectContinueLabel(label: 'Continue' | 'Done'): Promise<void> {
    await expect(this.continueButton()).toContainText(label);
  }

  /**
   * Press Continue on a step that commits to the API, and assert the command
   * succeeded. `commandName` is the command type, e.g. `EditGoal`.
   */
  async commitStep(commandName: string): Promise<void> {
    const [response] = await Promise.all([
      this.page.waitForResponse(r => r.url().includes(`/api/commands/${commandName}`)),
      this.continueButton().click(),
    ]);
    if (!response.ok()) {
      throw new Error(`${commandName} failed: ${response.status()} ${await response.text()}`);
    }
  }

  /**
   * Press Continue on a step that commits nothing (the optional enrichment steps),
   * and wait for the next step's panel to take over.
   */
  async skipToStep(nextKey: string): Promise<void> {
    await this.continueButton().click();
    await this.expectActiveStep(nextKey);
  }

  /** Press Done on the last step and wait for the resulting navigation. */
  async finish(urlPattern: RegExp): Promise<void> {
    await this.continueButton().click();
    await this.page.waitForURL(urlPattern);
  }

  /** Click a step in the nav (only works for reachable steps). */
  async gotoStep(key: string): Promise<void> {
    await this.stepButton(key).click();
    await this.expectActiveStep(key);
  }

  async cancel(urlPattern: string | RegExp): Promise<void> {
    await this.cancelButton().click();
    await this.page.waitForURL(urlPattern);
  }

  /** Keyboard nav: focus a step button and arrow to the next/previous one. */
  async focusStep(key: string): Promise<void> {
    await this.stepButton(key).focus();
  }

  async pressInNav(key: string): Promise<void> {
    await this.page.keyboard.press(key);
  }
}
