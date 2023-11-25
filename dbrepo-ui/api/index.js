import axios from 'axios'
import Vue from 'vue'

const baseUrl = `${location.protocol}//${location.host}`

console.debug('base url', baseUrl)

const instance = axios.create({
  timeout: 10000,
  params: {},
  baseURL: baseUrl
})

function displayError (error, warning) {
  const { code, message } = error.response.data
  console.error(warning, error)
  Vue.$toast.error(`[${code}] ${warning}: ${message}`)
}

export default instance
export {
  displayError
}
