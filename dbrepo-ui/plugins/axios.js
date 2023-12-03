import Vue from 'vue'
import store from '@/store'
import api from '@/api'
import AuthenticationService from '@/api/authentication.service'
import jwtDecode from 'jwt-decode'
import VueAxios from 'vue-axios'

api.interceptors.request.use((config) => {
  const token = store().state.token
  if (!token) {
    return config
  }
  const { exp } = jwtDecode(token)
  let accessTokenExpiryDate = new Date(exp * 1000)
  if (accessTokenExpiryDate <= Date.now()) {
    /* token expired */
    console.warn('access token has expired:', accessTokenExpiryDate)
    const refreshToken = store().state.refreshToken
    const refreshTokenExpiryDate = new Date(jwtDecode(refreshToken).exp * 1000)
    if (refreshTokenExpiryDate <= Date.now()) {
      /* refresh token expired */
      console.error('Refresh token expired')
      store().commit('SET_TOKEN', null)
      store().commit('SET_REFRESH_TOKEN', null)
      return config
    }
    AuthenticationService.authenticateToken(refreshToken)
      .then((response) => {
        accessTokenExpiryDate = new Date(jwtDecode(response.access_token).exp * 1000)
        console.info('Successfully requested a new access token')
        console.debug('new access token expires:', accessTokenExpiryDate)
        console.debug('attach access token to intercepted request:', config.url)
        config.headers.Authorization = `Bearer ${response.access_token}`
        return config
      })
      .finally(() => {
        return config
      })
  }
  // console.debug('interceptor inject authorization header for url', config.url)
  config.headers.Authorization = `Bearer ${token}`
  return config
})

Vue.use(VueAxios, api)
