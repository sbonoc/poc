# Vue 3 + Pact + Playwright: Accelerating Test Automation

Table of contents:
- [Why Pact? Beyond Traditional Integration Tests](#why-pact-beyond-traditional-integration-tests)
- [Project Setup](#project-setup)
    - [Compile and Hot-Reload for Development](#compile-and-hot-reload-for-development)
    - [Run Unit Tests with Vitest](#run-unit-tests-with-vitest)
    - [Run End-to-End Tests with Playwright](#run-end-to-end-tests-with-playwright)
- [What I've done step-by-step](#what-ive-done-step-by-step)
    - [Step 1 - Create Vue app using Vue CLI](#step-1---create-vue-app-using-vue-cli)
    - [Step 2 - Add required packages for Pact and Axios](#step-2---add-required-packages-for-pact-and-axios)
    - [Step 3 & 4 - Implement API Client while defining its contract with Pact](#step-3--4---implement-api-client-while-defining-its-contract-with-pact)
    - [Step 5 - Generate Pact file](#step-5---generate-pact-file)
    - [Step 6 - Add Pulse in HelloWorld component](#step-6---add-pulse-in-helloworld-component)
    - [Step 7 - Create Dockerfile and Docker Compose](#step-7---create-dockerfile-and-docker-compose)
    - [Step 8 - Create Playwright test using Pact stub server](#step-8---create-playwright-test-using-pact-stub-server)
    - [Step 9 - Create Provider API in another project](#step-9---create-provider-api-in-another-project)

# Why Pact? Beyond Traditional Integration Tests

Still relying on traditional integration tests? Here's why Pact offers a superior approach for microservices:

**Traditional Integration Tests (Provider-Side):**
*   **Blind Spots**: Pass internal tests, but still break *actual* consumers in production. Integration issues found late, leading to costly fixes.
*   **Brittle & Slow**: Complex setup, tightly coupled, and often slow, making them hard to maintain and run frequently.

**Pact Contract Tests (Consumer-Driven):**
*   **Guaranteed Compatibility**: Consumers define their exact needs. Providers verify they meet these *real-world* expectations, ensuring true compatibility.
*   **Shift Left**: Catch integration bugs in CI/CD, not production. Faster feedback loops mean quicker fixes and less debugging.
*   **Independent Deployments**: Confidently deploy services knowing they won't break consumers, enabling true microservice autonomy.
*   **Clear Contracts**: Pact files are living, executable documentation of API agreements, fostering better team communication.
*   **Fast & Reliable**: Tests run in isolation with mocked dependencies, making them quick, stable, and easy to maintain.

**In short: Pact ensures your API works *for your consumers*, not just in isolation. It's about confidence, speed, and seamless microservice integration.**

## Project Setup

```sh
pnpm install
```

### Compile and Hot-Reload for Development

```sh
pnpm dev
```

### Run Unit and Contract (Pact) Tests with [Vitest](https://vitest.dev/)

```sh
pnpm test:unit
```

### Run End-to-End Tests with [Playwright](https://playwright.dev)

```sh
# Install browsers for the first run
npx playwright install

# When testing on CI, must build the project first
pnpm build

# Runs the end-to-end tests
pnpm test:e2e
# Runs the tests only on Chromium
pnpm test:e2e:chromium
```

## What I've done step-by-step

Shortly:

1. Created a Vue app using Vue CLI.
2. Added required packages for Pact and Axios.
3. Wrote a Pact test to define the `getPulse` API contract.
4. Built the API client for `getPulse` while defining its contract.
5. Executed the tests to generate the resulting Pact file.
6. Add Pulse in HelloWorld component.
7. Created a Dockerfile and docker compose to build and execute the Vue app and Pact stub server.
8. Created a Playwright test using Pact stub server and the provider state as a mock API.

### Step 1 - Create Vue app using Vue CLI

Just followed the instructions from the official [Vue's Quick Start](https://vuejs.org/guide/quick-start.html#creating-a-vue-application):

```sh
pnpm create vue@latest
```

Adding my project name, choosing `Yes` in all Yes/No prompts and Playwright as the end-to-end testing solution:

```
✔ Project name: pact-consumer-vue
✔ Add TypeScript? Yes
✔ Add JSX Support? Yes
✔ Add Vue Router for Single Page Application development? Yes
✔ Add Pinia for state management? Yes
✔ Add Vitest for Unit testing? Yes
✔ Add an End-to-End Testing Solution? Playwright
✔ Add ESLint for code quality? Yes
✔ Add Prettier for code formatting? Yes
✔ Add Vue DevTools 7 extension for debugging? (experimental) Yes

Scaffolding project in ./pact-consumer-vue...
Done.
```

### Step 2 - Add required packages for Pact and Axios

Basically:

```sh
pnpm add -D @pact-foundation/pact@latest
```
And:

```sh
pnpm add axios
```

### Step 3 & 4 - Implement API Client while defining its contract with Pact

I started elaborating the initial version of the Pact in `src/services/__tests__/pulseService.pact.spec.ts` for `getPulse`:

```js
import { PactV4, MatchersV3, SpecificationVersion } from '@pact-foundation/pact'
import axios from 'axios'
import path from 'path'
import { expect, test } from 'vitest'

const provider = new PactV4({
  dir: path.resolve(process.cwd(), 'pacts'),
  consumer: 'superapp-ui',
  provider: 'superapp-api',
  spec: SpecificationVersion.SPECIFICATION_VERSION_V4,
})

const pulseExample = { value: 1 }
const EXPECTED_BODY = MatchersV3.like(pulseExample)

test('gets pulse', async () => {
  const interaction = provider
    .addInteraction()
    .given('I have data in all stats')
    .uponReceiving('a request for all stats')
    .withRequest('GET', '/api/pulses', (builder) => {
      builder.query({ from: 'today' }).headers({ Accept: 'application/json' })
    })
    .willRespondWith(200, (builder) => {
      builder.headers({ 'Content-Type': 'application/json' }).jsonBody(EXPECTED_BODY)
    })

  return interaction.executeTest(async (mockserver) => {
    if (!mockserver.url) {
      throw new Error('Mock server URL is undefined')
    }

    const apiClient = axios.create({
      timeout: 5000,
      headers: { 'Content-Type': 'application/json' },
    })
    const response = await apiClient.request({
      baseURL: mockserver.url,
      params: { from: 'today' },
      headers: { Accept: 'application/json' },
      method: 'GET',
      url: '/api/pulses',
    })

    expect(response).toBeDefined()
    expect(response.data).toBeTypeOf('object')
    expect(Object.keys(response.data).length).toBeGreaterThan(0)
    expect(response.data).toEqual(pulseExample)
  })
})

```

After having this primitive version, I elaborated a bit more, refactoring, etc... Until I end up with the following files:
* `src/services/apiClient.ts` which contains the basic Axios setup for any API Client.
* `src/types/pulse.ts` the interface for the object expected from `getPulse`.
* `src/services/pulseService.ts` having all the endpoints for the Pulse Service like `getPulse` with the response strong-typed with the expected interface.
* The new version of `src/services/__tests__/pulseService.pact.spec.ts` after the refactor.

### Step 5 - Generate Pact file

Just executed `pnpm run test:unit` and the output of Pact was written in `pacts/superapp-ui-superapp.api.json`.

### Step 6 - Add Pulse in HelloWorld component

First I added the following assertion in the component test of `src/components/__tests__/HelloWorld.spec.ts`:

```js
expect(wrapper.text()).toContain('Pulse: 1')
```
This failed in purpose because I didn't changed the component yet, following TDD.

Then I changed the component in `src/components/HelloWorld.vue`.

First added the property `pulse`
```js
defineProps<{
  msg: string,
  pulse: Pulse
}>()
```
Then, added the following in the template:
```js
<h2>Pulse: {{ pulse.pulse }}</h2>
```

### Step 7 - Create Dockerfile and Docker Compose

Created a `Dockerfile` to build Vue app and Pact stub server. Then, defined a `docker-compose.yml` file to orchestrate the services easily.

The Pact stub server is configured to use the custom header `X-Pact-Provider-State`.

The provider state is used to tell Pact to send out different response for the same request, it simulates that the API data is different, i.e. that the Pulse is 2 instead of 1.

This means that if the request contains the header `X-Pact-Provider-State: Pulse is 2`, Pact stub server will send back the response that was defined with `given('Pulse is 2')` in the contract code.

Run the services with:

```sh
docker compose up --build
```

### Step 8 - Create Playwright test using Pact stub server

Wrote an E2E test in `e2e/vue.spec.ts` to verify the UI and using different provider states of the Pact stub server.

```js
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

```

### Step 9 - Create Provider API in another project

Once the Consumer part was done, I started the Provider's part, the actual API to be consumed, in a separate project, [click on this link to continue the story](https://github.com/sbonoc/poc/tree/master/pact-provider-ktor).
