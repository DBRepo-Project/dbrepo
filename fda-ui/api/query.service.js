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

  importCsv (id, databaseId, tableId, data) {
    return new Promise((resolve, reject) => {
      api.post(`/api/container/${id}/database/${databaseId}/table/${tableId}/data/import`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const table = response.data
          console.debug('response table', table)
          resolve(table)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to import csv to table', error)
          Vue.$toast.error(`[${code}] Failed to import csv to table: ${message}`)
          reject(error)
        })
    })
  }

  insertTuple (id, databaseId, tableId, data) {
    return new Promise((resolve, reject) => {
      api.post(`/api/container/${id}/database/${databaseId}/table/${tableId}/data`, data, { headers: { Accept: 'text/csv' } })
        .then((response) => {
          const tuple = response.data
          console.debug('response insert tuple', tuple)
          resolve(tuple)
        })
        .catch((error) => {
          const { code, message, response } = error
          const { status } = response
          if (status === 423) {
            console.error('Database failed to accept tuple', error)
            Vue.$toast.error(`Database failed to accept tuple: ${message}`)
          } else {
            console.error('Failed to insert tuple', error)
            Vue.$toast.error(`[${code}] Failed to insert tuple: ${message}`)
          }
          reject(error)
        })
    })
  }

  updateTuple (id, databaseId, tableId, data) {
    return new Promise((resolve, reject) => {
      api.put(`/api/container/${id}/database/${databaseId}/table/${tableId}/data`, data, { headers: { Accept: 'text/csv' } })
        .then((response) => {
          const tuple = response.data
          console.debug('response update tuple', tuple)
          resolve(tuple)
        })
        .catch((error) => {
          const { code, message, response } = error
          const { status } = response
          if (status === 423) {
            console.error('Database failed to accept tuple', error)
            Vue.$toast.error(`Database failed to accept tuple: ${message}`)
          } else {
            console.error('Failed to update tuple', error)
            Vue.$toast.error(`[${code}] Failed to update tuple: ${message}`)
          }
          reject(error)
        })
    })
  }

  exportSubset (id, databaseId, queryId) {
    return new Promise((resolve, reject) => {
      api.put(`/api/container/${id}/database/${databaseId}/query/${queryId}/export`, {}, { headers: { Accept: 'text/csv' } })
        .then((response) => {
          resolve(response.data)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to export query', error)
          Vue.$toast.error(`[${code}] Failed to export query: ${message}`)
          reject(error)
        })
    })
  }

  exportMetadata (id, mime) {
    return new Promise((resolve, reject) => {
      api.get(`/api/pid/${id}`, { headers: { Accept: mime } })
        .then((response) => {
          resolve(response.data)
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
