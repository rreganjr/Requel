// @ts-check
import { defineConfig, devices } from '@playwright/test';

/**
 * #519: the UI review walk. Visits every route in light and dark, wide and narrow, runs axe on
 * each page and writes screenshots plus a summary to ui-review/ (gitignored). Not part of the
 * e2e suite or CI: the e2e config only matches *.e2e.ts. Needs the app running locally with the
 * dev profile, like the e2e suite. Run with `npm run ui-review`.
 */
export default defineConfig({
  testDir: './e2e/ui-review',
  testMatch: '**/*.walk.ts',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 120_000,
  reporter: 'list',
  outputDir: './ui-review/test-results',
  globalSetup: './e2e/global-setup.ts',
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'off',
    screenshot: 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
