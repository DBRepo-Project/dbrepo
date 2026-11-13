/** Return a translation key and parameters, without converting exact decimals to Number. */
export function numericValueError(column, value) {
  if (value == null || String(value).trim() === '') {
    return column.is_null_allowed ? null : ['validation.required']
  }
  const text = String(value).trim()
  const fixed = column.type === 'decimal' || (column.type === 'double' && column.size != null && column.d != null)
  const pattern = /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i
  if (!pattern.test(text) || !Number.isFinite(Number(text))) return ['validation.number']
  if (!fixed) return null
  const [significand, exponent = '0'] = text.replace(/^[+-]/, '').toLowerCase().split('e')
  const [integer, fraction = ''] = significand.split('.')
  const digits = (integer + fraction).replace(/^0+/, '')
  if (!digits) return null
  const fractionalDigits = fraction.length - Number(exponent)
  const trailingZeros = digits.match(/0+$/)?.[0].length ?? 0
  const scale = column.d ?? 0
  const integerDigits = (column.size ?? 10) - scale
  if (Math.max(0, digits.length - fractionalDigits) > integerDigits) return ['validation.size', {size: integerDigits}]
  if (Math.max(0, fractionalDigits - trailingZeros) > scale) return ['validation.d', {size: scale}]
  return null
}
