import { Locator, Page, expect } from '@playwright/test';

/**
 * Page object for the global app shell sidebar (`app-sidebar-nav`).
 *
 * The sidebar is rendered on every authenticated route, so callers should
 * ensure the user is already logged in (e.g. via `adminContext.newPage()`)
 * and on a route that mounts the app shell before driving any of these
 * helpers. The Import button + file input are gated on `canCreateProjects()`
 * — admins always see them; restricted users may not.
 */
export class SidebarPage {
  constructor(private readonly page: Page) {}

  /** The whole sidebar tree. Useful for scoping locators to project nodes. */
  tree(): Locator {
    return this.page.getByTestId('sidebar-tree');
  }

  /**
   * Drive the sidebar's hidden `<input type="file">` directly, bypassing the
   * (visible) Import button + invisible file picker. Mirrors the pattern used
   * by `ProjectsPage.importProjectFromFile()` for the project-list import.
   */
  async importProjectFromFile(filePath: string): Promise<void> {
    await this.page.getByTestId('sidebar-import-input').setInputFiles(filePath);
  }

  /**
   * Same as `importProjectFromFile` but feeds the bytes directly via
   * Playwright's in-memory `{ name, mimeType, buffer }` form. Lets tests
   * avoid touching the filesystem (and the `node:fs` types) when the bytes
   * already exist in memory — e.g. as the response from an authenticated
   * export API call.
   */
  async importProjectFromBytes(name: string, xml: string): Promise<void> {
    await this.page.getByTestId('sidebar-import-input').setInputFiles({
      name,
      mimeType: 'text/xml',
      buffer: Buffer.from(xml, 'utf-8'),
    });
  }

  /** Locator for a project node in the sidebar tree, matched by exact name. */
  projectNode(name: string): Locator {
    return this.tree().getByRole('treeitem').filter({
      has: this.page.getByText(name, { exact: true }),
    });
  }

  /** Wait for the named project to appear as a node in the sidebar tree. */
  async expectProjectInTree(name: string, timeout = 10_000): Promise<void> {
    await expect(this.projectNode(name)).toBeVisible({ timeout });
  }

  /**
   * Expand a project node so its entity-group children render in the DOM.
   *
   * PrimeNG's p-tree only renders child treeitems when their parent is
   * expanded — so this is required before any `expectEntityGroup()` call.
   * The toggler is the first `<button>` inside the project's treeitem
   * (the chevron toggle button); clicking it flips `aria-expanded`.
   */
  async expandProject(name: string): Promise<void> {
    const node = this.projectNode(name);
    await expect(node).toBeVisible();
    await node.locator('button').first().click();
    await expect(node).toHaveAttribute('aria-expanded', 'true');
  }

  /**
   * Assert that a project node is currently expanded WITHOUT toggling it.
   * Use this to verify the persisted expand state is being applied — e.g.
   * across an SSE-driven tree rebuild or a page reload — where calling
   * `expandProject()` would flip an already-open node back to collapsed.
   */
  async expectProjectExpanded(name: string, timeout = 5_000): Promise<void> {
    const node = this.projectNode(name);
    await expect(node).toHaveAttribute('aria-expanded', 'true', { timeout });
  }

  /**
   * Assert that an entity-group child label is rendered under the named
   * project. The sidebar formats counts inline (e.g. `Goals (1)`), so
   * `groupLabelPattern` should be a regex that tolerates the count tail —
   * e.g. `/^Goals \(\d+\)$/`.
   */
  async expectEntityGroup(projectName: string, groupLabelPattern: RegExp): Promise<void> {
    const node = this.projectNode(projectName);
    await expect(node.getByRole('treeitem').filter({ hasText: groupLabelPattern }).first())
      .toBeVisible();
  }
}
