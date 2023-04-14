import Vue from 'vue'
import axios from 'axios'

class MiddlewareService {
  upload (file) {
    return new Promise((resolve, reject) => {
      const data = new FormData()
      data.append('file', file)
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
      axios.post('/server-middleware/query/build', data, { headers: { 'Content-Type': 'multipart/form-data' } })
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
}

export default new MiddlewareService()
