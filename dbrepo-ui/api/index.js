import axios from 'axios'

const baseUrl = `${location.protocol}//${location.host}`

console.debug('base url', baseUrl)

const instance = axios.create({
  timeout: 10000,
  params: {},
  baseURL: baseUrl
})

export default instance
