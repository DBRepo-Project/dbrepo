const axios = require('axios/dist/browser/axios.cjs')

export function findQuery (token, containerId, databaseId, queryId) {
  return axios.get(`/api/container/${containerId}/database/${databaseId}/query/${queryId}`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}

export function persistQuery (token, containerId, databaseId, queryId) {
  return axios.put(`/api/container/${containerId}/database/${databaseId}/query/${queryId}`, {}, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}
