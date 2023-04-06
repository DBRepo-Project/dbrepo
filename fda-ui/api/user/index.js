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
    scope: 'openid profile roles'
  }
  return axios.post('/api/auth/realms/dbrepo/protocol/openid-connect/token', qs.stringify(payload), {
    headers: { ContentType: 'application/form-data' }
  })
}

export function updateUser (token, userId, data) {
  return axios.put(`/api/user/${userId}`, data, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}

export function updateUserPassword (token, userId, password) {
  const payload = {
    password
  }
  return axios.put(`/api/user/${userId}/password`, payload, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}

export function toggleUserTheme (token, userId, themeDark) {
  const payload = {
    theme_dark: themeDark
  }
  return axios.put(`/api/user/${userId}/theme`, payload, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}

export function findUser (token) {
  const user = tokenToUser(token)
  return axios.get(`/api/user/${user.id}`, {
    headers: {
      Authorization: `Bearer ${token}`
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
  if (!data) {
    return new Date()
  }
  return new Date(data.exp * 1000)
}

export function tokenToRoles (token) {
  const data = jwt_decode(token)
  if (!data) {
    return []
  }
  return data.realm_access.roles || []
}

export function getThemeDark (user) {
  if (!user || !user.attributes || user.attributes.filter(a => a.name === 'theme_dark').length === 0) {
    return false
  }
  return user.attributes.filter(a => a.name === 'theme_dark')[0].value === 'true'
}
