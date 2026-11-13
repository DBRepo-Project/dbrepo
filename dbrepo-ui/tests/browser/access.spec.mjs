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
