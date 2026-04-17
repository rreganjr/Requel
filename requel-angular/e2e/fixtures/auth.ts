import { test as base, expect, BrowserContext } from '@playwright/test';
import * as path from 'path';

export { expect };

type AuthFixtures = {
  adminContext: BrowserContext;
  projectContext: BrowserContext;
};

export const test = base.extend<AuthFixtures>({
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
