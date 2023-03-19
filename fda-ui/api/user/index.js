const axios = require('axios/dist/browser/axios.cjs')
const qs = require('qs')
const { clientSecret } = require('@/config')
function authenticate (username, password) {
  const payload = {
    client_id: 'dbrepo-client',
    username,
    password,
    grant_type: 'password',
    client_secret: clientSecret,
    scope: 'openid'
  }
  console.debug('===>', clientSecret)
  return axios.post('/api/auth/realms/dbrepo/protocol/openid-connect/token', qs.stringify(payload), {
    headers: { ContentType: 'application/form-data' }
  })
}

module.exports = {
  authenticate
}
