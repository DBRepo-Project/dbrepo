import Vue from 'vue'
import axios from 'axios'

class MiddlewareService {
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

  upload (file) {
    return new Promise((resolve, reject) => {
      const formData = new FormData()
      formData.append('file', file, file.name)
      axios.post('/server-middleware/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
        .then((response) => {
          const metadata = response.data
          console.debug('response metadata', metadata)
          resolve(metadata)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to upload file', error)
          Vue.$toast.error(`[${code}] Failed to upload file: ${message}`)
          reject(error)
        })
    })
  }
}

export default new MiddlewareService()
