const test = require('ava')
const { pageMacro, before, after } = require('./_utils')

test.before(before)
test.after(after)

test('login_succeeds', pageMacro, async (t, page) => {
  const username = 'ava'
  const password = Math.random().toString(36).substring(7)

  await page.go('/login')
  await page.fill('input[name="username"]', username)
  await page.fill('input[name="password"]', password)

  /* test */
  const success = await page.waitForSelector('button[name="submit"]:not([disabled])')
  t.true(!!success, 'Failed to login')
})
