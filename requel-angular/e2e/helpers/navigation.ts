import { Page, Response } from '@playwright/test';

export async function reloadAndWaitForGet(page: Page, predicate: (response: Response) => boolean): Promise<void> {
  const responseLoaded = page.waitForResponse(response =>
    response.status() === 200 && response.request().method() === 'GET' && predicate(response)
  );

  await page.reload({ waitUntil: 'domcontentloaded' });
  await responseLoaded;
}

/**
 * Navigate to `url` and wait for the entity GET that populates the page.
 *
 * `page.goto()` resolves on document load, which for this SPA is well before the editor's
 * detail fetch returns. A test that starts typing in that gap is racing the load: the fetch
 * lands afterwards and resets the form under it. Every assertion an editor route can satisfy
 * while still empty - element counts, a disabled Save on a pristine form - passes in that gap
 * too, so the race hides until the first assertion that needs real data.
 *
 * Use this instead of a bare `page.goto()` whenever the test interacts with a loaded editor.
 */
export async function gotoAndWaitForGet(
  page: Page,
  url: string,
  predicate: (response: Response) => boolean
): Promise<void> {
  const responseLoaded = page.waitForResponse(response =>
    response.status() === 200 && response.request().method() === 'GET' && predicate(response)
  );

  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await responseLoaded;
}
