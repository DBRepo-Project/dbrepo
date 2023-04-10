import Vue from 'vue'
import api from '@/api'
import AuthenticationService from '@/api/authentication.service'
import { getRefreshToken, getToken, setRefreshToken, setToken } from '@/server-middleware/store'
import jwtDecode from 'jwt-decode'

api.interceptors.request.use((config) => {
  const token = getToken()
  if (!token) {
    return config
  }
  const { exp } = jwtDecode(token)
  if (new Date(exp) <= new Date()) {
    /* token expired */
    const refreshToken = getRefreshToken()
    const { exp2 } = jwtDecode(refreshToken)
    if (new Date(exp2) <= new Date()) {
      /* refresh token expired */
      setToken(null)
      setRefreshToken(null)
    }
    AuthenticationService.authenticateToken(refreshToken)
    return config
  }
  console.debug('interceptor inject authorization header', exp)
  config.headers.Authorization = `Bearer ${token}`
  return config
})

Vue.use(api)
