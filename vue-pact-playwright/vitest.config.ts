import { createRequire } from "node:module";
import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config'

const require = createRequire(import.meta.url);

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
      setupFiles: [require.resolve('allure-vitest/setup')],
      reporters: [
        'default',
        ['allure-vitest/reporter', { resultsDir: 'allure-results' }],
        //['allure-playwright', { resultsDir: 'allure-results' }]
      ]
    },
  }),
)
