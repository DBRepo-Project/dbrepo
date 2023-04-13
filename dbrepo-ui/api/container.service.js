import Vue from 'vue'
import api from '@/api'

class ContainerService {
  findAll (limit = 100) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container?limit=${limit}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const containers = response.data
          console.debug('response containers', containers)
          resolve(containers)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load containers', error)
          Vue.$toast.error(`[${code}] Failed to load containers: ${message}`)
          reject(error)
        })
    })
  }

  findOne (id) {
    return new Promise((resolve, reject) => {
      api.get(`/api/container/${id}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const container = response.data
          console.debug('response container', container)
          resolve(container)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load container', error)
          Vue.$toast.error(`[${code}] Failed to load container: ${message}`)
          reject(error)
        })
    })
  }

  findImage (id) {
    return new Promise((resolve, reject) => {
      api.get(`/api/image/${id}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const image = response.data
          console.debug('response image', image)
          resolve(image)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load image', error)
          Vue.$toast.error(`[${code}] Failed to load image: ${message}`)
          reject(error)
        })
    })
  }

  create (data) {
    return new Promise((resolve, reject) => {
      api.post('/api/container', data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const container = response.data
          console.debug('response container', container)
          resolve(container)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to create container', error)
          Vue.$toast.error(`[${code}] Failed to create container: ${message}`)
          reject(error)
        })
    })
  }

  modify (id, action) {
    return new Promise((resolve, reject) => {
      api.put(`/api/container/${id}`, { action }, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const container = response.data
          console.debug('response container', container)
          resolve(container)
        })
        .catch((error) => {
          const { code, message, response } = error
          const { status } = response
          if (status === 409) {
            console.warn('Failed to modify container', error)
            reject(error)
          } else {
            console.error('Failed to modify container', error)
            Vue.$toast.error(`[${code}] Failed to modify container: ${message}`)
            reject(error)
          }
        })
    })
  }
}

export default new ContainerService()
