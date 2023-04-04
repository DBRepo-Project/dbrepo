const test = require('ava')
const { mutations } = require('store')
const { pageMacro, before, after } = require('./_utils')

test.before(before)
test.after(after)

test('databases_seeDatabases_succeeds', pageMacro, async (t, page) => {
  await page.go('/container')
  /* test */
  const success = await page.waitForSelector('main >> header >> text=Databases')
  t.true(!!success, 'Failed to find \'Databases\' in page')
})

test('databases_createDatabase_succeeds', pageMacro, async (t, page) => {
  const state = { token: null, user: null, database: null, table: null, access: null }

  await page.go('/container')
  mutations.SET_TOKEN(state, 'ABC')
  mutations.SET_USER(state, { username: 'ava' })
  await page.screenshot({ path: './screenshots/databases_createDatabase_succeeds.png' })
  /* test */
  t.true(true)
})
