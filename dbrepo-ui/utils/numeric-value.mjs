/** Return a translation key and parameters, without converting exact decimals to Number. */
export function numericValueError(column, value) {
  if (value == null || String(value).trim() === '') {
    return column.is_null_allowed ? null : ['validation.required']
  }
  const text = String(value).trim()
  const fixed = column.type === 'decimal' || (column.type === 'double' && column.size != null && column.d != null)
  const pattern = fixed ? /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/ : /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i
  if (!pattern.test(text) || !Number.isFinite(Number(text))) return ['validation.number']
  if (!fixed) return null
  const [integer, fraction = ''] = text.replace(/^[+-]/, '').split('.')
  const scale = column.d ?? 0
  const integerDigits = (column.size ?? 10) - scale
  if (integer.replace(/^0+/, '').length > integerDigits) return ['validation.size', {size: integerDigits}]
  if (fraction.length > scale) return ['validation.d', {size: scale}]
  return null
}
