import Vue from 'vue'
import api from '@/api'

class QueryService {
  findAll (id, databaseId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/${databaseId}/query`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const queries = response.data
          console.debug('response queries', queries)
          resolve(queries)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load queries', error)
          Vue.$toast.error(`[${code}] Failed to load queries: ${message}`)
          reject(error)
        })
    })
  }

  findOne (id, databaseId, queryId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/${databaseId}/query/${queryId}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const query = response.data
          console.debug('response query', query)
          resolve(query)
        }).catch((error) => {
          const { code, message } = error
          console.error('Failed to load query', error)
          Vue.$toast.error(`[${code}] Failed to load query: ${message}`)
          reject(error)
        })
    })
  }

  persist (id, databaseId, queryId) {
    return new Promise((resolve, reject) => {
      api.put(`/api/container/${id}/database/${databaseId}/query/${queryId}`, {}, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const query = response.data
          console.debug('response query', query)
          resolve(query)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to persist query', error)
          Vue.$toast.error(`[${code}] Failed to persist query: ${message}`)
          reject(error)
        })
    })
  }

  export (id, databaseId, queryId) {
    return new Promise((resolve, reject) => {
      api.put(`/api/container/${id}/database/${databaseId}/query/${queryId}/export`, {}, { headers: { Accept: 'text/csv' } })
        .then((response) => {
          const query = response.data
          console.debug('response export', query)
          resolve(query)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to export query', error)
          Vue.$toast.error(`[${code}] Failed to export query: ${message}`)
          reject(error)
        })
    })
  }
}

export default new QueryService()
