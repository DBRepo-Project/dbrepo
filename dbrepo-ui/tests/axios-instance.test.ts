import {beforeEach, expect, mock, test} from 'bun:test'

/* stand-ins for the Nuxt auto-imports used by the interceptor, shaped like nuxt-oidc-auth's useOidcAuth() */
const session: { value: any } = { value: undefined }
const fetch = mock(async () => {})
const toastError = mock((message: string) => {})
const g = globalThis as any
g.useRuntimeConfig = () => ({ public: { api: { client: 'http://localhost' } } })
g.useOidcAuth = () => ({
  loggedIn: { get value() { return Boolean(session.value?.expireAt) } },
  user: { get value() { return session.value } },
  fetch
})
g.useToastInstance = () => ({ error: toastError })
g.useNuxtApp = () => ({ $i18n: { t: (key: string) => key } })

const { useAxiosInstance } = await import('../composables/axios-instance')
const axios = useAxiosInstance()
/* echo the Authorization header instead of sending the request */
axios.defaults.adapter = async (config) =>
  ({ data: config.headers.Authorization ?? null, status: 200, statusText: 'OK', headers: {}, config })

const now = () => Math.trunc(Date.now() / 1000)
const authHeaders = async (count: number) =>
  (await Promise.all(Array.from({ length: count }, () => axios.get('/api/v1/database')))).map(r => r.data)

beforeEach(() => {
  fetch.mockReset()
  toastError.mockReset()
})

test('valid token is sent without refresh', async () => {
  session.value = { expireAt: now() + 300, canRefresh: true, accessToken: 'valid' }
  expect(await authHeaders(1)).toEqual(['Bearer valid'])
  expect(fetch).not.toHaveBeenCalled()
})

test('expired token is refreshed once for concurrent requests', async () => {
  session.value = { expireAt: now() - 10, canRefresh: true, accessToken: 'expired' }
  fetch.mockImplementation(async () => {
    await Bun.sleep(20)
    session.value = { expireAt: now() + 300, canRefresh: true, accessToken: 'refreshed' }
  })
  expect(await authHeaders(3)).toEqual(['Bearer refreshed', 'Bearer refreshed', 'Bearer refreshed'])
  expect(fetch).toHaveBeenCalledTimes(1)
})

test('token inside the expiry margin is refreshed', async () => {
  session.value = { expireAt: now() + 10, canRefresh: true, accessToken: 'expiring' }
  fetch.mockImplementation(async () => {
    session.value = { expireAt: now() + 300, canRefresh: true, accessToken: 'refreshed' }
  })
  expect(await authHeaders(1)).toEqual(['Bearer refreshed'])
  expect(fetch).toHaveBeenCalledTimes(1)
})

test('failed refresh drops the stale token and asks to sign in once', async () => {
  session.value = { expireAt: now() - 10, canRefresh: true, accessToken: 'expired' }
  fetch.mockImplementation(async () => {
    session.value = {}
  })
  expect(await authHeaders(2)).toEqual([null, null])
  expect(fetch).toHaveBeenCalledTimes(1)
  expect(toastError).toHaveBeenCalledTimes(1)
  expect(toastError).toHaveBeenCalledWith('error.auth.expired')
})

test('signed-out requests are sent without token or refresh', async () => {
  session.value = {}
  expect(await authHeaders(1)).toEqual([null])
  expect(fetch).not.toHaveBeenCalled()
})
