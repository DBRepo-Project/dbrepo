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
}

export default new TableService()
