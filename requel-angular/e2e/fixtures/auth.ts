import { test as base, expect, BrowserContext } from '@playwright/test';
import { addCoverageReport } from 'monocart-reporter';
import * as path from 'path';

export { expect };

type AuthFixtures = {
  adminContext: BrowserContext;
  projectContext: BrowserContext;
  _coverage: void;
};

export const test = base.extend<AuthFixtures>({
  // Collect Chromium V8 coverage per test and feed it to monocart-reporter.
  // Only active when COVERAGE_ENABLED=true (set by playwright.coverage.config.ts)
  // so normal dev-time runs are unaffected.
  _coverage: [
    async ({ page }, use) => {
      if (process.env['COVERAGE_ENABLED'] === 'true') {
        await page.coverage.startJSCoverage({ resetOnNavigation: false });
      }
      await use();
      if (process.env['COVERAGE_ENABLED'] === 'true') {
        const coverage = await page.coverage.stopJSCoverage();
        await addCoverageReport(coverage, test.info());
      }
    },
    { auto: true },
  ],

  adminContext: async ({ browser }, use) => {
    const ctx = await browser.newContext({
      storageState: path.join(__dirname, '../.auth/admin.json'),
    });
    await use(ctx);
    await ctx.close();
  },
  projectContext: async ({ browser }, use) => {
    const ctx = await browser.newContext({
      storageState: path.join(__dirname, '../.auth/project.json'),
    });
    await use(ctx);
    await ctx.close();
  },
});
