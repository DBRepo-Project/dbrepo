import api, { displayError } from '@/api'

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
          displayError('Failed to load container', error)
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
          displayError('Failed to load container', error)
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
          displayError('Failed to load image', error)
          reject(error)
        })
    })
  }
}

export default new ContainerService()
