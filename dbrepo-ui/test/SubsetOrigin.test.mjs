import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import test from 'node:test'
import vm from 'node:vm'
import {parse, compileTemplate} from '@vue/compiler-sfc'

const {descriptor} = parse(readFileSync(new URL('../pages/database/[database_id]/subset/[subset_id]/info.vue', import.meta.url), 'utf8'))
assert.deepEqual(compileTemplate({source: descriptor.template.content, filename: 'info.vue', id: 'test'}).errors, [])
const sandbox = {URL, Summary: {}, SubsetToolbar: {}, Select: {}, UserBadge: {}}
vm.runInNewContext(descriptor.script.content.replace(/^import .*\n/gm, '')
  .replace('export default {', 'globalThis.component = {'), sandbox)
const origin = creation_location => sandbox.component.computed.originUrl.call({subset: {creation_location},
  database: {creation_location: 'https://database-primary.example'}})
test('uses the subset origin even when the database was created on a different site', () => {
  assert.equal(origin('https://subset-origin.example'), 'https://subset-origin.example/')
  assert.equal(origin('http://localhost:8080'), 'http://localhost:8080/')
})
test('missing or unsafe origins never become clickable links', () => {
  for (const value of [undefined, null, '', '/relative', 'javascript:alert(1)', 'data:text/html,test',
    'https://user:password@example.com', 'not a URL']) assert.equal(origin(value), null)
})
