import api, { displayError } from '@/api'

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
          displayError(error, 'Failed to load databases')
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
          displayError(error, 'Failed to load my databases')
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
          displayError(error, 'Failed to count databases')
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
          displayError(error, 'Failed to load database')
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
          displayError(error, 'Failed to create database')
          reject(error)
        })
    })
  }

  delete (databaseId) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/database/${databaseId}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          displayError(error, 'Failed to delete database')
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
          displayError(error, 'Failed to modify database visibility')
          reject(error)
        })
    })
  }

  modifyImage (databaseId, payload) {
    return new Promise((resolve, reject) => {
      api.put(`/api/database/${databaseId}/image`, payload, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const database = response.data
          console.debug('response database', database)
          resolve(database)
        }).catch((error) => {
          displayError(error, 'Failed to modify database visibility')
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
          displayError(error, 'Failed to modify database owner')
          reject(error)
        })
    })
  }

  checkAccess (databaseId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/database/${databaseId}/access`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const databases = response.data
          console.debug('response databases access', databases)
          resolve(databases)
        })
        .catch((error) => {
          const { status } = error
          if (status !== 401 && status !== 403 && status !== 405) { /* ignore no access errors */
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
          displayError(error, 'Failed to modify database access')
          reject(error)
        })
    })
  }

  revokeAccess (databaseId, userId) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/database/${databaseId}/access/${userId}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          displayError(error, 'Failed to revoke database access')
          reject(error)
        })
    })
  }

  giveAccess (databaseId, userId, type) {
    return new Promise((resolve, reject) => {
      api.post(`/api/database/${databaseId}/access/${userId}`, { type }, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          displayError(error, 'Failed to give database access')
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
          displayError(error, 'Failed to load licenses')
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
          displayError(error, 'Failed to find view')
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
          displayError(error, 'Failed to create view')
          reject(error)
        })
    })
  }

  deleteView (databaseId, viewId) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/database/${databaseId}/view/${viewId}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          displayError(error, 'Failed to delete view')
          reject(error)
        })
    })
  }
}

export default new DatabaseService()
