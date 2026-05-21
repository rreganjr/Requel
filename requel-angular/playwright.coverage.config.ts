// Tell the coverage fixture in fixtures/auth.ts to start/stop V8 coverage per test.
process.env['COVERAGE_ENABLED'] = 'true';

// Coverage-specific Playwright config. Use for E2E coverage runs only:
//   npx playwright test --config=playwright.coverage.config.ts
//
// Identical to playwright.config.ts except it adds monocart-reporter to
// collect V8 JavaScript coverage from Chromium and map it back to TypeScript
// source via Angular's source maps.
//
// Normal dev runs should use playwright.config.ts (no coverage overhead).
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: false,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  workers: 1,
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['junit', { outputFile: './test-results/playwright.junit.xml' }],
    [
      'monocart-reporter',
      {
        name: 'Requel E2E Coverage',
        outputFile: './playwright-report/e2e-coverage.html',
        coverage: {
          // Collect coverage only for JS served by the app (not Playwright internals)
          entryFilter: (entry: { url: string }) =>
            entry.url.startsWith('http://localhost:8080/') &&
            entry.url.endsWith('.js'),
          // After source-map resolution, show only app source (not node_modules/vendor)
          sourceFilter: (sourcePath: string) =>
            sourcePath.startsWith('src/app/'),
          reports: ['console-summary', 'v8', 'html', 'lcovonly'],
          outputDir: './coverage',
        },
      },
    ],
  ],
  globalSetup: './e2e/global-setup.ts',
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
