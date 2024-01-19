import api, { displayError } from '@/api'

class IdentifierService {
  findAll (databaseId, type) {
    return new Promise((resolve, reject) => {
      const delim = databaseId !== null && type !== null ? '&' : '?'
      api.get(`/api/identifier${databaseId !== null ? `?dbid=${databaseId}` : ''}${type !== null ? `${delim}type=${type}` : ''}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const identifiers = response.data
          console.debug('response identifiers', identifiers)
          resolve(identifiers)
        })
        .catch((error) => {
          displayError('Failed to load identifiers', error)
          reject(error)
        })
    })
  }

  retrieve (url) {
    return new Promise((resolve, reject) => {
      if (url === null) {
        reject(Error)
      }
      api.get(`/api/identifier/retrieve?url=${url}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const { status, data } = response
          if (status === 200) {
            console.debug('response metadata', data)
            resolve(data)
          } else {
            console.error('response metadata', response)
            reject(response)
          }
        })
        .catch((error) => {
          displayError('Failed to load identifier', error)
          reject(error)
        })
    })
  }

  findAccept (id, accept) {
    return new Promise((resolve, reject) => {
      api.get(`/api/pid/${id}`, { headers: { Accept: accept } })
        .then((response) => {
          const identifier = response.data
          console.debug('response identifier', identifier)
          resolve(identifier)
        })
        .catch((error) => {
          displayError('Failed to load citation recommendation', error)
          reject(error)
        })
    })
  }

  create (data) {
    return new Promise((resolve, reject) => {
      api.post('/api/identifier', data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const identifier = response.data
          console.debug('response identifier', identifier)
          resolve(identifier)
        })
        .catch((error) => {
          displayError('Failed to create identifier', error)
          reject(error)
        })
    })
  }

  update (id, data) {
    return new Promise((resolve, reject) => {
      api.put(`/api/pid/${id}`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const identifier = response.data
          console.debug('response identifier', identifier)
          resolve(identifier)
        })
        .catch((error) => {
          displayError('Failed to update identifier', error)
          reject(error)
        })
    })
  }

  export (pid) {
    return new Promise((resolve, reject) => {
      api.get(`/api/pid/${pid}`, { headers: { Accept: 'text/xml' } })
        .then((response) => {
          const identifier = response.data
          console.debug('response identifier', identifier)
          resolve(identifier)
        })
        .catch((error) => {
          displayError('Failed to export identifier', error)
          reject(error)
        })
    })
  }

  delete (pid) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/pid/${pid}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          displayError('Failed to delete identifier', error)
          reject(error)
        })
    })
  }
}

export default new IdentifierService()
