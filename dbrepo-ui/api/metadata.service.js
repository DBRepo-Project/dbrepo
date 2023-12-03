import api, { displayError } from '@/api'

class MetadataService {
  findAllMessages () {
    return new Promise((resolve, reject) => {
      api.get('/api/maintenance/message', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const messages = response.data
          console.debug('response messages', messages)
          resolve(messages)
        })
        .catch((error) => {
          displayError('Failed to load maintenance messages', error)
          reject(error)
        })
    })
  }

  createMessage (data) {
    return new Promise((resolve, reject) => {
      api.post('/api/maintenance/message', data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const messages = response.data
          console.debug('response message', messages)
          resolve(messages)
        })
        .catch((error) => {
          displayError('Failed to create maintenance message', error)
          reject(error)
        })
    })
  }

  findMessage (id) {
    return new Promise((resolve, reject) => {
      api.get(`/api/maintenance/message/${id}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const messages = response.data
          console.debug('response message', messages)
          resolve(messages)
        })
        .catch((error) => {
          displayError('Failed to find maintenance message', error)
          reject(error)
        })
    })
  }

  updateMessage (id, data) {
    return new Promise((resolve, reject) => {
      api.put(`/api/maintenance/message/${id}`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const messages = response.data
          console.debug('response message', messages)
          resolve(messages)
        })
        .catch((error) => {
          displayError('Failed to update maintenance message', error)
          reject(error)
        })
    })
  }

  deleteMessage (id) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/maintenance/message/${id}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          displayError('Failed to delete maintenance message', error)
          reject(error)
        })
    })
  }

  findActiveMessages () {
    return new Promise((resolve, reject) => {
      api.get('/api/maintenance/message/active', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const messages = response.data
          console.debug('response messages', messages)
          resolve(messages)
        })
        .catch((error) => {
          displayError('Failed to load active maintenance messages', error)
          reject(error)
        })
    })
  }
}

export default new MetadataService()
