import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { runInNewContext } from 'node:vm'
import { parse } from 'vue/compiler-sfc'

function component(path, globals = {}) {
  const { descriptor, errors } = parse(readFileSync(new URL(path, import.meta.url), 'utf8'))
  assert.deepEqual(errors, [])
  const script = descriptor.script.content.replace(/^import .*$/gm, '').replace('export default', '')
  return runInNewContext(`(${script})`, {
    QueryResults: {},
    TableHistory: {},
    TableToolbar: {},
    BlobDownload: {},
    EditTuple: {},
    ...globals
  })
}
function bind(c, extra = {}) {
  const vm = { ...extra, ...c.data.call(extra) }
  for (const [k, v] of Object.entries(c.methods || {})) vm[k] = v.bind(vm)
  for (const [k, v] of Object.entries(c.computed || {}))
    Object.defineProperty(vm, k, { get: () => v.call(vm) })
  return vm
}
const defer = () => {
  let resolve, reject
  const promise = new Promise((a, b) => {
    resolve = a
    reject = b
  })
  return { promise, resolve, reject }
}
const payload = (value) => ({ headers: ['value'], result: [{ value }] })
function results(service) {
  return bind(component('../components/subset/Results.vue', { useViewService: () => service }), {
    type: 'view',
    primaryKeyColumnNames: [],
    $route: { params: { database_id: 'db' } },
    timestamp: '2020-01-01'
  })
}
test('data failure is distinct from an empty successful result and retry works', async () => {
  let fail = true
  const vm = results({
    reExecuteData: async () => {
      if (fail) throw new Error('offline')
      return { headers: [], result: [] }
    }
  })
  await vm.reExecute('v')
  assert.equal(vm.dataError, true)
  assert.equal(vm.loaded, false)
  assert.equal(vm.loadingExecute, false)
  fail = false
  await vm.reExecute('v')
  assert.equal(vm.dataError, false)
  assert.equal(vm.loaded, true)
  assert.equal(vm.result.rows.length, 0)
})
test('late data replies cannot overwrite the latest page', async () => {
  const first = defer()
  const second = defer()
  let n = 0
  const vm = results({ reExecuteData: () => (++n === 1 ? first.promise : second.promise) })
  const a = vm.reExecute('v')
  const b = vm.reExecute('v')
  second.resolve(payload('new'))
  await b
  first.resolve(payload('old'))
  await a
  assert.equal(vm.result.rows[0].value, 'new')
})
test('late failures cannot replace a newer successful result', async () => {
  const first = defer()
  let n = 0
  const vm = results({ reExecuteData: () => (++n === 1 ? first.promise : Promise.resolve(payload('new'))) })
  const a = vm.reExecute('v')
  await vm.reExecute('v')
  first.reject(new Error('old failure'))
  await a
  assert.equal(vm.dataError, false)
  assert.equal(vm.result.rows[0].value, 'new')
})
test('count failure preserves successful rows and clears its loading indicator', async () => {
  const vm = results({
    reExecuteData: async () => payload('row'),
    reExecuteCount: async () => {
      throw Error('timeout')
    }
  })
  await Promise.all([vm.reExecute('v'), vm.reExecuteCount('v')])
  assert.equal(vm.countError, true)
  assert.equal(vm.result.rows.length, 1)
  assert.equal(vm.loadingCount, false)
})
test('late count response cannot overwrite the latest count', async () => {
  const old = defer()
  let n = 0
  const vm = results({ reExecuteCount: () => (++n === 1 ? old.promise : Promise.resolve(22)) })
  const a = vm.reExecuteCount('v')
  await vm.reExecuteCount('v')
  old.resolve(11)
  await a
  assert.equal(vm.total, 22)
})
for (const [kind, permission] of [
  ['view', 'canReadData'],
  ['table', 'canViewTableData']
]) {
  test(`${kind} waits for matching metadata and reloads when it becomes readable`, async () => {
    const store = { getView: null, getTable: null, getAccess: null, getRoles: [], resourceErrors: {} }
    const c = component(`../pages/database/[database_id]/${kind}/[${kind}_id]/data.vue`, {
      useCacheStore: () => store,
      useUserService: () => ({ hasReadAccess: () => true })
    })
    const route = { params: { database_id: 'db', [`${kind}_id`]: 'resource' } }
    const vm = bind(c, { $route: route, $t: (x) => x, $nextTick: (fn) => Promise.resolve().then(fn) })
    assert.equal(vm.resourceKey, null)
    store[kind === 'view' ? 'getView' : 'getTable'] = {
      id: 'resource',
      database_id: 'other',
      is_public: true
    }
    assert.equal(vm[permission], false)
    store[kind === 'view' ? 'getView' : 'getTable'] = { id: 'resource', database_id: 'db', is_public: true }
    let reloads = 0
    vm.reload = () => reloads++
    c.watch.resourceKey.handler.call(vm, vm.resourceKey)
    await Promise.resolve()
    assert.equal(reloads, 1)
    store[kind === 'view' ? 'getView' : 'getTable'].is_public = false
    assert.equal(vm.resourceKey, null)
    c.watch.resourceKey.handler.call(vm, null)
    await Promise.resolve()
    assert.equal(reloads, 1)
  })
}
function store(globals = {}) {
  const text = readFileSync(new URL('../stores/cache.js', import.meta.url), 'utf8')
    .replace(/^import .*$/gm, '')
    .replace('export const useCacheStore =', '')
  const c = runInNewContext(text, { defineStore: (_, options) => options, ...globals })
  const vm = c.state()
  for (const [k, v] of Object.entries(c.actions)) vm[k] = v.bind(vm)
  return vm
}
test('metadata arriving after navigation is discarded and errors are retained', async () => {
  const vm = store()
  const old = defer()
  const a = vm.loadRouteResource('view', 'a', 'old', { findOne: () => old.promise })
  await vm.loadRouteResource('view', 'b', 'new', { findOne: async () => ({ id: 'new' }) })
  old.resolve({ id: 'old' })
  await a
  assert.equal(vm.view.id, 'new')
  await vm.loadRouteResource('view', 'b', 'bad', {
    findOne: async () => {
      throw Error('404')
    }
  })
  assert.equal(vm.view, null)
  assert.ok(vm.resourceErrors.view)
})
test('leaving a resource invalidates its pending metadata request', async () => {
  const vm = store()
  const pending = defer()
  const task = vm.loadRouteResource('table', 'a', 't', { findOne: () => pending.promise })
  vm.setTable(null)
  pending.resolve({ id: 't' })
  await task
  assert.equal(vm.table, null)
})
test('table list does not display the previous database while navigating', () => {
  const store = { getDatabase: { id: 'old', tables: [{ id: 'old-table' }] } }
  const c = component('../components/table/TableList.vue', {
    useCacheStore: () => store,
    sizeToHumanLabel: () => '',
    formatTimestampUTCLabel: () => ''
  })
  const vm = bind(c, { $route: { params: { database_id: 'new' } } })
  assert.equal(vm.database, null)
  assert.equal(vm.tables.length, 0)
})

test('Axios network errors and timeouts without a response are safely converted', () => {
  const source = readFileSync(new URL('../utils/index.ts', import.meta.url), 'utf8')
  const functionSource = source
    .slice(
      source.indexOf('export function axiosErrorToApiError('),
      source.indexOf('\nexport function timestampToTimeZonedTimestamp')
    )
    .replace('export function', 'function')
    .replace('error: AxiosError', 'error')
    .replace(' as Partial<ApiErrorDto> | null', '')
  const convert = runInNewContext(`(${functionSource})`)
  assert.equal(convert({ response: undefined, code: 'ERR_NETWORK' }).code, 'error.axios.connection')
  assert.equal(
    convert({ response: undefined, code: 'ECONNABORTED', message: 'timeout' }).code,
    'error.axios.timeout'
  )
  assert.equal(convert({ response: { status: 403, data: '' }, message: 'Forbidden' }).status, 403)
  const detail = 'Column name is required.'
  const invalidSchema = convert({
    response: { status: 400, data: { code: 'error.schema.invalid', message: detail } }
  })
  assert.equal(invalidSchema.code, null)
  assert.equal(invalidSchema.message, detail)
  const invalidForm = convert({
    response: { status: 400, data: { code: 'error.form.malformed', message: detail } }
  })
  assert.equal(invalidForm.code, null)
  assert.equal(invalidForm.message, detail)
})

test('data permission and missing-resource errors get distinct messages', async () => {
  let status = 403
  const vm = results({
    reExecuteData: async () => {
      throw { status }
    }
  })
  await vm.reExecute('v')
  assert.equal(vm.dataErrorKey, 'dataState.private')
  status = 404
  await vm.reExecute('v')
  assert.equal(vm.dataErrorKey, 'dataState.missing')
  status = 500
  await vm.reExecute('v')
  assert.equal(vm.dataErrorKey, 'dataState.failed')
})

test('database metadata and access grants cannot cross a route switch', async () => {
  const oldDB = defer(),
    oldAccess = defer()
  const vm = store({
    useDatabaseService: () => ({ findOne: (id) => (id === 'a' ? oldDB.promise : Promise.resolve({ id })) }),
    useAccessService: () => ({
      findOne: (id) => (id === 'a' ? oldAccess.promise : Promise.resolve({ database_id: id }))
    })
  })
  const pendingDB = vm.setRouteDatabase('a')
  const pendingAccess = vm.setRouteAccess('a', 'user')
  await vm.setRouteDatabase('b')
  await vm.setRouteAccess('b', 'user')
  oldDB.resolve({ id: 'a' })
  oldAccess.resolve({ database_id: 'a' })
  await Promise.all([pendingDB, pendingAccess])
  assert.equal(vm.database.id, 'b')
  assert.equal(vm.access.database_id, 'b')
})
