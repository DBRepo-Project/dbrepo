import Vue from 'vue'
import api from '@/api'

class IdentifierService {
  findPid (id) {
    return new Promise((resolve, reject) => {
      api.get(`/api/pid/${id}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const identifier = response.data
          console.debug('response identifier', identifier)
          resolve(identifier)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load identifier', error)
          Vue.$toast.error(`[${code}] Failed to load identifier: ${message}`)
          reject(error)
        })
    })
  }

  create (data) {
    return new Promise((resolve, reject) => {
      api.post('/api/identifier', data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const identifier = response.data
          console.debug('response identifier', identifier)
          resolve(identifier)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load identifier', error)
          Vue.$toast.error(`[${code}] Failed to load identifier: ${message}`)
          reject(error)
        })
    })
  }
}

export default new IdentifierService()
