export const emptyNumber = value => value == null || String(value).trim() === ''
export const optionalNumber = value => emptyNumber(value) ? null : Number(value)

export function schemaNumberError(column, definition, field) {
  const paired = ['decimal', 'double'].includes(column.type)
  if (paired && emptyNumber(column.size) !== emptyNumber(column.d)) return 'validation.numeric_pair'
  const value = column[field]
  if (emptyNumber(value)) return definition[`${field}_required`] ? 'validation.required' : null
  const number = Number(value)
  if (!Number.isSafeInteger(number)) return 'validation.integer'
  const min = paired ? (field === 'size' ? 1 : 0) : definition[`${field}_min`]
  const max = paired ? (field === 'size' ? (column.type === 'decimal' ? 65 : 255)
    : (column.type === 'decimal' ? 38 : 30)) : definition[`${field}_max`]
  if (min != null && number < min) return 'validation.min'
  if (max != null && number > max) return 'validation.max'
  if (paired && Number(column.d) > Number(column.size)) return 'validation.numeric_scale'
  return null
}
