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
    api.get(`/api/container/${id}`, { headers: { Accept: 'application/json' } })
      .then((response) => {
        const container = response.data
        console.debug('response container', container)
        return container
      }).catch((error) => {
        const { code, message } = error
        console.error('Failed to load container', error)
        Vue.$toast.error(`[${code}] Failed to load container: ${message}`)
      })
  }

  create (data) {
    api.post('/api/container', data, { headers: { Accept: 'application/json' } })
      .then((response) => {
        const container = response.data
        console.debug('response container', container)
        return container
      }).catch((error) => {
        const { code, message } = error
        console.error('Failed to create container', error)
        Vue.$toast.error(`[${code}] Failed to create container: ${message}`)
      })
  }

  modify (id, action) {
    api.put(`/api/container/${id}`, { action }, { headers: { Accept: 'application/json' } })
      .then((response) => {
        const container = response.data
        console.debug('response container', container)
        return container
      }).catch((error) => {
        const { code, message } = error
        console.error('Failed to modify container', error)
        Vue.$toast.error(`[${code}] Failed to modify container: ${message}`)
      })
  }
}

export default new ContainerService()
