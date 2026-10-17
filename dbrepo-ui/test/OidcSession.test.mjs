import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import test from 'node:test'
import vm from 'node:vm'
import { defu } from 'defu'
import { transformSync } from 'esbuild'

const require = createRequire(import.meta.url)
const entry = require.resolve('nuxt-oidc-auth/package.json')
const source = readFileSync(new URL('./dist/runtime/server/utils/session.js', `file://${entry}`), 'utf8')

test('refresh keeps exposed tokens out of the sealed session cookie', async () => {
  const sealed = []
  const session = {
    id: 'test-session',
    data: {
      provider: 'keycloak', canRefresh: true, expireAt: Date.now() / 1000 + 3600,
      userInfo: { preferred_username: 'researcher' }, claims: { realm_access: { roles: ['researcher'] } }
    },
    async update (data) {
      Object.assign(this.data, data)
      sealed.push(structuredClone(this.data))
    }
  }
  let persistent = { exp: Date.now() / 1000 + 3600, accessToken: 'original-access-token',
    refreshToken: 'refresh-token', idToken: 'original-id-token' }
  const config = { providers: { keycloak: { exposeAccessToken: true, exposeIdToken: true,
    tokenValidationMode: 'legacy', sessionConfiguration: {} } }, session: { expirationCheck: true } }
  const log = { info () {}, warn () {}, error () {} }
  const imports = {
    '#imports': { useRuntimeConfig: () => ({ oidc: config }) },
    defu: { defu },
    h3: { useSession: async () => session },
    hookable: { createHooks: () => ({ callHookParallel: async () => {} }) },
    'nitropack/runtime': { useStorage: () => ({ getItem: async () => persistent,
      setItem: async (_, value) => { persistent = value } }) },
    '../../providers/index.js': { keycloak: {} },
    './oidc.js': { configMerger: defu, useOidcLogger: () => log,
      refreshAccessToken: async () => ({
        user: { canRefresh: true, expireAt: Date.now() / 1000 + 3600 },
        tokens: { accessToken: 'new-access-token', refreshToken: 'new-refresh-token', idToken: 'new-id-token' },
        parsedAccessToken: { exp: Date.now() / 1000 + 3600, iat: Date.now() / 1000 }, expiresIn: 3600
      }) },
    './security.js': { encryptToken: async token => token, decryptToken: async token => token },
    './session-options.js': { resolveMissingPersistentSessionMode: () => 'clear' },
    './config.js': { resolveProviderConfig: defu, validateProviderConfig: () => ({ valid: true }) },
    './redirect.js': { withAppBase: value => value },
    '../../utils/environment.js': { isProductionEnvironment: () => true }
  }
  const exported = { exports: {} }
  vm.runInNewContext(transformSync(source, { format: 'cjs', platform: 'node' }).code, {
    module: exported, exports: exported.exports, structuredClone, Date, console,
    process: { env: { NUXT_OIDC_SESSION_SECRET: 'test'.repeat(16), NUXT_OIDC_TOKEN_KEY: 'test-key' } },
    require: name => {
      assert.ok(name in imports, `Unexpected dependency: ${name}`)
      return imports[name]
    }
  })
  const event = { context: { sessions: { 'nuxt-oidc-auth': { createdAt: Date.now() } } } }
  const exposed = await exported.exports.getUserSession(event)
  assert.equal(exposed.accessToken, 'original-access-token')
  assert.equal(exposed.idToken, 'original-id-token')
  const refreshed = await exported.exports.refreshUserSession(event)
  assert.equal(refreshed.accessToken, 'new-access-token')
  assert.equal(refreshed.userInfo.preferred_username, 'researcher')
  assert.equal(sealed.length, 1)
  assert.equal('accessToken' in sealed[0], false)
  assert.equal('idToken' in sealed[0], false)
  assert.equal('refreshToken' in sealed[0], false)
})
