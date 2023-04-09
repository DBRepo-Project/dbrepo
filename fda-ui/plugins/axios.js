import Vue from 'vue'
import axios from 'axios'
import api from '../config'
import updateToken from '../server-middleware/update-token'

const instance = axios.create({
  baseURL: api,
  timeout: 10000,
  params: {}
})

instance.interceptors.request.use(async (config) => {
  const token = await updateToken()
  config.headers.common.Authorization = `Bearer ${token}`
  return config
}, function (error) {
  // Do something with request error
  return Promise.reject(error)
})

Vue.use(instance)
