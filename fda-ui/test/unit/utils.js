const test = require('ava')
const {
  isNonNegativeInteger,
  isDeveloper,
  isResearcher,
  isDataSteward,
  formatUser,
  formatDateUTC,
  formatYearUTC,
  formatMonthUTC,
  formatDayUTC,
  formatTimestamp,
  formatCreators,
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

test('isDeveloper_succeeds', (t) => {
  const user = { roles: ['ROLE_DEVELOPER'] }
  /* test */
  const response = isDeveloper(user)
  t.is(response, true)
})

test('isDeveloper_fails', (t) => {
  const user = { roles: [] }
  /* test */
  const response = isDeveloper(user)
  t.is(response, false)
})

test('isDeveloper_otherRole_fails', (t) => {
  const user = { roles: ['ROLE_RESEARCHER'] }
  /* test */
  const response = isDeveloper(user)
  t.is(response, false)
})

test('isResearcher_succeeds', (t) => {
  const user = { roles: ['ROLE_RESEARCHER'] }
  /* test */
  const response = isResearcher(user)
  t.is(response, true)
})

test('isResearcher_fails', (t) => {
  const user = { roles: [] }
  /* test */
  const response = isResearcher(user)
  t.is(response, false)
})

test('isResearcher_otherRole_fails', (t) => {
  const user = { roles: ['ROLE_DEVELOPER'] }
  /* test */
  const response = isResearcher(user)
  t.is(response, false)
})

test('isDataSteward_succeeds', (t) => {
  const user = { roles: ['ROLE_DATA_STEWARD'] }
  /* test */
  const response = isDataSteward(user)
  t.is(response, true)
})

test('isDataSteward_fails', (t) => {
  const user = { roles: [] }
  /* test */
  const response = isDataSteward(user)
  t.is(response, false)
})

test('isDataSteward_otherRole_fails', (t) => {
  const user = { roles: ['ROLE_DEVELOPER'] }
  /* test */
  const response = isDataSteward(user)
  t.is(response, false)
})

test('formatUser_fails', (t) => {
  const user = null
  /* test */
  const response = formatUser(user)
  t.is(response, null)
})

test('formatUser_usernameMissing_fails', (t) => {
  const user = { lastname: null, firstname: null }
  /* test */
  const response = formatUser(user)
  t.is(response, null)
})

test('formatUser_succeeds', (t) => {
  const user = { lastname: null, firstname: null, username: 'mweise' }
  /* test */
  const response = formatUser(user)
  t.is(response, 'mweise')
})

test('formatUser_firstnameLastname_succeeds', (t) => {
  const user = { lastname: 'Martin', firstname: 'Weise', username: 'mweise' }
  /* test */
  const response = formatUser(user)
  t.is(response, 'Weise Martin')
})

test('formatUser_titles_succeeds', (t) => {
  const user = { lastname: 'Martin', firstname: 'Weise', username: 'mweise', titles_before: 'Dipl.-Ing.', titles_after: 'BSc' }
  /* test */
  const response = formatUser(user)
  t.is(response, 'Dipl.-Ing. Weise Martin BSc')
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

test('formatCreators_containerMissing_fails', (t) => {
  const container = null
  /* test */
  const response = formatCreators(container)
  t.is(response, null)
})

test('formatCreators_databaseMissing_fails', (t) => {
  const container = { }
  /* test */
  const response = formatCreators(container)
  t.is(response, null)
})

test('formatCreators_identifierMissing_fails', (t) => {
  const container = { database: { } }
  /* test */
  const response = formatCreators(container)
  t.is(response, null)
})

test('formatCreators_creatorsMissing_fails', (t) => {
  const container = { database: { identifier: { } } }
  /* test */
  const response = formatCreators(container)
  t.is(response, null)
})

test('formatCreators_identifierNull_fails', (t) => {
  const container = { database: { identifier: null } }
  /* test */
  const response = formatCreators(container)
  t.is(response, null)
})

test('formatCreators_creatorsEmpty_fails', (t) => {
  const container = { database: { identifier: { creators: [] } } }
  /* test */
  const response = formatCreators(container)
  t.is(response, null)
})

test('formatCreators_single_succeeds', (t) => {
  const container = { database: { identifier: { creators: [{ firstname: 'Martin', lastname: 'Weise' }] } } }
  /* test */
  const response = formatCreators(container)
  t.is(response, 'M., Weise')
})

test('formatCreators_double_succeeds', (t) => {
  const container = { database: { identifier: { creators: [{ firstname: 'Martin', lastname: 'Weise' }, { firstname: 'Tobias', lastname: 'Grantner' }] } } }
  /* test */
  const response = formatCreators(container)
  t.is(response, 'M., Weise, & T., Grantner')
})

test('formatCreators_multiple_succeeds', (t) => {
  const container = { database: { identifier: { creators: [{ firstname: 'Martin', lastname: 'Weise' }, { firstname: 'Tobias', lastname: 'Grantner' }, { firstname: 'Josef', lastname: 'Taha' }] } } }
  /* test */
  const response = formatCreators(container)
  t.is(response, 'M., Weise, T., Grantner, & J., Taha')
})

test('formatTimestampUTCLabel_succeeds', (t) => {
  /* test */
  const response = formatTimestampUTCLabel('2023-02-15 10:32:21')
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
  const response = formatTimestampUTC('2023-02-15 10:32:21')
  t.is(response, '2023-02-15 09:32:21')
})
