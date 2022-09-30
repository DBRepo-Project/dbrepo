const { format } = require('date-fns')

function notEmpty (str) {
  return typeof str === 'string' && str.trim().length > 0
}

/**
 * From https://stackoverflow.com/questions/10834796/validate-that-a-string-is-a-positive-integer

 Tests:

 "0"                     : true
 "23"                    : true
 "-10"                   : false
 "10.30"                 : false
 "-40.1"                 : false
 "string"                : false
 "1234567890"            : true
 "129000098131766699.1"  : false
 "-1e7"                  : false
 "1e7"                   : true
 "1e10"                  : false
 "1edf"                  : false
 " "                     : false
 ""                      : false
 */
function isNonNegativeInteger (str) {
  return str >>> 0 === parseFloat(str)
}

function formatUser (user) {
  if (user.firstname === undefined || user.lastname === undefined) {
    return user.username
  }
  let name = ''
  if (user.titles_before) {
    name += user.titles_before + ' '
  }
  name += user.firstname + ' ' + user.lastname
  if (user.titles_after) {
    name += ' ' + user.titles_after
  }
  return name
}

function padLeft (str, padString, length) {
  while (str.length < length) {
    str = padString + str
  }
  return str
}

function formatDateUTC (str) {
  if (str === null) {
    return null
  }
  const date = new Date(str).toISOString().slice(0, -1)
  return format(new Date(date), 'yyyy-MM-dd')
}

function formatYearUTC (str) {
  if (str === null) {
    return null
  }
  const date = new Date(str).toISOString().slice(0, -1)
  return format(new Date(date), 'yyyy')
}

function formatMonthUTC (str) {
  if (str === null) {
    return null
  }
  const date = new Date(str).toISOString().slice(0, -1)
  return format(new Date(date), 'MM')
}

function formatDayUTC (str) {
  if (str === null) {
    return null
  }
  const date = new Date(str).toISOString().slice(0, -1)
  return format(new Date(date), 'dd')
}

function formatTimestamp (str) {
  if (str === null) {
    return null
  }
  return format(new Date(str), 'yyyy-MM-dd HH:mm:ss')
}

function formatTimestampUTCLabel (str) {
  if (str === null) {
    return null
  }
  const date = new Date(str).toISOString().slice(0, -1)
  return format(new Date(date), 'yyyy-MM-dd HH:mm:ss') + ' (UTC)'
}

function formatTimestampUTC (str) {
  if (str === null) {
    return null
  }
  const date = new Date(str).toISOString().slice(0, -1)
  return format(new Date(date), 'yyyy-MM-dd HH:mm:ss')
}

module.exports = {
  notEmpty,
  formatTimestamp,
  formatTimestampUTC,
  formatTimestampUTCLabel,
  formatDateUTC,
  isNonNegativeInteger,
  formatUser,
  formatYearUTC,
  formatMonthUTC,
  formatDayUTC,
  padLeft
}
