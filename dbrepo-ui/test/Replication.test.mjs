import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import vm from 'node:vm'
import { compileTemplate, parse } from '@vue/compiler-sfc'
import { transformSync } from 'esbuild'

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const { descriptor, errors } = parse(read('../pages/replication/index.vue'))
assert.equal(errors.length, 0)
assert.equal(compileTemplate({ source: descriptor.template.content, filename: 'replication/index.vue', id: 'test' }).errors.length, 0)

function context ({ stubRefresh = true } = {}) {
  const calls = []
  const notifications = []
  const failures = []
  const sandbox = {
    URL,
    useCacheStore: () => ({ getRoles: ['system'] }),
    useToastInstance: () => ({ success: message => notifications.push(message) }),
    useAxiosInstance: () => ({
      post: async (path, body) => {
        calls.push({ path, body })
        return { status: 202, data: { status: 'queued' } }
      }
    })
  }
  for (const path of ['../utils/index.ts', '../composables/replication-service.ts']) {
    const source = read(path).replace(/^import .*\n/gm, '').replace(/\bexport /g, '')
      .replace('const useReplicationService =', 'globalThis.useReplicationService =')
    vm.runInNewContext(transformSync(source, { loader: 'ts' }).code, sandbox)
  }
  vm.runInNewContext(descriptor.script.content.replace(/^import .*\n/gm, '')
    .replace('export default {', 'globalThis.component = {'), sandbox)
  const component = sandbox.component
  const state = {
    ...component.data(),
    $config: { public: { api: { client: 'https://primary.example/' } } },
    $t: (key, values) => values ? `${key}: ${JSON.stringify(values)}` : key,
    databases: [
      { id: 'first', name: 'First replica', creation_location: 'https://primary.example', replica_urls: {} },
      { id: 'existing', name: 'Existing replicas', replica_urls: { 'https://peer.example/': 'remote-id' } },
      { id: 'secondary', name: 'Secondary', creation_location: 'https://other.example', replica_urls: {} }
    ]
  }
  for (const [name, method] of Object.entries(component.methods)) {
    state[name] = method.bind(state)
  }
  for (const [name, getter] of Object.entries(component.computed)) {
    Object.defineProperty(state, name, { get: () => getter.call(state) })
  }
  if (stubRefresh) state.refreshAll = async () => { state.refreshed = true }
  state.showError = error => failures.push(error)
  return { state, sandbox, calls, notifications, failures }
}

const deferred = () => {
  let resolve
  let reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

test('slow status checks do not hold back databases, access or the visible outbox', async () => {
  const { state, sandbox } = context({ stubRefresh: false })
  const status = deferred()
  sandbox.useReplicationService = () => ({
    findStatus: () => status.promise,
    findReplicationOutbox: async () => [{ id: 'job' }],
    findAccess: async () => [{ database_id: 'replica', local_username: 'owner' }]
  })
  sandbox.useDatabaseService = () => ({ findAll: async () => [{ id: 'database' }] })
  sandbox.useUserService = () => ({ findAll: async () => [{ username: 'owner' }] })
  const refresh = state.refreshAll()
  await new Promise(setImmediate)
  assert.equal(state.databases[0].id, 'database')
  assert.equal(state.replicationOutbox[0]?.id, 'job')
  assert.equal(state.replicaAccess[0]?.database_id, 'replica')
  assert.equal(state.loadingDatabases, false)
  assert.equal(state.loadingOutboxes, false)
  assert.equal(state.loadingAccess, false)
  assert.equal(state.loading, true)
  status.resolve({ health: { status: 'UP' } })
  await refresh
  assert.equal(state.status.health.status, 'UP')
  assert.equal(state.loading, false)
})

test('a failed status check leaves successful sections available', async () => {
  const { state, sandbox, failures } = context({ stubRefresh: false })
  sandbox.useReplicationService = () => ({
    findStatus: async () => { throw new Error('Health check unavailable') },
    findReplicationOutbox: async () => [{ id: 'job' }],
    findAccess: async () => []
  })
  sandbox.useDatabaseService = () => ({ findAll: async () => [{ id: 'database' }] })
  sandbox.useUserService = () => ({ findAll: async () => [] })
  await state.refreshAll()
  assert.equal(state.databases[0].id, 'database')
  assert.equal(state.replicationOutbox[0]?.id, 'job')
  assert.equal(failures.length, 1)
  assert.equal(state.loading, false)
})

test('outbox refresh reads only the selected service and never retries jobs', async () => {
  const { state, sandbox } = context()
  const calls = []
  sandbox.useReplicationService = () => ({
    findReplicationOutbox: async () => { calls.push('replication'); return [{ id: 'r' }] },
    findMetadataOutbox: async () => { calls.push('metadata'); return [{ id: 'm' }] },
    findDataOutbox: async id => { calls.push(`data:${id}`); return [{ id: 'd' }] }
  })
  for (const source of ['replication', 'metadata', 'data']) {
    state.activeOutbox = source
    await state.refreshOutbox()
  }
  assert.deepEqual(calls, ['replication', 'metadata'])
  state.selectedDatabaseId = 'database'
  await state.refreshOutbox()
  assert.deepEqual(calls, ['replication', 'metadata', 'data:database'])
  assert.equal(state.dataOutbox[0].id, 'd')
  assert.equal(state.refreshed, undefined)
  assert.equal(state.loadingOutboxes, false)
  assert.match(descriptor.template.content, /@click="refreshOutbox"/)
})

test('outdated outbox requests cannot replace a newer refresh or clear its loading state', async () => {
  const { state, sandbox, failures } = context()
  const old = deferred()
  const current = deferred()
  let request = 0
  sandbox.useReplicationService = () => ({ findReplicationOutbox: () => (++request === 1 ? old : current).promise })
  const first = state.refreshOutbox()
  const second = state.refreshOutbox()
  old.reject(new Error('Outdated failure'))
  await first
  assert.equal(state.loadingOutboxes, true)
  assert.equal(failures.length, 0)
  current.resolve([{ id: 'current' }])
  await second
  assert.equal(state.replicationOutbox[0].id, 'current')
  assert.equal(state.loadingOutboxes, false)
})

test('a data outbox response from another selected database is ignored', async () => {
  const { state, sandbox } = context()
  const old = deferred()
  state.activeOutbox = 'data'
  state.selectedDatabaseId = 'first'
  sandbox.useReplicationService = () => ({ findDataOutbox: () => old.promise })
  const first = state.refreshOutbox()
  state.selectedDatabaseId = 'second'
  old.resolve([{ id: 'wrong-database' }])
  await first
  assert.equal(state.dataOutbox.length, 0)
  assert.equal(state.loadingOutboxes, false)
})

test('outbox refresh respects administrative permissions', async () => {
  const { state, sandbox } = context()
  state.cacheStore = { getRoles: [] }
  sandbox.useReplicationService = () => { throw new Error('Must not make requests') }
  await state.refreshOutbox()
  assert.equal(state.loadingOutboxes, false)
})

test('add-replica sources include every primary, while synchronisation keeps its existing filter', () => {
  const { state } = context()
  assert.deepEqual(Array.from(state.primaryDatabases, database => database.id), ['first', 'existing'])
  assert.deepEqual(Array.from(state.replicatedDatabases, database => database.id), ['existing'])
})

test('target validation rejects invalid, same-site and configured URLs after normalisation', () => {
  const { state } = context()
  state.replicaDatabaseId = 'existing'
  for (const [url, error] of [
    ['', ''],
    ['not a URL', 'invalidUrl'],
    ['ftp://peer.example', 'invalidUrl'],
    ['https://user:secret@peer.example', 'invalidUrl'],
    ['https://peer.example?query=1', 'invalidUrl'],
    ['https://peer.example#fragment', 'invalidUrl'],
    [' https://PRIMARY.example:443/// ', 'sameSite'],
    ['https://PEER.example:443', 'exists'],
    ['https://new.example/site/', '']
  ]) {
    state.replicaUrl = url
    assert.equal(state.replicaUrlError, error ? `replication.add.${error}` : '', url)
  }
})

test('submission uses the shared API service, reports queued and refreshes without claiming completion', async () => {
  const { state, calls, notifications } = context()
  state.replicaDatabaseId = 'first'
  state.replicaUrl = ' https://NEW.example:443/ '
  await state.addReplica()
  assert.equal(calls.length, 1)
  assert.equal(calls[0].path, '/api/v1/database/first/replicas')
  assert.equal(JSON.stringify(calls[0].body), JSON.stringify({ replica_url: 'https://new.example' }))
  assert.match(state.replicaQueued, /^replication.add.queued:/)
  assert.match(state.replicaQueued, /First replica/)
  assert.deepEqual(notifications, [state.replicaQueued])
  assert.equal(state.refreshed, true)
  assert.equal(state.replicaUrl, '')
  assert.equal(state.addingReplica, false)
  const locale = JSON.parse(read('../locales/en-US.json'))
  assert.match(locale.replication.add.queued, /queued.*not yet complete/)
})

test('submission is guarded for missing sources, secondaries, invalid targets, loading and permissions', async () => {
  const { state, calls } = context()
  for (const patch of [
    { replicaDatabaseId: null },
    { replicaDatabaseId: 'secondary' },
    { replicaUrl: '' },
    { replicaUrl: 'https://primary.example/' },
    { replicaDatabaseId: 'existing', replicaUrl: 'https://peer.example' },
    { addingReplica: true },
    { loadingDatabases: true },
    { cacheStore: { getRoles: [] } }
  ]) {
    Object.assign(state, { replicaDatabaseId: 'first', replicaUrl: 'https://new.example', addingReplica: false, loadingDatabases: false }, patch)
    await state.addReplica()
  }
  assert.equal(calls.length, 0)
})

test('pending requests cannot be submitted twice and server rejections preserve input', async () => {
  const { state, sandbox, notifications, failures } = context()
  let rejectRequest
  let requests = 0
  sandbox.useAxiosInstance = () => ({ post: () => {
    requests++
    return new Promise((resolve, reject) => { rejectRequest = reject })
  } })
  state.replicaDatabaseId = 'first'
  state.replicaUrl = 'https://new.example'
  const pending = state.addReplica()
  assert.equal(state.addingReplica, true)
  await state.addReplica()
  assert.equal(requests, 1)
  rejectRequest({ response: { data: { status: 'CONFLICT', message: 'Target already configured' } } })
  await pending
  assert.equal(state.addingReplica, false)
  assert.equal(state.replicaQueued, null)
  assert.equal(state.replicaUrl, 'https://new.example')
  assert.equal(state.refreshed, undefined)
  assert.equal(notifications.length, 0)
  assert.equal(failures[0].message, 'Target already configured')
})
