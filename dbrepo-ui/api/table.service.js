import Vue from 'vue'
import api from '@/api'

/**
 * Service class for interaction with Table Service in the back end.
 *
 * @author Martin Weise
 */
class TableService {
  findAll (id, databaseId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/${databaseId}/table`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const tables = response.data
          console.debug('response tables', tables)
          resolve(tables)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load tables', error)
          Vue.$toast.error(`[${code}] Failed to load tables: ${message}`)
          reject(error)
        })
    })
  }

  findOne (id, databaseId, tableId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/${databaseId}/table/${tableId}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const table = response.data
          console.debug('response table', table)
          resolve(table)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load table', error)
          Vue.$toast.error(`[${code}] Failed to load table: ${message}`)
          reject(error)
        })
    })
  }

  updateColumn (id, databaseId, tableId, columnId, data) {
    return new Promise((resolve, reject) => {
      api.put(`/api/container/${id}/database/${databaseId}/table/${tableId}/column/${columnId}`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const column = response.data
          console.debug('response column', column)
          resolve(column)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to update column', error)
          Vue.$toast.error(`[${code}] Failed to update column: ${message}`)
          reject(error)
        })
    })
  }

  data (id, databaseId, tableId, page, size, timestamp) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/${databaseId}/table/${tableId}/data?page=${page}&size=${size}&timestamp=${timestamp}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const data = response.data
          console.debug('response data', data)
          resolve(data)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load table data', error)
          Vue.$toast.error(`[${code}] Failed to load table data: ${message}`)
          reject(error)
        })
    })
  }

  dataCount (id, databaseId, tableId, timestamp) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/${databaseId}/table/${tableId}/data/count?timestamp=${timestamp}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const count = response.data
          console.debug('response count', count)
          resolve(count)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load table count', error)
          Vue.$toast.error(`[${code}] Failed to load table count: ${message}`)
          reject(error)
        })
    })
  }

  findHistory (id, databaseId, tableId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/${databaseId}/table/${tableId}/history`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const history = response.data
          console.debug('response history', history)
          resolve(history)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load table history', error)
          Vue.$toast.error(`[${code}] Failed to load table history: ${message}`)
          reject(error)
        })
    })
  }

  exportData (id, databaseId, tableId) {
    return this.exportDataTimestamp(id, databaseId, tableId, null)
  }

  exportDataTimestamp (id, databaseId, tableId, timestamp) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/${databaseId}/table/${tableId}/export?timestamp=${timestamp}`, { responseType: 'text' })
        .then((response) => {
          const data = response.data
          console.debug('response data', data)
          resolve(data)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to export table data', error)
          Vue.$toast.error(`[${code}] Failed to export table data: ${message}`)
          reject(error)
        })
    })
  }

  create (id, databaseId, data) {
    return new Promise((resolve, reject) => {
      api.post(`/api/container/${id}/database/${databaseId}/table`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const table = response.data
          console.debug('response table', table)
          resolve(table)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to create table', error)
          Vue.$toast.error(`[${code}] Failed to create table: ${message}`)
          reject(error)
        })
    })
  }

  deleteTuple (id, databaseId, tableId, data) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/container/${id}/database/${databaseId}/table/${tableId}/data`, { headers: { Accept: 'application/json' }, data })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to delete table tuple', error)
          Vue.$toast.error(`[${code}] Failed to delete table tuple: ${message}`)
          reject(error)
        })
    })
  }
}

export default new TableService()
