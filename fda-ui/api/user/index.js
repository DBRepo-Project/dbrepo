// eslint-disable-next-line camelcase
import jwt_decode from 'jwt-decode'
const axios = require('axios/dist/browser/axios.cjs')
const qs = require('qs')

export function authenticate (clientSecret, username, password) {
  const payload = {
    client_id: 'dbrepo-client',
    username,
    password,
    grant_type: 'password',
    client_secret: clientSecret,
    scope: 'openid roles'
  }
  return axios.post('/api/auth/realms/dbrepo/protocol/openid-connect/token', qs.stringify(payload), {
    headers: { ContentType: 'application/form-data' }
  })
}

export function userinfo (clientSecret, token) {
  return axios.get('/api/auth/realms/dbrepo/protocol/openid-connect/userinfo', {
    headers: {
      Authorization: `Bearer ${token}`,
      ContentType: 'application/form-data'
    }
  })
}

export function refresh (clientSecret, token) {
  const payload = {
    client_id: 'dbrepo-client',
    grant_type: 'refresh_token',
    client_secret: clientSecret,
    refresh_token: token
  }
  return axios.post('/api/auth/realms/dbrepo/protocol/openid-connect/token', qs.stringify(payload), {
    headers: { ContentType: 'application/form-data' }
  })
}

export function tokenToUser (token) {
  const data = jwt_decode(token)
  return {
    id: data.sub,
    firstname: data.given_name || null,
    lastname: data.family_name || null,
    username: data.client_id,
    roles: data.realm_access.roles || []
  }
}

export function tokenToExp (token) {
  const data = jwt_decode(token)
  return new Date(data.exp * 1000)
}

export function tokenToRoles (token) {
  const data = jwt_decode(token)
  return data.realm_access.roles
}
