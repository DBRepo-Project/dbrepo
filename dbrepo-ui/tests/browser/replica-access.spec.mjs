import {test, expect} from '@playwright/test'

test('secondary replica still permits READ and revoke but never offers write grants', async ({page}) => {
  const database = {id: 'db', name: 'Replica', owner: {username: 'owner'}, tables: [], identifiers: [],
    creation_location: 'https://primary.example', is_public: false, is_schema_public: false,
    accesses: [{user: {username: 'reader', qualified_name: 'Reader'}, type: 'read'}]}
  await page.route('**/api/v1/**', route => route.fulfill({json: route.request().url().endsWith('/user')
    ? [{username: 'reader', qualified_name: 'Reader'}] : database}))
  await page.goto('/')
  await page.getByRole('row').filter({hasText: 'Reader'}).getByRole('button', {name: 'Modify'}).click()
  await page.getByRole('dialog').getByRole('combobox').click()
  await expect(page.getByRole('option')).toHaveCount(2)
  await expect(page.getByRole('option', {name: 'Read all contents', exact: true})).toBeVisible()
  await expect(page.getByRole('option', {name: 'No access', exact: true})).toBeVisible()
})
