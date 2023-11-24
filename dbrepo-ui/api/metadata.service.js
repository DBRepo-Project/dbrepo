import Vue from 'vue'
import api from '@/api'

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
          const { code, message } = error.response.data
          console.error('Failed to load messages', error)
          Vue.$toast.error(`[${code}] Failed to load messages: ${message}`)
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
          const { code, message } = error.response.data
          console.error('Failed to create message', error)
          Vue.$toast.error(`[${code}] Failed to create message: ${message}`)
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
          const { code, message } = error.response.data
          console.error('Failed to find message', error)
          Vue.$toast.error(`[${code}] Failed to find message: ${message}`)
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
          const { code, message } = error.response.data
          console.error('Failed to update message', error)
          Vue.$toast.error(`[${code}] Failed to update message: ${message}`)
          reject(error)
        })
    })
  }

  deleteMessage (id) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/maintenance/message/${id}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error.response.data
          console.error('Failed to delete message', error)
          Vue.$toast.error(`[${code}] Failed to delete message: ${message}`)
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
          const { code, message } = error.response.data
          console.error('Failed to load active messages', error)
          Vue.$toast.error(`[${code}] Failed to load active messages: ${message}`)
          reject(error)
        })
    })
  }
}

export default new MetadataService()
