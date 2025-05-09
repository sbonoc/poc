import * as allure from 'allure-js-commons'
import { test, expect } from '@playwright/test';

// See here how to get started:
// https://playwright.dev/docs/intro
test('visits the app root url', async ({ page }) => {
  await allure.parentSuite('E2E Tests')
  await allure.suite('UI Tests')
  await allure.subSuite('Home Page')

  await allure.step('renders properly', async () => {

    await allure.description(
      'This test verifies that the Home Page renders Pulse: 1 when Pulse is 1 from the API',
    )
    await allure.severity('critical')
    await allure.feature('Home Page')
    await allure.story('Renders Pulse: 1 when Pulse is 1 from the API')

    await page.goto('/');
    await expect(page.locator('h1')).toHaveText('You did it!');
    await expect(page.locator('h2')).toHaveText('Pulse: 1');
  })


})

test('visits the app root url given pulse is 2', async ({ page }) => {
  await allure.parentSuite('E2E Tests')
  await allure.suite('UI Tests')
  await allure.subSuite('Home Page')

  await allure.step('renders properly', async () => {
    await allure.description(
      'This test verifies that the Home Page renders Pulse: 2 when Pulse is 2 from the API',
    )
    await allure.severity('critical')
    await allure.feature('Home Page')
    await allure.story('Renders Pulse: 2 when Pulse is 2 from the API')
    await page.setExtraHTTPHeaders({
      'X-Pact-Provider-State': 'Pulse is 2',
    });
    await page.goto('/');
    await expect(page.locator('h1')).toHaveText('You did it!');
    await expect(page.locator('h2')).toHaveText('Pulse: 2');
  })
})
