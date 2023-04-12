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
  if (new Date(exp) <= new Date()) {
    /* token expired */
    const refreshToken = store().state.refreshToken
    const { exp2 } = jwtDecode(refreshToken)
    if (new Date(exp2) <= new Date()) {
      /* refresh token expired */
      store().commit('SET_TOKEN', null)
      store().commit('SET_REFRESH_TOKEN', null)
      console.warn('Refresh token expired')
    }
    AuthenticationService.authenticateToken(refreshToken)
      .then(() => {
        return config
      })
  }
  console.debug('interceptor inject authorization header')
  config.headers.Authorization = `Bearer ${token}`
  return config
})

Vue.use(VueAxios, api)
