import Vue from 'vue'
import store from '@/store'
import qs from 'qs'
import UserMapper from '@/api/user.mapper'
import axios from 'axios'
import { clientSecret } from '@/config'

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
      client_id: 'dbrepo-client',
      username,
      password,
      grant_type: 'password',
      client_secret: clientSecret,
      scope: 'openid profile roles attributes'
    }
    if (!username) {
      throw new Error('parameter username is empty')
    }
    if (!password) {
      throw new Error('parameter password is empty')
    }
    if (!clientSecret) {
      throw new Error('parameter clientSecret is empty')
    }
    return this._authenticate(payload)
  }

  authenticateToken (refreshToken) {
    const payload = {
      client_id: 'dbrepo-client',
      grant_type: 'refresh_token',
      client_secret: clientSecret,
      refresh_token: refreshToken
    }
    if (!refreshToken) {
      throw new Error('parameter refreshToken is empty')
    }
    if (!clientSecret) {
      throw new Error('parameter clientSecret is empty')
    }
    return this._authenticate(payload)
  }

  _authenticate (payload) {
    return new Promise((resolve, reject) => {
      axios.post('/api/auth/realms/dbrepo/protocol/openid-connect/token', qs.stringify(payload), {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }).then((response) => {
        const authentication = response.data
        // eslint-disable-next-line camelcase
        const { access_token, refresh_token } = authentication
        console.debug('response authenticate', authentication)
        store().commit('SET_TOKEN', access_token)
        store().commit('SET_REFRESH_TOKEN', refresh_token)
        const user = UserMapper.tokenToUser(access_token)
        store().commit('SET_USER', user)
        resolve(authentication)
      }).catch((error) => {
        console.error('Failed to authenticate', error)
        const { code, message, response } = error
        const { status } = response
        if (status === 401) {
          Vue.$toast.error('Invalid username-password combination.')
        } else {
          Vue.$toast.error(`[${code}] Failed to authenticate: ${message}`)
        }
        reject(error)
      })
    })
  }
}

export default new AuthenticationService()
