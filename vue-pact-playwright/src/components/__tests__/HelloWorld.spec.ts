import { test, expect } from 'vitest'
import * as allure from 'allure-js-commons'

import { mount } from '@vue/test-utils'
import HelloWorld from '../HelloWorld.vue'

test('HelloWorld', async () => {
  await allure.parentSuite('Unit Tests')
  await allure.suite('Component Tests')
  await allure.subSuite('Helloworld.vue')
  allure.step('renders properly', async () => {
    await allure.description('This test verifies that the it shows 1 when Pulse property is 1')
    await allure.severity('critical')
    await allure.feature('HelloWorld.vue')
    await allure.story('Renders Pulse when Pulse property is 1')
    const wrapper = mount(HelloWorld, { props: { msg: 'Hello Vitest', pulse: { pulse: 1 } } })
    expect(wrapper.text()).toContain('Hello Vitest')
    expect(wrapper.text()).toContain('Pulse: 1')
  })
})
