import {defineConfig} from '@playwright/test'
export default defineConfig({
  testDir: './tests/browser',
  use: {baseURL: 'http://127.0.0.1:4173', headless: true},
  webServer: {command: 'node node_modules/vite/bin/vite.js --config tests/browser/vite.config.mjs',
    url: 'http://127.0.0.1:4173', reuseExistingServer: !process.env.CI}
})
