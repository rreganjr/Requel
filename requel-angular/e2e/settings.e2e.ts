import { test, expect } from './fixtures/auth';
import { getPreferences, savePreferences, PreferencesFixture } from './fixtures/api-helper';
import { SettingsPage } from './pages/SettingsPage';

let originalPrefs: PreferencesFixture | null = null;

test.beforeEach(async ({ request }) => {
  // Snapshot the admin user's current preferences so afterEach can restore them.
  originalPrefs = await getPreferences(request);
});

test.afterEach(async ({ request }) => {
  if (originalPrefs) {
    try { await savePreferences(request, originalPrefs); } catch { /* ignore */ }
    originalPrefs = null;
  }
});

test.describe('Settings / preferences', () => {

  test('change sidebar project limit → persists after reload', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const settings = new SettingsPage(page);

    await settings.goto();
    await settings.fillProjectLimit(7);
    await settings.save();

    // Register listener before reload so we don't miss the Angular bootstrap GET.
    // waitUntil:'domcontentloaded' returns before Angular scripts run, guaranteeing
    // the listener is active before Angular fires its GET /user-preferences.
    const prefsLoaded = page.waitForResponse(
      r => r.url().includes('/user-preferences') && r.status() === 200 && r.request().method() === 'GET'
    );
    await page.reload({ waitUntil: 'domcontentloaded' });
    await prefsLoaded;

    await expect(page.locator('p-inputnumber input')).toHaveValue('7', { timeout: 10000 });

    await page.close();
  });

  test('change staleness threshold → persists after reload', async ({ adminContext }) => {
    const page = await adminContext.newPage();
    const settings = new SettingsPage(page);

    await settings.goto();
    await settings.selectStaleness('6 Months');
    await settings.save();

    const prefsLoaded = page.waitForResponse(
      r => r.url().includes('/user-preferences') && r.status() === 200 && r.request().method() === 'GET'
    );
    await page.reload({ waitUntil: 'domcontentloaded' });
    await prefsLoaded;

    await expect(page.locator('p-select')).toContainText('6 Months', { timeout: 10000 });

    await page.close();
  });

});
