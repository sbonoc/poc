import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
      setupFiles: ['allure-vitest/setup'],
      reporters: ['default', ['allure-vitest/reporter', { resultsDir: 'allure-results' }], ['allure-playwright', { resultsDir: 'allure-results' }]]
    },
  }),
)
