const test = require('ava')
const {
  isNonNegativeInteger,
  formatDateUTC,
  formatYearUTC,
  formatMonthUTC,
  formatDayUTC,
  formatTimestamp,
  formatTimestampUTCLabel,
  formatTimestampUTC
} = require('@/utils')

test('isNonNegativeInteger_succeeds', (t) => {
  /* test */
  const response = isNonNegativeInteger('1')
  t.is(response, true)
})

test('isNonNegativeInteger_zero_succeeds', (t) => {
  /* test */
  const response = isNonNegativeInteger('0')
  t.is(response, true)
})

test('isNonNegativeInteger_fails', (t) => {
  /* test */
  const response = isNonNegativeInteger('-1')
  t.is(response, false)
})

test('formatDateUTC_succeeds', (t) => {
  /* test */
  const response = formatDateUTC('2023-02-15 10:32:21')
  t.is(response, '2023-02-15')
})

test('formatDateUTC_fails', (t) => {
  /* test */
  const response = formatDateUTC(null)
  t.is(response, null)
})

test('formatYearUTC_fails', (t) => {
  /* test */
  const response = formatYearUTC(null)
  t.is(response, null)
})

test('formatYearUTC_succeeds', (t) => {
  /* test */
  const response = formatYearUTC('2023-02-15 10:32:21')
  t.is(response, '2023')
})

test('formatMonthUTC_fails', (t) => {
  /* test */
  const response = formatMonthUTC(null)
  t.is(response, null)
})

test('formatMonthUTC_succeeds', (t) => {
  /* test */
  const response = formatMonthUTC('2023-02-15 10:32:21')
  t.is(response, '02')
})

test('formatDayUTC_fails', (t) => {
  /* test */
  const response = formatDayUTC(null)
  t.is(response, null)
})

test('formatDayUTC_succeeds', (t) => {
  /* test */
  const response = formatDayUTC('2023-02-15 10:32:21')
  t.is(response, '15')
})

test('formatTimestamp_fails', (t) => {
  /* test */
  const response = formatTimestamp(null)
  t.is(response, null)
})

test('formatTimestamp_succeeds', (t) => {
  /* test */
  const response = formatTimestamp('2023-02-15 10:32:21')
  t.is(response, '2023-02-15 10:32:21')
})

test('formatTimestampUTCLabel_succeeds', (t) => {
  /* test */
  const response = formatTimestampUTCLabel('2023-02-15 09:32:21')
  t.is(response, '2023-02-15 09:32:21 (UTC)')
})

test('formatTimestampUTCLabel_fails', (t) => {
  /* test */
  const response = formatTimestampUTCLabel(null)
  t.is(response, null)
})

test('formatTimestampUTC_fails', (t) => {
  /* test */
  const response = formatTimestampUTC(null)
  t.is(response, null)
})

test('formatTimestampUTC_succeeds', (t) => {
  /* test */
  const response = formatTimestampUTC('2023-02-15 09:32:21')
  t.is(response, '2023-02-15 09:32:21')
})
