import Vue from 'vue'
import axios from 'axios'

class MiddlewareService {
  upload (name, file) {
    return new Promise((resolve, reject) => {
      const data = new FormData()
      data.append(name, file)
      axios.post('/server-middleware/upload', data, { headers: { 'Content-Type': 'multipart/form-data' } })
        .then((response) => {
          const file = response.data
          console.debug('response file', file)
          resolve(file)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to create database', error)
          Vue.$toast.error(`[${code}] Failed to create database: ${message}`)
          reject(error)
        })
    })
  }

  buildQuery (data) {
    return new Promise((resolve, reject) => {
      axios.post('/server-middleware/query/build', data, { headers: { 'Content-Type': 'application/json' } })
        .then((response) => {
          const file = response.data
          console.debug('response query', file)
          resolve(file)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to build query', error)
          Vue.$toast.error(`[${code}] Failed to build query: ${message}`)
          reject(error)
        })
    })
  }
}

export default new MiddlewareService()
