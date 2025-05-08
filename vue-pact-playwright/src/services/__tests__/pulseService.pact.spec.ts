import * as allure from 'allure-js-commons'
import { PactV4, MatchersV3, SpecificationVersion } from '@pact-foundation/pact'
import path from 'path'
import { expect, test } from 'vitest'
import { getPulse } from '@/services/pulseService'
import type { Pulse } from '@/types/pulse'

const provider = new PactV4({
  dir: path.resolve(process.cwd(), 'pacts'),
  consumer: 'superapp-ui',
  provider: 'superapp-api',
  spec: SpecificationVersion.SPECIFICATION_VERSION_V4,
})

test('Pulse API', async () => {
  await allure.parentSuite('Integration Tests')
  await allure.suite('Contract Tests')
  await allure.subSuite('Pulse API')

  await allure.step('gets pulse when Pulse is 1', async () => {
    await allure.description(
      'This test verifies that the Pulse API returns the correct pulse value when Pulse is 1',
    )
    await allure.severity('critical')

    const pulseExample: Pulse = { pulse: 1 }
    const EXPECTED_BODY = MatchersV3.like(pulseExample)
    const interaction = provider
      .addInteraction()
      .given('Pulse is 1')
      .uponReceiving('a request for Pulse')
      .withRequest('GET', '/api/pulse', (builder) => {
        builder.query({ from: 'today' }).headers({ Accept: 'application/json' })
      })
      .willRespondWith(200, (builder) => {
        builder.headers({ 'Content-Type': 'application/json' }).jsonBody(EXPECTED_BODY)
      })

    return interaction.executeTest(async (mockserver) => {
      if (!mockserver.url) {
        throw new Error('Mock server URL is undefined')
      }

      try {
        const response = await getPulse(mockserver.url, 'today')
        expect(response).toBeDefined()
        expect(response).toBeTypeOf('object')
        expect(Object.keys(response).length).toBeGreaterThan(0)
        expect(response).toEqual(pulseExample)
      } catch (error) {
        throw new Error(`API call failed: ${error}`)
      }
    })
  })

  await allure.step('gets pulse when Pulse is 2', async () => {
    await allure.description(
      'This test verifies that the Pulse API returns the correct pulse value when Pulse is 2',
    )
    await allure.severity('critical')
    const pulseExample: Pulse = { pulse: 2 }
    const EXPECTED_BODY = MatchersV3.like(pulseExample)
    const interaction = provider
      .addInteraction()
      .given('Pulse is 2')
      .uponReceiving('a request for Pulse')
      .withRequest('GET', '/api/pulse', (builder) => {
        builder.query({ from: 'today' }).headers({ Accept: 'application/json' })
      })
      .willRespondWith(200, (builder) => {
        builder.headers({ 'Content-Type': 'application/json' }).jsonBody(EXPECTED_BODY)
      })

    return interaction.executeTest(async (mockserver) => {
      if (!mockserver.url) {
        throw new Error('Mock server URL is undefined')
      }

      try {
        const response = await getPulse(mockserver.url, 'today')
        expect(response).toBeDefined()
        expect(response).toBeTypeOf('object')
        expect(Object.keys(response).length).toBeGreaterThan(0)
        expect(response).toEqual(pulseExample)
      } catch (error) {
        throw new Error(`API call failed: ${error}`)
      }
    })
  })
})
