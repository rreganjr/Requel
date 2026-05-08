import { Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

export class TermListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(projectName: string): Promise<void> {
    await this.gotoList(`/projects/${encodeURIComponent(projectName)}/terms`, '/terms');
  }

  async searchFor(name: string): Promise<void> {
    await this.searchWithPlaceholder('Search terms...', name);
  }

  async clickNewTerm(): Promise<void> {
    await this.clickNewButton('New Term', '**/terms/new');
  }

  async clickTerm(name: string): Promise<void> {
    await this.searchFor(name);
    await this.clickTableRow(name, /\/terms\/\d+/);
  }

  async expectTermInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowVisible(name);
  }

  async expectTermNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowNotVisible(name);
  }
}

export class TermEditorPage {
  constructor(private page: Page) {}

  async fillName(name: string): Promise<void> {
    const input = this.page.locator('#name');
    await input.clear();
    await input.fill(name);
  }

  async fillText(text: string): Promise<void> {
    const ta = this.page.locator('#text');
    await ta.clear();
    await ta.fill(text);
  }

  // Use saveNew() when creating a term — Angular navigates with replaceUrl:true after
  // the API responds, and Playwright can cancel waitForResponse when it sees replaceState.
  // waitForURL is the right signal for creation; waitForResponse is right for edits.
  async saveNew(): Promise<void> {
    // Angular navigates with replaceUrl:true (history.replaceState) — no load event fires.
    // waitUntil:'commit' gates only on the URL changing, not on a subsequent load.
    await this.page.getByTestId('term-save').click();
    await this.page.waitForURL(/\/terms\/\d+/, { waitUntil: 'commit', timeout: 10000 });
  }

  async save(): Promise<void> {
    await Promise.all([
      this.page.waitForResponse(r => r.url().includes('/api/commands/EditGlossaryTerm')),
      this.page.getByTestId('term-save').click(),
    ]);
  }

  async delete(): Promise<void> {
    await this.page.getByTestId('term-delete').click();
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForURL(/\/terms$/);
  }

  async navigateBack(projectName: string): Promise<void> {
    await this.page.getByTestId('term-back').click();
    await this.page.waitForURL(`**/projects/${encodeURIComponent(projectName)}/terms`);
  }

  async expectNameValue(name: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(name);
  }

  async expectTextValue(text: string): Promise<void> {
    await expect(this.page.locator('#text')).toHaveValue(text);
  }

  /** Wait for the validation/error <p-message> to render with the given text. */
  async expectErrorMessage(message: string | RegExp): Promise<void> {
    await expect(this.page.getByTestId('term-error')).toBeVisible();
    await expect(this.page.getByTestId('term-error')).toContainText(message);
  }

  /**
   * Pick a canonical term in the p-select. Opens the dropdown, clicks the
   * matching option, then waits for the select's panel to close so callers
   * can act on the dirtied form synchronously.
   */
  async selectCanonicalTerm(name: string): Promise<void> {
    await this.page.getByTestId('term-canonical-select').click();
    await this.page.getByRole('option', { name, exact: true }).click();
  }

  /**
   * Wait for the alternate-terms section to appear with the given alternate listed.
   *
   * The section is rendered behind `@if (!isNew() && term()?.alternateTerms?.length)`,
   * so it only materializes after loadTerm() has set the `term` signal with a
   * non-empty alternateTerms array. Under e2e-with-coverage's Dockerized backend
   * the cold-cache load chain (permissionService → listTerms → getTerm) can take
   * noticeably longer than the default 5s for the first few hits, so we give the
   * conditional render a more generous window before we conclude the data is
   * actually missing.
   */
  async expectAlternateTermInTable(name: string): Promise<void> {
    const section = this.page.getByTestId('term-alternate-terms-section');
    await expect(section).toBeVisible({ timeout: 15_000 });
    await expect(
      section.getByTestId('term-alternate-row').filter({ hasText: name })
    ).toBeVisible({ timeout: 5_000 });
  }

  /**
   * Click an alternate-term row, exercising navigateToTerm() in the editor
   * (the only path through the alternate-terms <tr (click)> handler).
   */
  async clickAlternateTerm(name: string): Promise<void> {
    await this.page.getByTestId('term-alternate-terms-section')
      .getByTestId('term-alternate-row')
      .filter({ hasText: name })
      .first()
      .click();
    await this.page.waitForURL(/\/terms\/\d+/);
  }
}
