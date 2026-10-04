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

function context () {
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
  state.refreshAll = async () => { state.refreshed = true }
  state.showError = error => failures.push(error)
  return { state, sandbox, calls, notifications, failures }
}

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
