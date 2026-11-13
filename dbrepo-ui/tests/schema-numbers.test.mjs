import assert from 'node:assert/strict'
import {test} from 'node:test'
import {optionalNumber, schemaNumberError as error} from '../utils/schema-numbers.mjs'
const definition = {size_required: false, d_required: false}
test('zero survives serialization; cleared or omitted numeric fields become null', () => {
  for (const value of [0, '0']) assert.equal(optionalNumber(value), 0)
  for (const value of [null, undefined, '', ' ']) assert.equal(optionalNumber(value), null)
  assert.equal(optionalNumber('10'), 10)
})
test('numeric pairs validate both fields, including clear, zero, limits and type changes', () => {
  for (const field of ['size', 'd']) {
    for (const [size, d] of [[10, 0], [65, 38], [null, null], ['', undefined]])
      assert.equal(error({type: 'decimal', size, d}, definition, field), null)
    for (const [size, d] of [[10, null], ['', 0], [null, 2]])
      assert.equal(error({type: 'decimal', size, d}, definition, field), 'validation.numeric_pair')
  }
  for (const [size, d, field] of [[0, 0, 'size'], [66, 1, 'size'], [65, 39, 'd'], [10, -1, 'd'], [10, 1.5, 'd']])
    assert.ok(error({type: 'decimal', size, d}, definition, field))
  assert.equal(error({type: 'decimal', size: 3, d: 4}, definition, 'size'), 'validation.numeric_scale')
  assert.equal(error({type: 'varchar', size: 255}, {size_required: true, size_min: 1, size_max: 255}, 'size'), null)
})
