import { Page, Response } from '@playwright/test';

export async function reloadAndWaitForGet(page: Page, predicate: (response: Response) => boolean): Promise<void> {
  const responseLoaded = page.waitForResponse(response =>
    response.status() === 200 && response.request().method() === 'GET' && predicate(response)
  );

  await page.reload({ waitUntil: 'domcontentloaded' });
  await responseLoaded;
}
