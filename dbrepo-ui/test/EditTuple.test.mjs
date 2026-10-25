import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import vm from 'node:vm'
import { compileTemplate, parse } from '@vue/compiler-sfc'
import { transformSync } from 'esbuild'

const source = readFileSync(new URL('../components/dialogs/EditTuple.vue', import.meta.url), 'utf8')
const { descriptor, errors } = parse(source)
assert.equal(errors.length, 0)
assert.equal(compileTemplate({ source: descriptor.template.content, filename: 'EditTuple.vue', id: 'test' }).errors.length, 0)

let payload
const storage = new Map()
const failures = []
const sandbox = {
  BlobUpload: {},
  console,
  sessionStorage: {
    getItem: key => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value),
    removeItem: key => storage.delete(key)
  },
  useOidcAuth: () => ({ user: { value: { provider: 'keycloak', userInfo: { sub: 'owner' } } } }),
  useTupleService: () => ({
    create: (_database, _table, body) => { payload = body; return Promise.resolve() },
    update: (_database, _table, body) => { payload = body; return Promise.resolve() }
  }),
  useToastInstance: () => ({ success () {}, error: message => failures.push(message) })
}
const draftSource = readFileSync(new URL('../composables/tuple-draft.ts', import.meta.url), 'utf8')
  .replace('export const useTupleDraft =', 'globalThis.useTupleDraft =')
vm.runInNewContext(transformSync(draftSource, { loader: 'ts' }).code, sandbox)
const script = descriptor.script.content
  .replace(/^import BlobUpload.*\n/m, '')
  .replace('export default {', 'globalThis.component = {')
vm.runInNewContext(script, sandbox)
const component = sandbox.component
const columns = [{ internal_name: 'id' }, { internal_name: 'replication_key' }, { internal_name: 'value' }]

function context (replicaUrls) {
  storage.clear()
  failures.length = 0
  const state = {
    database: { replica_urls: replicaUrls },
    table: { columns, constraints: { primary_key: [{ column: columns[0] }] } },
    tuple: { id: 1, replication_key: 'existing', value: 'new' },
    oldTuple: { id: 1 },
    primaryKeyColumns: [columns[0]],
    fetchContainer () {},
    validate () {},
    $route: { params: { database_id: 'database', table_id: 'table' } },
    $t: key => key,
    $emit () {}
  }
  state.replicationKeyManaged = component.computed.replicationKeyManaged.call(state)
  state.editableColumns = component.computed.editableColumns.call(state)
  component.mounted.call(state)
  return state
}

test('replicated tables hide and omit the managed key', async () => {
  const state = context(['https://peer.example'])
  assert.deepEqual(Array.from(state.editableColumns, column => column.internal_name), ['id', 'value'])
  component.methods.addTuple.call(state)
  assert.deepEqual(Object.keys(payload.data).sort(), ['id', 'value'])
  await new Promise(setImmediate)
  component.methods.updateTuple.call(state)
  assert.deepEqual(Object.keys(payload.data).sort(), ['id', 'value'])
  await new Promise(setImmediate)
  assert.equal(state.tuple.replication_key, 'existing')
  assert.deepEqual(failures, [])
})

test('ordinary columns named replication_key remain editable', async () => {
  const state = context([])
  assert.deepEqual(Array.from(state.editableColumns, column => column.internal_name), ['id', 'replication_key', 'value'])
  component.methods.addTuple.call(state)
  assert.equal(payload.data.replication_key, 'existing')
  await new Promise(setImmediate)
  component.methods.updateTuple.call(state)
  assert.equal(payload.data.replication_key, 'existing')
  await new Promise(setImmediate)
  assert.deepEqual(failures, [])
})

test('restored drafts keep the original update key and omit managed replication keys', async () => {
  const state = context(['https://peer.example'])
  state.edit = true
  state.draft = sandbox.useTupleDraft('database', 'table', [1])
  state.draft.save({ id: 2, replication_key: 'stale-draft-key', value: 'restored' })
  component.mounted.call(state)
  assert.equal(state.tuple.value, 'restored')
  component.methods.updateTuple.call(state)
  assert.equal(payload.keys.id, 1)
  assert.equal(payload.data.id, 2)
  assert.equal(payload.data.replication_key, undefined)
  await new Promise(setImmediate)
  assert.equal(state.draft.restore(), null)
  assert.deepEqual(failures, [])
})
