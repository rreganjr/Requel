import { Download, Page, expect } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

export class ProjectsPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  async goto(): Promise<void> {
    await this.gotoList('/projects', '/api/projects');
  }

  /** Filter the table so only rows matching name are visible — handles pagination. */
  async searchFor(name: string): Promise<void> {
    await this.searchWithPlaceholder('Search projects...', name);
  }

  async expectProjectInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowVisible(name);
  }

  async expectProjectNotInTable(name: string): Promise<void> {
    await this.searchFor(name);
    await this.expectTableRowNotVisible(name);
  }

  async clickNewProject(): Promise<void> {
    await this.page.getByTestId('project-list-new-project').click();
    await this.page.waitForURL('**/projects/new');
  }

  async clickProject(name: string): Promise<void> {
    await this.searchFor(name);
    await this.clickTableRow(name, `**/projects/${encodeURIComponent(name)}`);
  }

  async expectCreateActionsVisible(): Promise<void> {
    await expect(this.page.getByTestId('project-list-new-project')).toBeVisible();
    await expect(this.page.getByTestId('project-list-import-button')).toBeVisible();
  }

  async expectCreateActionsHidden(): Promise<void> {
    await expect(this.page.getByTestId('project-list-new-project')).toHaveCount(0);
    await expect(this.page.getByTestId('project-list-import-button')).toHaveCount(0);
  }

  async importProjectFromFile(filePath: string): Promise<void> {
    await this.page.getByTestId('project-list-import-input').setInputFiles(filePath);
  }

  async clearImportSelection(): Promise<void> {
    await this.page.getByTestId('project-list-import-input').dispatchEvent('change');
  }

  async expectImportSuccess(message = 'Project imported successfully.'): Promise<void> {
    await expect(this.page.getByTestId('project-list-success')).toContainText(message);
  }

  async expectImportError(message: string): Promise<void> {
    await expect(this.page.getByTestId('project-list-error')).toContainText(message);
  }

  async expectImportWarning(message: string): Promise<void> {
    await expect(this.page.getByTestId('project-list-warning')).toContainText(message);
  }

  async expectNoImportMessages(): Promise<void> {
    await expect(this.page.getByTestId('project-list-success')).toHaveCount(0);
    await expect(this.page.getByTestId('project-list-error')).toHaveCount(0);
    await expect(this.page.getByTestId('project-list-warning')).toHaveCount(0);
  }

  async expectNoProjectsMessage(): Promise<void> {
    await expect(this.page.getByTestId('project-list-empty')).toContainText('No projects found.');
  }
}

export class ProjectEditorPage {
  constructor(private page: Page) {}

  async waitForLoad(expectedName: string): Promise<void> {
    await expect(this.page.locator('#name')).toHaveValue(expectedName);
    // drain the setTimeout(markAsPristine) that fires after populateForm
    await this.page.evaluate(() => new Promise<void>(resolve => setTimeout(resolve, 0)));
  }

  async fillName(name: string): Promise<void> {
    const input = this.page.locator('#name');
    await input.clear();
    await input.fill(name);
  }

  async fillDescription(text: string): Promise<void> {
    const ta = this.page.locator('#description');
    await ta.clear();
    await ta.fill(text);
  }

  async save(): Promise<void> {
    await this.page.getByTestId('project-save').click();
    // wait for navigation away from /new or for save to complete
    await this.page.waitForLoadState('domcontentloaded');
  }

  async expectError(message: string): Promise<void> {
    await expect(this.page.locator('app-project-editor p-message[severity="error"]'))
      .toContainText(message);
  }

  async expectAnyError(): Promise<void> {
    await expect(this.page.locator('app-project-editor p-message[severity="error"]'))
      .toBeVisible({ timeout: 5000 });
  }

  async cancel(): Promise<void> {
    await this.page.getByTestId('project-cancel').click();
  }

  /**
   * Click the Export button and return the resulting browser download.
   * The Angular UI calls window.open(exportUrl, '_blank'), and the export
   * endpoint sends Content-Disposition: attachment, so the browser fires a
   * download event we can capture.
   */
  async clickExportAndCaptureDownload(): Promise<Download> {
    const downloadPromise = this.page.waitForEvent('download');
    await this.page.getByTestId('project-export').click();
    return await downloadPromise;
  }

  async expectHeaderContains(text: string): Promise<void> {
    await expect(this.page.locator('h2, .page-title, .editor-title')).toContainText(text);
  }

  async delete(): Promise<void> {
    await this.page.getByRole('button', { name: 'Delete' }).click();
    // PrimeNG confirmation dialog — click Accept
    await this.page.getByRole('button', { name: 'Yes' }).click();
    await this.page.waitForURL('**/projects');
  }
}
