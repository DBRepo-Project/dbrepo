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

/**
 * https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier
 * @param str The ORCID
 * @returns {boolean} True if ORCID is valid, false otherwise
 */
function isValidOrcid (str) {
  if (str == null) {
    return false
  }
  if (str.length !== 19) {
    return false
  }
  let total = 0
  for (let i = 0; i < str.length; i++) {
    const digit = parseInt(str.charAt(i))
    if (isNaN(digit)) {
      continue
    }
    total = (total + digit) * 2
  }
  const remainder = total % 11
  const result = (12 - remainder) % 11
  const check = result === 10 ? 'X' : result.toString()
  return str.substr(18) === check
}

function formatUser (user) {
  if (user.firstname && user.lastname) {
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
  return user.username
}

function formatDateUTC (str) {
  if (str === null) {
    return null
  }
  const date = new Date(str).toISOString().slice(0, -1)
  return format(new Date(date), 'yyyy-MM-dd')
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
  isValidOrcid,
  formatUser
}
