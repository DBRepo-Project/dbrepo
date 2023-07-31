import Vue from 'vue'
import api from '@/api'

class QueryService {
  findAll (databaseId, persisted) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}/query${persisted === null ? '' : `?persisted=${persisted}`}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const queries = response.data
          console.debug('response queries', queries)
          resolve(queries)
        })
        .catch((error) => {
          console.error('Failed to load queries', error)
          reject(error)
        })
    })
  }

  findOne (databaseId, queryId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}/query/${queryId}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const query = response.data
          console.debug('response query', query)
          resolve(query)
        }).catch((error) => {
          console.error('Failed to load query', error)
          reject(error)
        })
    })
  }

  persist (databaseId, queryId) {
    return new Promise((resolve, reject) => {
      api.put(`/api/database/${databaseId}/query/${queryId}`, {}, { headers: { Accept: 'application/json' } })
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

  importCsv (databaseId, tableId, data) {
    return new Promise((resolve, reject) => {
      api.post(`/api/database/${databaseId}/table/${tableId}/data/import`, data, { headers: { Accept: 'application/json' } })
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

  insertTuple (databaseId, tableId, data) {
    return new Promise((resolve, reject) => {
      api.post(`/api/database/${databaseId}/table/${tableId}/data`, { data }, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const tuple = response.data
          console.debug('response insert tuple', tuple)
          resolve(tuple)
        })
        .catch((error) => {
          const { response } = error
          const { status, data } = response
          const { message } = data
          if (status === 423) {
            console.error('Database failed to accept tuple', error)
            Vue.$toast.error(message)
          } else {
            console.error('Failed to insert tuple', error)
            Vue.$toast.error(message)
          }
          reject(error)
        })
    })
  }

  updateTuple (databaseId, tableId, data) {
    return new Promise((resolve, reject) => {
      api.put(`/api/database/${databaseId}/table/${tableId}/data`, data, { headers: { Accept: 'text/csv' } })
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

  exportSubset (databaseId, queryId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}/query/${queryId}/export`, { headers: { Accept: 'text/csv' } })
        .then((response) => {
          const subset = response.data
          console.debug('response subset', subset)
          resolve(subset)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to export query', error)
          Vue.$toast.error(`[${code}] Failed to export query: ${message}`)
          reject(error)
        })
    })
  }

  exportMetadata (pid, mime) {
    return new Promise((resolve, reject) => {
      api.get(`/api/pid/${pid}`, { headers: { Accept: mime } })
        .then((response) => {
          const metadata = response.data
          console.debug('response metadata', metadata)
          resolve(metadata)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to export metadata', error)
          Vue.$toast.error(`[${code}] Failed to export metadata: ${message}`)
          reject(error)
        })
    })
  }

  execute (databaseId, data, page, size) {
    return new Promise((resolve, reject) => {
      api.post(`/api/database/${databaseId}/query?page=${page}&size=${size}`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const result = response.data
          console.debug('response result', result)
          resolve(result)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to execute statement', error)
          Vue.$toast.error(`[${code}] Failed to execute statement: ${message}`)
          reject(error)
        })
    })
  }

  reExecuteQuery (databaseId, queryId, page, size) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}/query/${queryId}/data?page=${page}&size=${size}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const result = response.data
          console.debug('response result', result)
          resolve(result)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to re-execute query', error)
          Vue.$toast.error(`[${code}] Failed to re-execute query: ${message}`)
          reject(error)
        })
    })
  }

  reExecuteQueryCount (databaseId, queryId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}/query/${queryId}/data/count`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const count = response.data
          console.debug('response count', count)
          resolve(count)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to re-execute query count', error)
          Vue.$toast.error(`[${code}] Failed to re-execute query count: ${message}`)
          reject(error)
        })
    })
  }

  reExecuteView (databaseId, viewId, page, size) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}/view/${viewId}/data?page=${page}&size=${size}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const result = response.data
          console.debug('response result', result)
          resolve(result)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to re-execute view', error)
          Vue.$toast.error(`[${code}] Failed to re-execute view: ${message}`)
          reject(error)
        })
    })
  }

  reExecuteViewCount (databaseId, viewId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}/view/${viewId}/data/count`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const count = response.data
          console.debug('response count', count)
          resolve(count)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to re-execute view count', error)
          Vue.$toast.error(`[${code}] Failed to re-execute view count: ${message}`)
          reject(error)
        })
    })
  }

  findView (databaseId, viewId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}/view/${viewId}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const view = response.data
          console.debug('response view', view)
          resolve(view)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to find view', error)
          Vue.$toast.error(`[${code}] Failed to find view: ${message}`)
          reject(error)
        })
    })
  }
}

export default new QueryService()
