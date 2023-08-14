import axios from 'axios'

const baseUrl = `${location.protocol}//${location.host}`

const instance = axios.create({
  timeout: 10000,
  params: {},
  baseURL: baseUrl
})

console.debug('base url:', baseUrl)

export default instance
