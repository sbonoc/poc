import { test, expect } from '@playwright/test';

// See here how to get started:
// https://playwright.dev/docs/intro
test('visits the app root url', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('h1')).toHaveText('You did it!');
  await expect(page.locator('h2')).toHaveText('Pulse: 1');
})

test('visits the app root url given pulse is 2', async ({ page }) => {
  await page.setExtraHTTPHeaders({
    'X-Pact-Provider-State': 'Pulse is 2',
  });
  await page.goto('/');
  await expect(page.locator('h1')).toHaveText('You did it!');
  await expect(page.locator('h2')).toHaveText('Pulse: 2');
})
