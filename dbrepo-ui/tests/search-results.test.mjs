import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import {test} from 'node:test'
import {runInNewContext} from 'node:vm'
import {parse} from 'vue/compiler-sfc'

function options(path, globals = {}) {
  const source = readFileSync(new URL(path, import.meta.url), 'utf8')
  const {descriptor, errors} = parse(source, {filename: path})
  assert.deepEqual(errors, [])
  const script = descriptor.script.content
    .replace(/^import AdvancedSearch.*$/m, '')
    .replace('export default', '')
  return runInNewContext(`(${script})`, {AdvancedSearch: {}, ...globals})
}

function page(globals = {}) {
  const component = options('../pages/search.vue', globals)
  const instance = component.data()
  for (const [name, method] of Object.entries(component.methods)) instance[name] = method.bind(instance)
  return instance
}

test('selected result type reaches the page with its result array', async () => {
  const records = [{id: 'owner-id', username: 'owner'}]
  const instance = page({
    useSearchService: () => ({general_search: async (type) => {
      assert.equal(type, 'user')
      return records
    }})
  })
  const component = options('../components/search/AdvancedSearch.vue')
  let pending
  const advanced = {searchType: 'user', advancedSearchData: {},
    $emit: (event, type, filters) => {
      assert.equal(event, 'search')
      pending = instance.searchResults(type, filters)
    }}
  component.methods.advancedSearch.call(advanced)
  advanced.searchType = 'column'
  await pending
  assert.equal(instance.type, 'user')
  assert.equal(instance.results, records)
  assert.equal(instance.title(records[0]), 'owner')
  assert.equal(instance.tags(records[0]).length, 0)
})

test('a direct text search shows the latest term even if the earlier request finishes last', async () => {
  let finishCoffee
  const traffic = [{name: 'Traffic'}]
  const instance = page({useSearchService: () => ({
    fuzzy_search: term => term === 'coffee'
      ? new Promise(resolve => { finishCoffee = resolve })
      : Promise.resolve(traffic)
  })})
  instance.q = 'coffee'
  const pending = instance.searchFromRoute()
  instance.q = 'traffic'
  await instance.searchFromRoute()
  finishCoffee([{name: 'Coffee'}])
  await pending
  assert.equal(instance.results, traffic)
  assert.equal(instance.loading, false)
})

test('a service error stays visible until the same search is retried successfully', async () => {
  let attempts = 0
  const instance = page({useSearchService: () => ({fuzzy_search: async () => {
    if (++attempts === 1) throw new Error('Unavailable')
    return []
  }})})
  instance.q = 'coffee_productivity_prediction'
  await instance.searchFromRoute()
  assert.equal(instance.failed, true)
  await instance.searchFromRoute()
  assert.equal(instance.failed, false)
  assert.equal(instance.results.length, 0)
})

test('user metadata renders from UserBrief fields', () => {
  const instance = page()
  instance.type = 'user'
  const user = {qualified_name: 'Example User', orcid: '0000-0001-2345-6789'}
  assert.equal(instance.title(user), 'Example User')
  assert.equal(instance.tags(user)[0].text, 'ORCID')
})

test('column tags accept missing and URI-only semantic metadata', () => {
  const instance = page()
  instance.type = 'column'
  assert.equal(instance.tags({concept: null, unit: null}).length, 0)
  const tags = instance.tags({concept: null, unit: null, concept_uri: 'https://example.org/concept',
    unit_uri: 'https://example.org/unit'})
  assert.equal(tags[0].text, 'https://example.org/concept')
  assert.equal(tags[1].text, 'https://example.org/unit')
})

test('concept and unit results use their URI when no label is supplied', () => {
  const instance = page()
  for (const type of ['concept', 'unit']) {
    instance.type = type
    assert.equal(instance.title({uri: 'https://example.org/value'}), 'https://example.org/value')
  }
})

for (const [type, record, title, link] of [
  ['database', {id: 'db', name: 'Database'}, 'Database', '/database/db/info'],
  ['table', {id: 'table', database_id: 'db', name: 'Table'}, 'Table', '/database/db/table/table/info'],
  ['view', {id: 'view', database_id: 'db', name: 'View', query: 'SELECT 1'}, 'View', '/database/db/view/view/info'],
  ['column', {id: 'column', database_id: 'db', table_id: 'table', name: 'Column', type: 'bigint',
    concept: null, unit: null}, 'Column', '/database/db/table/table/schema'],
  ['user', {id: 'user', username: 'owner', name: 'Owner'}, 'owner', null],
  ['concept', {uri: 'https://example.org/concept'}, 'https://example.org/concept', 'https://example.org/concept'],
  ['unit', {uri: 'https://example.org/unit'}, 'https://example.org/unit', 'https://example.org/unit']
]) {
  test(`${type} results select their title, description and link`, () => {
    const instance = page()
    instance.onSearchResult([record], type)
    assert.equal(instance.title(record), title)
    assert.equal(instance.link(record), link)
    assert.doesNotThrow(() => instance.description(record))
    assert.doesNotThrow(() => instance.tags(record))
  })
}

test('identifier results select their metadata and persistent link', () => {
  const component = options('../pages/search.vue', {useIdentifierService: () => ({
    identifierPreferEnglishTitle: (item) => item.titles[0].title,
    identifierPreferEnglishDescription: (item) => item.descriptions[0].description
  })})
  const instance = component.data()
  for (const [name, method] of Object.entries(component.methods)) instance[name] = method.bind(instance)
  const record = {id: 'record', titles: [{title: 'Record'}], descriptions: [{description: 'Description'}],
    publication_year: 2026, status: 'published'}
  instance.onSearchResult([record], 'identifier')
  assert.equal(instance.title(record), 'Record')
  assert.equal(instance.description(record), 'Description')
  assert.equal(instance.link(record), '/pid/record')
  assert.equal(instance.tags(record)[0].text, 2026)
})
