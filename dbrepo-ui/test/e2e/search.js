const test = require('ava')
const { pageMacro, before, after } = require('./_utils')

test.before(before)
test.after(after)

test('search_succeeds', pageMacro, async (t, page) => {
  const query = 'dummy'

  await page.go('/')
  await page.fill('input[placeholder="Search ..."]', query)

  /* test */
  const success = await page.waitForSelector('button[name="search-submit"]')
  t.true(!!success, 'Failed to search')
})

test('search_execute_succeeds', pageMacro, async (t, page) => {
  const query = 'dummy'

  await page.go('/')
  await page.fill('input[placeholder="Search ..."]', query)
  await page.click('button[name="search-submit"]')

  /* test */
  const success = await page.waitForSelector('button[name="search-submit"]')
  t.true(!!success, 'Failed to search')
})
