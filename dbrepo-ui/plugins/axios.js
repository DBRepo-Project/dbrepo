import Vue from 'vue'
import store from '@/store'
import api from '@/api'
import VueAxios from 'vue-axios'

api.interceptors.request.use((config) => {
  const token = store().state.token
  if (!token) {
    return config
  }
  console.debug('interceptor inject authorization header for url', config.url)
  config.headers.Authorization = `Bearer ${store().state.token}`
  return config
})

Vue.use(VueAxios, api)
