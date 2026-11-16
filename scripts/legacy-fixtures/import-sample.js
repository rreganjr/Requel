// #380: drive an old Requel (1.0.x / 1.1.0, Echo2 UI) to put a sample project in its database,
// so scripts/legacy-fixtures.sh can dump rows written by the old code itself.
//   node import-sample.js <url> <project-xml>
// Grants admin the ProjectUserRole (+createProjects), as the sample's creator is admin, then
// imports the file as the "project" user with analysis off (its annotations vary from run to run).
// Needs Playwright with Chromium (NODE_PATH pointing at a node_modules that has it).
const { chromium } = require('playwright');

async function signIn(browser, url, user, pass) {
  const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });
  await page.goto(url);
  await page.waitForTimeout(3000);
  const inputs = page.locator('input');
  await inputs.nth(0).click();
  await inputs.nth(0).pressSequentially(user);
  await inputs.nth(1).click();
  await inputs.nth(1).pressSequentially(pass);
  await page.getByText('Login', { exact: true }).click();
  await page.waitForTimeout(4000);
  return page;
}

/** Echo2 renders tree checkboxes as images; click the one on the row with this label. */
async function tickTreeRow(page, label) {
  const box = await page.getByText(label, { exact: true }).boundingBox();
  if (!box) throw new Error(`no row "${label}"`);
  await page.mouse.click(box.x - 12, box.y + box.height / 2);
  await page.waitForTimeout(1500);
}

(async () => {
  const [url, file] = process.argv.slice(2);
  const browser = await chromium.launch();
  try {
    const admin = await signIn(browser, url, 'admin', 'admin');
    // Only a user with ProjectUserRole gets a Projects tab; skip the grant if admin already has it
    // (ticking the box again would take the role away).
    if (await admin.getByText('Projects', { exact: true }).count() === 0) {
      await admin.getByText('admin', { exact: true }).waitFor({ timeout: 30000 });
      await admin.getByText('admin', { exact: true }).click();
      await admin.getByText('ProjectUserRole', { exact: true }).waitFor({ timeout: 30000 });
      await admin.waitForTimeout(1000);
      await tickTreeRow(admin, 'ProjectUserRole');
      await tickTreeRow(admin, 'createProjects');
      await admin.getByText('Save', { exact: true }).click();
      await admin.waitForTimeout(4000);
    }
    await admin.close();

    const page = await signIn(browser, url, 'project', 'project');
    await page.getByText('Import Project', { exact: true }).waitFor({ timeout: 30000 });
    await page.getByText('Import Project', { exact: true }).click();
    await page.getByText('Enable Analysis:', { exact: true }).waitFor({ timeout: 30000 });
    // The sample's <name> comes after <actors>, which 1.1.0's importer misses (it NPEs on a null
    // project name), so give the name explicitly.
    await page.locator('input[type=text]').first().fill('Requel');
    const frame = page.frameLocator('iframe');
    await frame.locator('input[type=file]').setInputFiles(file);
    await frame.locator('input[name=sendButton]').click();
    await page.waitForTimeout(5000);
    // "Enable Analysis" is on by default; the checkbox sits to the right of its label.
    const label = await page.getByText('Enable Analysis:', { exact: true }).boundingBox();
    await page.mouse.click(label.x + label.width + 30, label.y + label.height / 2);
    await page.waitForTimeout(1500);
    await page.getByText('Save', { exact: true }).click();
    await page.waitForTimeout(20000);
    await page.screenshot({ path: require('path').join(require('os').tmpdir(), 'requel-legacy-import.png') });
  } finally {
    await browser.close();
  }
})();
