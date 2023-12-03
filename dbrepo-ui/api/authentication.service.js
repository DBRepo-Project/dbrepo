import Vue from 'vue'
import store from '@/store'
import qs from 'qs'
import UserMapper from '@/api/user.mapper'
import axios from 'axios'

/**
 * Service class for interaction with Authentication Service in the back end.
 *
 * @author Martin Weise
 * @description This service needs **important** its own axios instance for calls to the back end, otherwise it creates
 * an infinite loop with the interceptors.
 */
class AuthenticationService {
  /**
   * Authenticates a user in the back end with their username and password credential.
   * @param username The username.
   * @param password The password credential.
   */
  authenticatePlain (username, password) {
    const payload = {
      client_id: store().state.clientId,
      client_secret: store().state.clientSecret,
      username,
      password,
      grant_type: 'password',
      scope: 'roles'
    }
    if (!username) {
      return new Promise((resolve, reject) => {
        Vue.$toast.warning('[client-error] Parameter username is empty')
        reject(new Error('parameter username is empty'))
      })
    }
    if (!password) {
      return new Promise((resolve, reject) => {
        Vue.$toast.warning('[client-error] Parameter password is empty')
        reject(new Error('parameter password is empty'))
      })
    }
    if (!payload.client_secret) {
      return new Promise((resolve, reject) => {
        Vue.$toast.warning('[client-error] Parameter clientSecret is empty')
        reject(new Error('parameter clientSecret is empty'))
      })
    }
    return this._authenticate(payload)
  }

  authenticateToken (refreshToken) {
    const payload = {
      client_id: store().state.clientId,
      client_secret: store().state.clientSecret,
      grant_type: 'refresh_token',
      refresh_token: refreshToken
    }
    if (!refreshToken) {
      return new Promise((resolve, reject) => {
        Vue.$toast.warning('[client-error] Parameter refreshToken is empty')
        reject(new Error('parameter refreshToken is empty'))
      })
    }
    if (!payload.client_secret) {
      return new Promise((resolve, reject) => {
        Vue.$toast.warning('[client-error] Parameter clientSecret is empty')
        reject(new Error('parameter clientSecret is empty'))
      })
    }
    return this._authenticate(payload)
  }

  _authenticate (payload) {
    return new Promise((resolve, reject) => {
      const instance = axios.create({
        timeout: 10000,
        params: {},
        baseURL: `${location.protocol}//${location.host}`,
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      })
      instance.post('/api/auth/realms/dbrepo/protocol/openid-connect/token', qs.stringify(payload))
        .then((response) => {
          const authentication = response.data
          // eslint-disable-next-line camelcase
          const { access_token, refresh_token } = authentication
          store().commit('SET_TOKEN', access_token)
          store().commit('SET_REFRESH_TOKEN', refresh_token)
          store().commit('SET_ROLES', UserMapper.tokenToRoles(access_token))
          resolve(authentication)
        }).catch((error) => {
          console.error('Failed to authenticate', error)
          const { response } = error
          const { status } = response
          if (status === 401) {
            Vue.$toast.error('Invalid username-password combination.')
          }
          reject(error)
        })
    })
  }
}

export default new AuthenticationService()
