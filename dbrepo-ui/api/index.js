import axios from 'axios'
import Vue from 'vue'

const baseUrl = `${location.protocol}//${location.host}`

console.debug('base url', baseUrl)

const instance = axios.create({
  timeout: 10000,
  params: {},
  baseURL: baseUrl
})

function displayError (altMessage, error) {
  const { code, message } = error.response.data
  if (code && message) {
    console.error(error)
    Vue.$toast.error(message)
    return
  }
  console.error(altMessage, error)
  Vue.$toast.error(`[${error.code}] ${altMessage}: ${error.message}`)
}

export default instance
export {
  displayError
}
