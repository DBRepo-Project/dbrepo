const { format } = require('date-fns')
const VueJwtDecode = require('vue-jwt-decode')

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

function isDeveloper (user) {
  if (!user || !user.roles || user.roles.length === 0) {
    return false
  }
  return user.roles.filter(a => a === 'ROLE_DEVELOPER').length === 1
}

function isResearcher (user) {
  if (!user || !user.roles || user.roles.length === 0) {
    return false
  }
  return user.roles.filter(a => a === 'ROLE_RESEARCHER').length === 1
}

function isDataSteward (user) {
  if (!user || !user.roles || user.roles.length === 0) {
    return false
  }
  return user.roles.filter(a => a === 'ROLE_DATA_STEWARD').length === 1
}

function formatUser (user) {
  if (!user) {
    return null
  }
  if (!('firstname' in user) || !('lastname' in user) || user.firstname === null || user.lastname === null) {
    return user?.username
  }
  return user.firstname + ' ' + user.lastname
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

function formatCreators (container) {
  if (!container || !('database' in container) || !('identifier' in container.database) || !container.database.identifier || !('creators' in container.database.identifier) || !container.database.identifier.creators) {
    return null
  }
  const creators = container.database.identifier.creators
  if (creators.length === 0) {
    return formatUser(container.database.creator)
  }
  let str = ''
  for (let i = 0; i < creators.length; i++) {
    /* separator */
    if (creators.length > 1 && i === creators.length - 1) {
      str += ', & '
    } else if (i > 0 && creators.length !== 2) {
      str += ', '
    }
    /* name */
    if (creators[i].firstname) {
      str += (creators[i].firstname.toUpperCase().substring(0, 1) + '., ')
    }
    if (creators[i].lastname) {
      str += creators[i].lastname
    }
  }
  return str
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

function jwtToUser (jwt) {
  // eslint-disable-next-line camelcase
  const { access_token } = jwt
  const data = VueJwtDecode.decode(access_token)
  return {
    id: data.sub,
    firstname: data.given_name,
    lastname: data.family_name,
    username: data.preferred_username,
    theme_dark: data?.theme_dark,
    orcid: data?.orcid,
    titles_before: data?.titles_before,
    titles_after: data?.titles_after,
    email_verified: data.email_verified
  }
}

function isTokenExpired (accessToken) {
  const data = VueJwtDecode.decode(accessToken)
  const exp = new Date(data.exp)
  return exp <= new Date()
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
  formatCreators,
  isDeveloper,
  isResearcher,
  isDataSteward,
  jwtToUser,
  isTokenExpired
}
