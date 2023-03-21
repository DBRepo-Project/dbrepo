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
    scope: 'openid'
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
    firstname: data.given_name,
    lastname: data.family_name,
    username: data.preferred_username,
    theme_dark: data.theme_dark,
    titles_before: data.titles_before,
    titles_after: data.titles_after,
    affiliation: data.affiliation,
    orcid: data.orcid,
    email_verified: data.email_verified
  }
}
