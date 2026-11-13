import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import test from 'node:test'
import vm from 'node:vm'
import {parse, compileTemplate} from '@vue/compiler-sfc'

const {descriptor} = parse(readFileSync(new URL('../components/subset/Builder.vue', import.meta.url), 'utf8'))
assert.deepEqual(compileTemplate({source: descriptor.template.content, filename: 'Builder.vue', id: 'test'}).errors, [])
const sandbox = {Raw: {}, Results: {}}
vm.runInNewContext(descriptor.script.content.replace(/^import .*\n/gm, '')
  .replace('export default {', 'globalThis.component = {'), sandbox)
const component = sandbox.component
const source = (id, replica_urls = {}) => ({id, internal_name: id, replica_urls, columns: [
  {id: `${id}-id`, internal_name: 'id'}, {id: `${id}-key`, internal_name: 'replication_key'}]})
function context (replicas) {
  const state = {database: {replica_urls: replicas}, datasources: [source('first')],
    items: [source('second')], joins: [{datasource: source('second')}], select: []}
  state.selectableColumns = component.methods.selectableColumns.bind(state)
  state.columns = component.computed.columns.call(state)
  return state
}
test('Select All, filters, sorts and both join sides exclude managed replication keys', () => {
  const state = context({'https://peer.example': 'peer'})
  component.methods.toggleColumns.call(state)
  assert.deepEqual(Array.from(state.select, c => c.qualified_name), ['first.id', 'second.id'])
  assert.deepEqual(Array.from(state.selectableColumns(state.joins[0].datasource), c => c.internal_name), ['id'])
  component.methods.toggleColumns.call(state)
  assert.equal(state.select.length, 0)
})
test('table replica metadata also identifies managed keys', () => {
  const state = context({})
  assert.equal(state.selectableColumns(source('replica', {'https://peer.example': 'peer'})).length, 1)
  assert.equal(state.selectableColumns(undefined).length, 0)
})
test('an ordinary user column named replication_key stays selectable', () => {
  const state = context({})
  component.methods.toggleColumns.call(state)
  assert.equal(state.select.length, 4)
})

test('view columns with the same name are not internal table keys', () => {
  const state = context({'https://peer.example': 'peer'})
  state.database.views = [source('view')]
  assert.equal(state.selectableColumns(state.database.views[0]).length, 2)
})
