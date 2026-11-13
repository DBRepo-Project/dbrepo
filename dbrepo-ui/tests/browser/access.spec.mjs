import {test, expect} from '@playwright/test'

async function setup(page, query = '') {
  const database = {id: 'db', name: 'Manual test', owner: {username: 'owner'}, tables: [], identifiers: [],
    is_public: false, is_schema_public: false, is_dashboard_enabled: true,
    accesses: [{user: {username: 'reader', qualified_name: 'Reader'}, type: 'read'}]}
  const requests = []
  await page.route('**/api/v1/**', async route => {
    const request = route.request(), path = new URL(request.url()).pathname
    if (path.endsWith('/access/reader')) {
      requests.push({method: request.method(), data: request.postDataJSON()})
      if (request.method() === 'DELETE') database.accesses = []
      else database.accesses[0].type = request.postDataJSON().type
      return route.fulfill({status: 202})
    }
    if (path.endsWith('/user')) return route.fulfill({json: [
      {username: 'owner', qualified_name: 'Owner'}, {username: 'reader', qualified_name: 'Reader'},
      {username: 'new-user', qualified_name: 'New user'}]})
    return route.fulfill({json: database})
  })
  await page.goto('/' + query)
  await page.getByRole('row').filter({hasText: 'Reader'}).getByRole('button', {name: 'Modify'}).click()
  return requests
}

test('existing grant opens for its recipient and sends only DELETE when revoked', async ({page}) => {
  const errors = []; page.on('pageerror', e => errors.push(e.message))
  const requests = await setup(page)
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText('reader', {exact: true})).toBeVisible()
  await dialog.getByRole('combobox').click()
  await page.getByRole('option', {name: 'No access', exact: true}).click()
  await dialog.getByRole('button', {name: 'Modify'}).click()
  await expect(dialog).not.toBeVisible()
  await expect(page.getByRole('row').filter({hasText: 'Reader'})).toHaveCount(0)
  expect(requests).toEqual([{method: 'DELETE', data: null}])
  expect(errors).toEqual([])
})

test('update-only permission cannot revoke; delete-only permission can', async ({page}) => {
  await setup(page, '?roles=update-database-access')
  await page.getByRole('dialog').getByRole('combobox').click()
  await expect(page.getByRole('option', {name: 'No access', exact: true})).toHaveCount(0)
  await setup(page, '?roles=delete-database-access')
  await page.getByRole('dialog').getByRole('combobox').click()
  await expect(page.getByRole('option', {name: 'No access', exact: true})).toBeVisible()
  await expect(page.getByRole('option', {name: 'Read all contents', exact: true})).toHaveCount(0)
})

for (const operation of ['create', 'update', 'remove']) test(`${operation}: partial dashboard failure closes the dialog and can be retried after reload`, async ({page}) => {
  const database = {id: 'db', name: 'Manual test', owner: {username: 'owner'}, tables: [], identifiers: [],
    is_public: false, is_schema_public: false, is_dashboard_enabled: true,
    accesses: operation === 'create' ? [] : [{user: {username: 'reader', qualified_name: 'Reader'}, type: 'write_all'}]}
  const calls = []
  await page.route('**/api/v1/**', async route => {
    const req = route.request(), path = new URL(req.url()).pathname
    if (path.endsWith('/access/reader')) {
      calls.push(req.method())
      database.accesses = operation === 'remove' ? [] : [{user: {username: 'reader', qualified_name: 'Reader'}, type: 'read'}]
      if (calls.length === 1) return route.fulfill({status: 502,
        json: {code: 'error.access.dashboard_sync', status: 'BAD_GATEWAY', message: 'Database access was saved.'}})
      return route.fulfill({status: 202})
    }
    return route.fulfill({json: path.endsWith('/user') ? [{username: 'reader', qualified_name: 'Reader'}] : database})
  })
  await page.goto('/')
  if (operation === 'create') {
    await page.getByRole('button', {name: 'Create', exact: true}).click()
    await page.getByRole('dialog').getByRole('combobox').first().locator('input').fill('Reader')
    await page.getByRole('option', {name: 'Reader', exact: true}).click()
  } else {
    await page.getByRole('row').filter({hasText: 'Reader'}).getByRole('button', {name: 'Modify'}).click()
  }
  const dialog = page.getByRole('dialog')
  await dialog.getByRole('combobox').last().click()
  await page.getByRole('option', {name: operation === 'remove' ? 'No access' : 'Read all contents', exact: true}).click()
  await dialog.getByRole('button', {name: 'Modify'}).click()
  await expect(dialog).not.toBeVisible()
  await expect(page.getByRole('alert').filter({hasText: 'Dashboard permissions may still be out of date'})).toBeVisible()
  await page.reload()
  await page.getByRole('button', {name: 'Retry', exact: true}).click()
  await expect(page.getByRole('button', {name: 'Retry', exact: true})).toHaveCount(0)
  const method = {create: 'POST', update: 'PUT', remove: 'DELETE'}[operation]
  expect(calls).toEqual([method, method])
})

test('an obsolete dashboard retry does not restore a revoked database grant', async ({page}) => {
  let mutations = 0
  await page.addInitScript(() => localStorage.setItem('cache', JSON.stringify({dashboardSyncFailures: {
    '["owner","db"]': [{username: 'reader', operation: 'create', type: 'read'}]
  }})))
  await page.route('**/api/v1/**', route => {
    const req = route.request()
    if (req.method() !== 'GET') mutations++
    return route.fulfill({json: req.url().endsWith('/user') ? [] : {
      id: 'db', name: 'Manual test', owner: {username: 'owner'}, tables: [], identifiers: [], accesses: []}})
  })
  await page.goto('/')
  await page.getByRole('button', {name: 'Retry', exact: true}).click()
  await expect(page.locator('[data-toast=warning]')).toContainText('Access has changed')
  expect(mutations).toBe(0)
  await expect(page.getByRole('button', {name: 'Retry', exact: true})).toBeVisible()
})

test('a database-service failure keeps the dialog open without claiming saved access', async ({page}) => {
  await setup(page)
  await page.route('**/api/v1/database/db/access/reader', route => route.fulfill({status: 503,
    json: {code: 'error.data.connection', message: 'Not saved', status: 'SERVICE_UNAVAILABLE'}}))
  const dialog = page.getByRole('dialog')
  await dialog.getByRole('combobox').click()
  await page.getByRole('option', {name: 'No access', exact: true}).click()
  await dialog.getByRole('button', {name: 'Modify'}).click()
  await expect(page.locator('[data-toast=error]')).toHaveCount(1)
  await expect(dialog).toBeVisible()
  await expect(page.getByRole('button', {name: 'Retry', exact: true})).toHaveCount(0)
})
