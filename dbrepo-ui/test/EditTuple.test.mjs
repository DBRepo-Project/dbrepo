import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import vm from 'node:vm'
import { compileTemplate, parse } from '@vue/compiler-sfc'

const source = readFileSync(new URL('../components/dialogs/EditTuple.vue', import.meta.url), 'utf8')
const { descriptor, errors } = parse(source)
assert.equal(errors.length, 0)
assert.equal(compileTemplate({ source: descriptor.template.content, filename: 'EditTuple.vue', id: 'test' }).errors.length, 0)

let payload
const sandbox = {
  BlobUpload: {},
  console,
  useTupleService: () => ({
    create: (_database, _table, body) => { payload = body; return Promise.resolve() },
    update: (_database, _table, body) => { payload = body; return Promise.resolve() }
  }),
  useToastInstance: () => ({ success () {} })
}
const script = descriptor.script.content
  .replace(/^import BlobUpload.*\n/m, '')
  .replace('export default {', 'globalThis.component = {')
vm.runInNewContext(script, sandbox)
const component = sandbox.component
const columns = [{ internal_name: 'id' }, { internal_name: 'replication_key' }, { internal_name: 'value' }]

function context (replicaUrls) {
  const state = {
    database: { replica_urls: replicaUrls },
    table: { columns, constraints: { primary_key: [{ column: columns[0] }] } },
    tuple: { id: 1, replication_key: 'existing', value: 'new' },
    oldTuple: { id: 1 },
    primaryKeyColumns: [columns[0]],
    $route: { params: { database_id: 'database', table_id: 'table' } },
    $t: key => key,
    $emit () {}
  }
  state.replicationKeyManaged = component.computed.replicationKeyManaged.call(state)
  state.editableColumns = component.computed.editableColumns.call(state)
  return state
}

test('replicated tables hide and omit the managed key', () => {
  const state = context(['https://peer.example'])
  assert.deepEqual(Array.from(state.editableColumns, column => column.internal_name), ['id', 'value'])
  component.methods.addTuple.call(state)
  assert.deepEqual(Object.keys(payload.data).sort(), ['id', 'value'])
  component.methods.updateTuple.call(state)
  assert.deepEqual(Object.keys(payload.data).sort(), ['id', 'value'])
  assert.equal(state.tuple.replication_key, 'existing')
})

test('ordinary columns named replication_key remain editable', () => {
  const state = context([])
  assert.deepEqual(Array.from(state.editableColumns, column => column.internal_name), ['id', 'replication_key', 'value'])
  component.methods.addTuple.call(state)
  assert.equal(payload.data.replication_key, 'existing')
  component.methods.updateTuple.call(state)
  assert.equal(payload.data.replication_key, 'existing')
})
