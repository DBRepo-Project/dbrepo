const test = require('ava')
const { pageMacro, before, after } = require('./_utils')

test.before(before)
test.after(after)

test('login_succeeds', pageMacro, async (t, page) => {
  const email = 'ava@example.com'
  const username = 'ava'

  await page.go('/forgot')
  await page.fill('input[name="username"]', username)
  await page.fill('input[name="email"]', email)

  /* test */
  const success = await page.waitForSelector('button[name="submit"]:not([disabled])')
  t.true(!!success, 'Failed to reset user information')
})
