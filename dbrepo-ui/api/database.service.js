import Vue from 'vue'
import api from '@/api'

class DatabaseService {
  findAll (id) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const databases = response.data
          console.debug('response databases', databases)
          resolve(databases)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load databases', error)
          Vue.$toast.error(`[${code}] Failed to load databases: ${message}`)
          reject(error)
        })
    })
  }

  findOne (id, databaseId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/${databaseId}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        }).catch((error) => {
          const { code, message } = error
          console.error('Failed to load database', error)
          Vue.$toast.error(`[${code}] Failed to load database: ${message}`)
          reject(error)
        })
    })
  }

  create (id, data) {
    return new Promise((resolve, reject) => {
      api.post(`/api/container/${id}/database`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        }).catch((error) => {
          const { code, message } = error
          console.error('Failed to create database', error)
          Vue.$toast.error(`[${code}] Failed to create database: ${message}`)
          reject(error)
        })
    })
  }

  delete (id, databaseId) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/container/${id}/database/${databaseId}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to delete database', error)
          Vue.$toast.error(`[${code}] Failed to delete database: ${message}`)
          reject(error)
        })
    })
  }

  modifyVisibility (id, databaseId, isPublic) {
    return new Promise((resolve, reject) => {
      api.put(`/api/container/${id}/database/${databaseId}/visibility`, { is_public: isPublic }, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        }).catch((error) => {
          const { code, message } = error
          console.error('Failed to modify database visibility', error)
          Vue.$toast.error(`[${code}] Failed to modify database visibility: ${message}`)
          reject(error)
        })
    })
  }

  modifyOwner (id, databaseId, username) {
    return new Promise((resolve, reject) => {
      api.put(`/api/container/${id}/database/${databaseId}/transfer`, { username }, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        }).catch((error) => {
          const { code, message } = error
          console.error('Failed to modify database owner', error)
          Vue.$toast.error(`[${code}] Failed to modify database owner: ${message}`)
          reject(error)
        })
    })
  }

  checkAccess (id, databaseId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/${databaseId}/access`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const databases = response.data
          console.debug('response databases', databases)
          resolve(databases)
        })
        .catch((error) => {
          const { code, message, response } = error
          const { status } = response
          if (status !== 403 && status !== 405) { /* ignore no access errors */
            console.error('Failed to check database access', error)
            Vue.$toast.error(`[${code}] Failed to check database access: ${message}`)
            reject(error)
          }
        })
    })
  }

  modifyAccess (id, databaseId, username, type) {
    return new Promise((resolve, reject) => {
      api.put(`/api/container/${id}/database/${databaseId}/access/${username}`, { type }, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to modify database access', error)
          Vue.$toast.error(`[${code}] Failed to modify database access: ${message}`)
          reject(error)
        })
    })
  }

  revokeAccess (id, databaseId, username) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/container/${id}/database/${databaseId}/access/${username}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to revoke database access', error)
          Vue.$toast.error(`[${code}] Failed to revoke database access: ${message}`)
          reject(error)
        })
    })
  }

  giveAccess (id, databaseId, username, type) {
    return new Promise((resolve, reject) => {
      api.post(`/api/container/${id}/database/${databaseId}/access/${username}`, { username, type }, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to give database access', error)
          Vue.$toast.error(`[${code}] Failed to give database access: ${message}`)
          reject(error)
        })
    })
  }

  findAllLicenses (id) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}/database/license`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const licenses = response.data
          console.debug('response licenses', licenses)
          resolve(licenses)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load licenses', error)
          Vue.$toast.error(`[${code}] Failed to load licenses: ${message}`)
          reject(error)
        })
    })
  }

  createView (id, databaseId, data) {
    return new Promise((resolve, reject) => {
      api.post(`/api/container/${id}/database/${databaseId}/view`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const view = response.data
          console.debug('response view', view)
          resolve(view)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to delete view', error)
          Vue.$toast.error(`[${code}] Failed to delete view: ${message}`)
          reject(error)
        })
    })
  }

  deleteView (id, databaseId, viewId) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/container/${id}/database/${databaseId}/view/${viewId}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to delete view', error)
          Vue.$toast.error(`[${code}] Failed to delete view: ${message}`)
          reject(error)
        })
    })
  }
}

export default new DatabaseService()
