import Vue from 'vue'
import api from '@/api'

class IdentifierService {
  findAll (databaseId, type) {
    return new Promise((resolve, reject) => {
      const delim = databaseId !== null && type !== null ? '&' : '?'
      api.get(`/api/identifier${databaseId !== null ? `?dbid=${databaseId}` : ''}${type !== null ? `${delim}type=${type}` : ''}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const identifiers = response.data
          console.debug('response identifiers', identifiers)
          resolve(identifiers)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load identifiers', error)
          Vue.$toast.error(`[${code}] Failed to load identifiers: ${message}`)
          reject(error)
        })
    })
  }

  findOne (id) {
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

  export (pid) {
    return new Promise((resolve, reject) => {
      api.get(`/api/pid/${pid}`, { headers: { Accept: 'text/xml' } })
        .then((response) => {
          const identifier = response.data
          console.debug('response identifier', identifier)
          resolve(identifier)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to export identifier', error)
          Vue.$toast.error(`[${code}] Failed to export identifier: ${message}`)
          reject(error)
        })
    })
  }

  delete (pid) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/pid/${pid}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to delete identifier', error)
          Vue.$toast.error(`[${code}] Failed to delete identifier: ${message}`)
          reject(error)
        })
    })
  }
}

export default new IdentifierService()
