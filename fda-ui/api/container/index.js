const axios = require('axios/dist/browser/axios.cjs')

export function listContainers (limit) {
  return axios.get(`/api/container?limit=${limit}`)
}

export function createContainer (token, payload) {
  return axios.post('/api/container/', payload, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}

export function startContainer (token, containerId) {
  const payload = {
    action: 'start'
  }
  return axios.put(`/api/container/${containerId}`, payload, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}
