import { test as base, expect, Browser, BrowserContext, Page } from '@playwright/test';
import { addCoverageReport } from 'monocart-reporter';
import * as path from 'path';

export { expect };

type AuthFixtures = {
  adminContext: BrowserContext;
  projectContext: BrowserContext;
  _coverage: void;
};

async function stopCoverageForPage(page: Page, testInfo: ReturnType<typeof test.info>, trackedPages: WeakSet<Page>): Promise<void> {
  if (!trackedPages.has(page) || page.isClosed()) {
    return;
  }
  trackedPages.delete(page);
  const coverage = await page.coverage.stopJSCoverage();
  if (Array.isArray(coverage)) {
    await addCoverageReport(coverage, testInfo);
  }
}

function instrumentContext(ctx: BrowserContext, testInfo: ReturnType<typeof test.info>): void {
  const trackedPages = new WeakSet<Page>();
  const livePages = new Set<Page>();

  const startCoverage = async (page: Page): Promise<void> => {
    if (trackedPages.has(page)) {
      return;
    }
    await page.coverage.startJSCoverage({ resetOnNavigation: false });
    trackedPages.add(page);
    livePages.add(page);

    const originalClose = page.close.bind(page);
    page.close = async (...args: Parameters<Page['close']>) => {
      try {
        await stopCoverageForPage(page, testInfo, trackedPages);
      } finally {
        livePages.delete(page);
      }
      return originalClose(...args);
    };
  };

  ctx.on('page', page => {
    if (process.env['COVERAGE_ENABLED'] === 'true') {
      void startCoverage(page);
    }
  });

  const originalClose = ctx.close.bind(ctx);
  ctx.close = async (...args: Parameters<BrowserContext['close']>) => {
    if (process.env['COVERAGE_ENABLED'] === 'true') {
      for (const page of [...livePages]) {
        if (!page.isClosed()) {
          await stopCoverageForPage(page, testInfo, trackedPages);
        }
        livePages.delete(page);
      }
    }
    return originalClose(...args);
  };
}

export const test = base.extend<AuthFixtures>({
  // Collect Chromium V8 coverage per test and feed it to monocart-reporter.
  // Only active when COVERAGE_ENABLED=true (set by playwright.coverage.config.ts).
  // Coverage must be attached to the actual pages created by browser.newContext()
  // and admin/project contexts, not Playwright's unused default page fixture.
  _coverage: [
    async ({ browser }, use) => {
      const originalNewContext = browser.newContext.bind(browser);
      browser.newContext = async (...args: Parameters<Browser['newContext']>) => {
        const ctx = await originalNewContext(...args);
        if (process.env['COVERAGE_ENABLED'] === 'true') {
          instrumentContext(ctx, test.info());
        }
        return ctx;
      };

      try {
        await use();
      } finally {
        browser.newContext = originalNewContext;
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
