import {test, expect} from '@playwright/test'

for (const add of [false, true]) test(`${add ? 'add' : 'edit'} numeric row loaded through HTTP, save zero and reload`, async ({page}) => {
  const errors = []; page.on('pageerror', e => errors.push(e.message))
  let row = {id: 1, wert: 12.5, floating: -3.75, double: 0}
  const writes = []
  const table = {id: 'table', columns: [
    {internal_name: 'id', type: 'bigint', is_null_allowed: false},
    {internal_name: 'wert', type: 'decimal', size: 10, d: 4, is_null_allowed: false},
    {internal_name: 'floating', type: 'float', is_null_allowed: true},
    {internal_name: 'double', type: 'double', is_null_allowed: true}],
    constraints: {primary_key: [{column: {internal_name: 'id'}}]}}
  await page.route('**/api/v1/**', route => {
    const req = route.request(), path = new URL(req.url()).pathname
    if (path.endsWith('/data')) {
      if (req.method() !== 'GET') {
        writes.push({method: req.method(), payload: req.postDataJSON()})
        row = req.postDataJSON().data
        return route.fulfill({status: 202})
      }
      return route.fulfill({json: row})
    }
    return route.fulfill({json: path.endsWith('/table') ? table : {id: 'db', container: {id: 'container'}}})
  })
  const url = '/?mode=tuple' + (add ? '&add' : '')
  await page.goto(url)
  await expect(page.getByLabel('wert', {exact: true})).toHaveValue('12.5')
  await expect(page.getByLabel('floating', {exact: true})).toHaveValue('-3.75')
  await expect(page.getByLabel('double', {exact: true})).toHaveValue('0')
  const save = page.locator(add ? '#addTuple' : '#updateTuple')
  await page.getByLabel('wert', {exact: true}).fill('1000000')
  await expect(page.getByText('At most 6 digits before the decimal point.')).toBeVisible()
  await expect(save).toBeDisabled()
  await page.getByLabel('wert', {exact: true}).fill('0')
  await page.getByLabel('floating', {exact: true}).fill('')
  await save.click()
  await expect(save).not.toBeVisible()
  expect(writes).toHaveLength(1)
  expect(writes[0].method).toBe(add ? 'POST' : 'PUT')
  expect(writes[0].payload.data.wert).toBe('0')
  expect(writes[0].payload.data.floating).toBe(null)
  if (!add) expect(writes[0].payload.keys).toEqual({id: 1})
  await page.reload()
  await expect(page.getByLabel('wert', {exact: true})).toHaveValue('0')
  expect(errors).toEqual([])
})
