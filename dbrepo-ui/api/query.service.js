import api, { displayError } from '@/api'

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
          displayError('Failed to load queries', error)
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
          displayError('Failed to load query', error)
          reject(error)
        })
    })
  }

  persist (databaseId, queryId, persist) {
    return new Promise((resolve, reject) => {
      api.put(`/api/database/${databaseId}/query/${queryId}`, { persist }, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const query = response.data
          console.debug('response query', query)
          resolve(query)
        })
        .catch((error) => {
          displayError('Failed to persist query', error)
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
          displayError('Failed to import csv to table', error)
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
          displayError('Failed to insert tuple', error)
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
          displayError('Failed to update tuple', error)
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
          displayError('Failed to export subset', error)
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
          displayError('Failed to export metadata', error)
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
          displayError('Failed to execute query', error)
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
          displayError('Failed to re-execute query', error)
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
          displayError('Failed to re-execute query and count results', error)
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
          displayError('Failed to re-execute view', error)
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
          displayError('Failed to re-execute view and count results', error)
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
          displayError('Failed to find view', error)
          reject(error)
        })
    })
  }
}

export default new QueryService()
