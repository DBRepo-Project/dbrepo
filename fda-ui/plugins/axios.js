import Vue from 'vue'
import api from '@/api/api'

api.interceptors.request.use((config) => {
  console.debug('loading token', Vue.$keycloak.token, Vue.$keycloak.refreshToken)
  Vue.$keycloak.updateToken(70)
    .then(() => {
      const token = String(Vue.$keycloak.token)
      config.headers.common.Authorization = `Bearer ${token}`
      return config
    })
    .catch((error) => {
      console.error('Failed to update token', error)
    })
}, function (error) {
  // Do something with request error
  return Promise.reject(error)
})

Vue.use(api)
