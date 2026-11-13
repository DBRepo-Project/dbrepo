import {test, expect} from '@playwright/test'

const table = {id: 'table', name: 'Measurements', internal_name: 'measurements', columns: [
  {id: 'id', internal_name: 'id'}, {id: 'value', internal_name: 'value'}, {id: 'managed', internal_name: 'replication_key'}]}
const database = {id: 'db', owner: {username: 'owner'}, is_public: true, is_schema_public: true, tables: [table], views: [],
  replica_urls: {'https://peer.example': 'peer'}, container: {image: {operators: [], data_types: []}}}

test('Select All submits a replicated table without its managed key', async ({page}) => {
  const errors = [], writes = []
  page.on('pageerror', e => errors.push(e.message))
  await page.route('**/api/v1/**', route => {
    if (route.request().method() === 'POST') {
      writes.push(route.request().postDataJSON())
      return route.fulfill({json: {id: 'subset'}})
    }
    return route.fulfill({json: database})
  })
  await page.goto('/?mode=subset')
  await page.getByRole('combobox').nth(0).click()
  await page.getByRole('option', {name: 'measurements', exact: true}).click()
  await page.keyboard.press('Escape')
  await page.getByRole('combobox').nth(1).click()
  await expect(page.getByRole('option', {name: 'replication_key', exact: true})).toHaveCount(0)
  await page.getByText('Select All', {exact: true}).click()
  await page.keyboard.press('Escape')
  await page.getByRole('button', {name: 'Create', exact: true}).click()
  await expect.poll(() => writes.length).toBe(1)
  expect(writes[0].columns.map(c => c.id)).toEqual(['id', 'value'])
  expect(errors).toEqual([])
})

for (const origin of ['https://origin.example', null, 'javascript:alert(1)']) {
  test(`subset info renders safe provenance: ${origin}`, async ({page}) => {
    const errors = []; page.on('pageerror', e => errors.push(e.message))
    await page.route('**/api/v1/**', route => route.fulfill({json: route.request().url().endsWith('/subset/subset')
      ? {id: 'subset', owner: {username: 'owner'}, query: 'SELECT id FROM measurements', creation_location: origin, identifiers: []} : database}))
    await page.goto('/?mode=origin')
    await expect(page.getByText('Originally created on', {exact: true})).toBeVisible()
    if (origin?.startsWith('https:')) {
      await expect(page.getByRole('link', {name: 'https://origin.example/'})).toHaveAttribute('href', 'https://origin.example/')
    } else {
      await expect(page.getByText('Not available', {exact: true})).toBeVisible()
      await expect(page.locator('a[href^="javascript:"]')).toHaveCount(0)
    }
    expect(errors).toEqual([])
  })
}
