import Vue from 'vue'
import api from '@/api'

class DatabaseService {
  findAll () {
    return new Promise((resolve, reject) => {
      api.get('/api/database', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const databases = response.data
          console.debug('response databases', databases)
          resolve(databases)
        })
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to load databases', error)
          Vue.$toast.error(`[${code}] Failed to load databases: ${message}`)
          reject(error)
        })
    })
  }

  findAllOnlyAccess () {
    return new Promise((resolve, reject) => {
      api.get('/api/database?filter=access', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const databases = response.data
          console.debug('response my databases', databases)
          resolve(databases)
        })
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to load my databases', error)
          Vue.$toast.error(`[${code}] Failed to load my databases: ${message}`)
          reject(error)
        })
    })
  }

  countAll (filter) {
    return new Promise((resolve, reject) => {
      api.head(`/api/database${filter ? '?filter=access' : ''}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const count = response.headers.get('x-count')
          console.debug('response count', count)
          resolve(count)
        })
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to count databases', error)
          Vue.$toast.error(`[${code}] Failed to count databases: ${message}`)
          reject(error)
        })
    })
  }

  findOne (databaseId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        }).catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to load database', error)
          Vue.$toast.error(`[${code}] Failed to load database: ${message}`)
          reject(error)
        })
    })
  }

  create (data) {
    return new Promise((resolve, reject) => {
      api.post('/api/database', data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        }).catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to create database', error)
          Vue.$toast.error(`[${code}] Failed to create database: ${message}`)
          reject(error)
        })
    })
  }

  delete (databaseId) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/database/${databaseId}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to delete database', error)
          Vue.$toast.error(`[${code}] Failed to delete database: ${message}`)
          reject(error)
        })
    })
  }

  modifyVisibility (databaseId, isPublic) {
    return new Promise((resolve, reject) => {
      api.put(`/api/database/${databaseId}/visibility`, { is_public: isPublic }, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        }).catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to modify database visibility', error)
          Vue.$toast.error(`[${code}] Failed to modify database visibility: ${message}`)
          reject(error)
        })
    })
  }

  modifyOwner (databaseId, username) {
    return new Promise((resolve, reject) => {
      api.put(`/api/database/${databaseId}/transfer`, { username }, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        }).catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to modify database owner', error)
          Vue.$toast.error(`[${code}] Failed to modify database owner: ${message}`)
          reject(error)
        })
    })
  }

  checkAccess (databaseId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}/access`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const databases = response.data
          console.debug('response databases', databases)
          resolve(databases)
        })
        .catch((error) => {
          const { status } = error
          const { code, message } = error.response.data
          if (status !== 401 && status !== 403 && status !== 405) { /* ignore no access errors */
            console.error('Failed to check database access', error)
            Vue.$toast.error(`[${code}] Failed to check database access: ${message}`)
            reject(error)
          }
        })
    })
  }

  modifyAccess (databaseId, userId, type) {
    return new Promise((resolve, reject) => {
      api.put(`/api/database/${databaseId}/access/${userId}`, { type }, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        })
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to modify database access', error)
          Vue.$toast.error(`[${code}] Failed to modify database access: ${message}`)
          reject(error)
        })
    })
  }

  revokeAccess (databaseId, userId) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/database/${databaseId}/access/${userId}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to revoke database access', error)
          Vue.$toast.error(`[${code}] Failed to revoke database access: ${message}`)
          reject(error)
        })
    })
  }

  giveAccess (databaseId, userId, type) {
    return new Promise((resolve, reject) => {
      api.post(`/api/database/${databaseId}/access/${userId}`, { type }, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to give database access', error)
          Vue.$toast.error(`[${code}] Failed to give database access: ${message}`)
          reject(error)
        })
    })
  }

  findAllLicenses () {
    return new Promise((resolve, reject) => {
      api.get('/api/database/license', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const licenses = response.data
          console.debug('response licenses', licenses)
          resolve(licenses)
        })
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to load licenses', error)
          Vue.$toast.error(`[${code}] Failed to load licenses: ${message}`)
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
          const { code, message } = error.response.data
          console.error('Failed to find view', error)
          Vue.$toast.error(`[${code}] Failed to find view: ${message}`)
          reject(error)
        })
    })
  }

  createView (databaseId, data) {
    return new Promise((resolve, reject) => {
      api.post(`/api/database/${databaseId}/view`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const view = response.data
          console.debug('response view', view)
          resolve(view)
        })
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to delete view', error)
          Vue.$toast.error(`[${code}] Failed to delete view: ${message}`)
          reject(error)
        })
    })
  }

  deleteView (databaseId, viewId) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/database/${databaseId}/view/${viewId}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to delete view', error)
          Vue.$toast.error(`[${code}] Failed to delete view: ${message}`)
          reject(error)
        })
    })
  }
}

export default new DatabaseService()
