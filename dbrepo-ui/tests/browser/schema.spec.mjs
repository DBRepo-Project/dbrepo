import {test, expect} from '@playwright/test'

test('shared schema form preserves zero scale and blocks incomplete or invalid pairs', async ({page}) => {
  const errors = []; page.on('pageerror', e => errors.push(e.message))
  const writes = []
  await page.route('**/api/v1/**', route => {
    const req = route.request()
    if (req.method() === 'POST') { writes.push(req.postDataJSON()); return route.fulfill({json: {id: 'table'}}) }
    const json = req.url().includes('/image/') ? {data_types: [
      {value: 'decimal', display_name: 'DECIMAL', size_required: false, size_min: 0, size_max: 65,
        size_step: 1, size_default: 10, d_required: false, d_min: 0, d_max: 38, d_default: 4, d_step: 1},
      {value: 'varchar', display_name: 'VARCHAR', size_required: true, size_min: 1, size_max: 255,
        size_default: 255, d_required: null, d_default: null}]} : {id: 'db', tables: [], container: {image: {id: 'image'}}}
    return route.fulfill({json})
  })
  await page.goto('/?mode=schema')
  const size = page.getByLabel('Size', {exact: true}), scale = page.getByLabel('d', {exact: true})
  const submit = page.getByRole('button', {name: 'Create table', exact: true})
  await expect(scale).toHaveValue('0')
  await scale.fill('')
  await expect(page.getByText('Set both size and d, or leave both empty to use the defaults.').first()).toBeVisible()
  await expect(submit).toBeDisabled()
  expect(writes).toHaveLength(0)
  await scale.fill('11')
  await expect(submit).toBeDisabled()
  await scale.fill('0')
  await submit.click()
  await expect.poll(() => writes.length).toBe(1)
  expect(writes[0].columns[0]).toMatchObject({size: 10, d: 0})
  await size.fill('')
  await scale.fill('')
  await submit.click()
  await expect.poll(() => writes.length).toBe(2)
  expect(writes[1].columns[0]).toMatchObject({size: null, d: null})
  expect(errors).toEqual([])
})

test('upload success is not reported as import success when a required CSV value is empty', async ({page}) => {
  await page.route('**/api/v1/**', route => {
    const req = route.request()
    if (req.url().endsWith('/upload')) return route.fulfill({json: {key: 'uploaded.csv'}})
    if (req.url().endsWith('/data/import')) return route.fulfill({status: 400,
      json: {code: 'error.import.required', message: '<b>notiz</b>', status: 'BAD_REQUEST'}})
    return route.fulfill({json: {id: 'db', tables: [], container: {image: {id: 'image'}}}})
  })
  await page.goto('/?mode=import')
  await page.locator('input[type=file]').setInputFiles({name: 'messwerte.csv', mimeType: 'text/csv',
    buffer: Buffer.from('id,ort,wert,notiz\n1,Wien,12.5,"Grüße, Wien"\n2,Graz,0,\n')})
  await page.getByRole('button', {name: 'Upload & Import', exact: true}).click()
  await expect(page.locator('.v-toast__item--error')).toContainText('Column <b>notiz</b> contains an empty value')
  await expect(page.locator('.v-toast__item--success')).toHaveCount(0)
  await expect(page.locator('.v-toast__item--error b')).toHaveCount(0)
})
