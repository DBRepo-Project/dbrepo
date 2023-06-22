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
          console.error('Failed to load identifiers', error)
          reject(error)
        })
    })
  }

  find (id) {
    return this.findAccept(id, 'application/json')
  }

  retrieve (url) {
    return new Promise((resolve, reject) => {
      if (url === null) {
        reject(Error)
      }
      api.get(`/api/identifier/retrieve?url=${url}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const metadata = response.data
          console.debug('response metadata', metadata)
          resolve(metadata)
        })
        .catch((error) => {
          console.error('Failed to load metadata', error)
          reject(error)
        })
    })
  }

  findAccept (id, accept) {
    return new Promise((resolve, reject) => {
      api.get(`/api/pid/${id}`, { headers: { Accept: accept } })
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
          console.error('Failed to create identifier', error)
          Vue.$toast.error(`[${code}] Failed to create identifier: ${message}`)
          reject(error)
        })
    })
  }

  update (id, data) {
    return new Promise((resolve, reject) => {
      api.put(`/api/pid/${id}`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const identifier = response.data
          console.debug('response identifier', identifier)
          resolve(identifier)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to update identifier', error)
          Vue.$toast.error(`[${code}] Failed to update identifier: ${message}`)
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
