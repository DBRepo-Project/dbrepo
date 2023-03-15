const test = require('ava')
const { pageMacro, before, after } = require('./_utils')

test.before(before)
test.after(after)

test('home_seeDatabaseRepository_succeeds', pageMacro, async (t, page) => {
  await page.go('/')

  // find 'Database Repository' anywhere on the page:
  const success = await page.waitForSelector('text=Database Repository')
  t.true(!!success, 'Failed to find \'Database Repository\' in page')
})

test('home_seeInformation_succeeds', pageMacro, async (t, page) => {
  await page.go('/')

  // find 'Information' anywhere on the page:
  const success = await page.waitForSelector('text=Information')
  t.true(!!success, 'Failed to find \'Information\' in page')
})

test('home_seeDatabases_succeeds', pageMacro, async (t, page) => {
  await page.go('/')

  // find 'Databases' anywhere on the page:
  const success = await page.waitForSelector('text=Databases')
  t.true(!!success, 'Failed to find \'Databases\' in page')
})

test('home_seeLogin_succeeds', pageMacro, async (t, page) => {
  await page.go('/')

  // find 'Login' anywhere on the page:
  const success = await page.waitForSelector('text=Login')
  t.true(!!success, 'Failed to find \'Login\' in page')
})

test('home_seeSignup_succeeds', pageMacro, async (t, page) => {
  await page.go('/')

  // find 'Signup' anywhere on the page:
  const success = await page.waitForSelector('text=Signup')
  t.true(!!success, 'Failed to find \'Signup\' in page')
})

test('home_seeSearch_succeeds', pageMacro, async (t, page) => {
  await page.go('/')

  // find 'Search' anywhere on the page:
  const success = await page.waitForSelector('[placeholder="Search ..."]')
  t.true(!!success, 'Failed to find \'Search\' in page')
})
