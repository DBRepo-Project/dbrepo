import assert from 'node:assert/strict'
import {test} from 'node:test'
import {numericValueError as error} from '../utils/numeric-value.mjs'
const decimal = {type: 'decimal', size: 10, d: 4, is_null_allowed: false}
test('DECIMAL accepts API numbers, zero, signs and exact strings', () => {
  for (const value of [12.5, -3.75, 0, '0', '-0', '12', '.125', '00012.5000', '-999999.9999'])
    assert.equal(error(decimal, value), null, String(value))
  assert.equal(error({...decimal, size: 65, d: 0}, '123456789012345678901234567890'), null)
})
test('DECIMAL enforces precision, scale and real nullability', () => {
  for (const value of [null, undefined, '', ' ']) {
    assert.equal(error(decimal, value)[0], 'validation.required')
    assert.equal(error({...decimal, is_null_allowed: true}, value), null)
  }
  for (const value of ['1000000', '-1000000.1234']) assert.equal(error(decimal, value)[0], 'validation.size')
  assert.equal(error(decimal, '1.12345')[0], 'validation.d')
  assert.equal(error({...decimal, d: 0}, '1.5')[0], 'validation.d')
  assert.equal(error({...decimal, size: 4}, '0.1234'), null)
  for (const value of ['no', '1e', Infinity, NaN]) assert.equal(error(decimal, value)[0], 'validation.number')
})
test('FLOAT and unconstrained DOUBLE allow exponents without fake decimal limits', () => {
  for (const type of ['float', 'double']) {
    for (const value of [12.5, -3.75, 0, '1e3', '1e-12']) assert.equal(error({type}, value), null)
    assert.equal(error({type}, '1e999')[0], 'validation.number')
  }
})

test('scientific notation from the API is checked without rounding exact decimal input', () => {
  assert.equal(error({...decimal, size: 10, d: 8}, 1e-7), null)
  assert.equal(error(decimal, 1e-7)[0], 'validation.d')
  assert.equal(error({...decimal, size: 65, d: 0}, '1e21'), null)
  assert.equal(error({...decimal, size: 3, d: 0}, '0.001e3'), null)
  assert.equal(error({...decimal, size: 3, d: 0}, '1000e-1'), null)
  assert.equal(error({...decimal, size: 3, d: 0}, '1e3')[0], 'validation.size')
})
