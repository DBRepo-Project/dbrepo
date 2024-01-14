import Vue from 'vue'
import store from '@/store'
import api from '@/api'
import VueAxios from 'vue-axios'
import AuthenticationMapper from '@/api/authentication.mapper'
import AuthenticationService from '@/api/authentication.service'

/**
 * Attempts to refresh the access token with the refresh token (if not expired).
 * Success: stores the new access-refresh token pair in the store.
 * Error: deletes the access-refresh token pair in the store.
 * https://stackoverflow.com/questions/44985708/axios-request-interceptor-wait-until-ajax-call-finishes
 */
api.interceptors.request.use(config =>
  new Promise((resolve, reject) => {
    const token = store().state.token
    const refreshToken = store().state.refreshToken
    if (!store().state.token || !refreshToken) {
      resolve(config)
    } else if (AuthenticationMapper.isExpiredToken(token)) {
      if (AuthenticationMapper.isExpiredToken(refreshToken)) {
        console.warn('Refresh token is expired: trigger logout of user')
        store().dispatch('logout')
        resolve(config)
      }
      AuthenticationService.authenticateToken(refreshToken)
        .then((response) => {
          store().commit('SET_TOKEN', response.access_token)
          store().commit('SET_REFRESH_TOKEN', response.refresh_token)
          console.debug('new access token expires:', AuthenticationMapper.tokenToExpiryDate(response.access_token))
          config.headers.Authorization = `Bearer ${response.access_token}`
          resolve(config)
        })
        .catch((error) => {
          if (error.response.data.error === 'invalid_grant') {
            store().dispatch('logout')
          }
          reject(error)
        })
    } else {
      config.headers.Authorization = `Bearer ${store().state.token}`
      resolve(config)
    }
  })
)

Vue.use(VueAxios, api)
