import { Page, expect } from '@playwright/test';

/**
 * The active step's key, or null once the wizard has unmounted.
 *
 * One DOM read with no auto-wait. It used to be count() then getAttribute(): when the last
 * Continue navigated away between the two calls, getAttribute() waited for a panel that no longer
 * existed and hung the caller's poll until its timeout, reported as "wizard did not leave step"
 * although the wizard had finished (#257 CI, scenario create).
 */
async function activeStepKey(page: Page): Promise<string | null> {
  const id = await page
    .locator('[data-testid^="wizard-panel-"]')
    .evaluateAll(panels => panels[0]?.getAttribute('data-testid') ?? null);
  return id ? id.replace('wizard-panel-', '') : null;
}

/**
 * Drives an `app-form-wizard` create flow to completion.
 *
 * #173 turned create into a wizard for project, actor, stakeholder, scenario and use case. The
 * Save button now exists only in edit mode; creating means pressing Continue on each step and
 * Done on the last, after which the host navigates and the wizard unmounts.
 *
 * Returns false when no wizard is on the page, so callers keep their edit-mode path unchanged
 * and one `save()` serves both flows.
 *
 * Clicks target the inner `<button>` rather than the `p-button` host: the host is a custom
 * element whose box can be intercepted by PrimeNG's toast overlay, which appears right after
 * the step-1 save and made an earlier version of this helper hang until the test timeout.
 *
 * Progress is asserted per step - after each click the active step must change or the wizard
 * must unmount - so a step that refuses to advance fails naming itself instead of producing a
 * 30s "target closed" with no indication of where it stalled.
 *
 * @param commandUrl when given, the first Continue is awaited against this command's response
 *                   and a non-2xx or `success: false` throws, so a failed create reports the
 *                   server's message rather than failing later on a missing row.
 */
export async function completeCreateWizard(page: Page, commandUrl?: RegExp): Promise<boolean> {
  const continueBtn = page.getByTestId('wizard-continue').locator('button');
  if ((await page.getByTestId('wizard-continue').count()) === 0) {
    return false;
  }

  let key = await activeStepKey(page);

  await expect(continueBtn).toBeEnabled();
  if (commandUrl) {
    const [response] = await Promise.all([
      page.waitForResponse(r => commandUrl.test(r.url())),
      continueBtn.click({ timeout: 10_000 }),
    ]);
    if (!response.ok()) {
      throw new Error(`${response.url()} failed: ${response.status()} ${await response.text()}`);
    }
    const body = (await response.json()) as { success?: boolean; error?: string };
    if (body.success === false) {
      throw new Error(`create failed on step "${key}": ${body.error ?? 'success=false'}`);
    }
  } else {
    await continueBtn.click({ timeout: 10_000 });
  }

  // Step 1 is the only one that talks to the API; the association steps commit through their
  // own widgets, so their Continue just advances. Loop until the wizard unmounts on finish.
  for (let i = 0; i < 6; i++) {
    const previous = key;
    await expect
      .poll(() => activeStepKey(page), {
        timeout: 15_000,
        message: `wizard did not leave step "${previous}" after pressing Continue`,
      })
      .not.toBe(previous);

    key = await activeStepKey(page);
    if (key === null) {
      // Wizard gone: the host navigated on finish, which is the success exit.
      await page.waitForLoadState('domcontentloaded');
      return true;
    }

    await expect(continueBtn).toBeEnabled();
    await continueBtn.click({ timeout: 10_000 });
  }

  throw new Error(`wizard still on step "${key}" after 6 Continue presses`);
}
