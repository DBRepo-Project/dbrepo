import {beforeEach, expect, test} from 'bun:test'

/* stand-ins for the browser's sessionStorage and nuxt-oidc-auth's useOidcAuth() */
const storage = new Map<string, string>()
const session: { value: any } = { value: undefined }
const g = globalThis as any
g.sessionStorage = {
  getItem: (key: string) => storage.get(key) ?? null,
  setItem: (key: string, value: string) => storage.set(key, value),
  removeItem: (key: string) => storage.delete(key)
}
g.useOidcAuth = () => ({ user: { get value() { return session.value } } })

const { useTupleDraft } = await import('../composables/tuple-draft')
const alice = { provider: 'keycloak', userInfo: { sub: 'alice-sub' }, expireAt: 1 }
const bob = { provider: 'keycloak', userInfo: { sub: 'bob-sub' }, expireAt: 1 }

beforeEach(() => {
  storage.clear()
})

test('a new-row draft is restored only for the account that wrote it', () => {
  session.value = alice
  useTupleDraft('db', 'table', 'new').save({ id: 1, note: 'alice' })
  session.value = bob
  expect(useTupleDraft('db', 'table', 'new').restore()).toBeNull()
  useTupleDraft('db', 'table', 'new').save({ id: 2, note: 'bob' })
  session.value = alice
  expect(useTupleDraft('db', 'table', 'new').restore()).toEqual({ id: 1, note: 'alice' })
})

test('an edit draft is restored only for the account that wrote it', () => {
  session.value = alice
  useTupleDraft('db', 'table', [5]).save({ id: 5, note: 'alice edit' })
  session.value = bob
  expect(useTupleDraft('db', 'table', [5]).restore()).toBeNull()
  session.value = alice
  expect(useTupleDraft('db', 'table', [5]).restore()).toEqual({ id: 5, note: 'alice edit' })
})

test('a draft opened while signed in stays with that account after the session is cleared', () => {
  session.value = alice
  const draft = useTupleDraft('db', 'table', 'new')
  session.value = {}
  draft.save({ id: 1, note: 'typed after sign-out' })
  session.value = bob
  expect(useTupleDraft('db', 'table', 'new').restore()).toBeNull()
  session.value = alice
  expect(useTupleDraft('db', 'table', 'new').restore()).toEqual({ id: 1, note: 'typed after sign-out' })
})

test('clearing removes only the owner\'s draft', () => {
  session.value = alice
  useTupleDraft('db', 'table', 'new').save({ id: 1 })
  session.value = bob
  useTupleDraft('db', 'table', 'new').save({ id: 2 })
  useTupleDraft('db', 'table', 'new').clear()
  session.value = alice
  expect(useTupleDraft('db', 'table', 'new').restore()).toEqual({ id: 1 })
})

test('no draft is kept without a signed-in account', () => {
  session.value = {}
  const draft = useTupleDraft('db', 'table', 'new')
  draft.save({ id: 1 })
  expect(storage.size).toBe(0)
  expect(draft.restore()).toBeNull()
})
