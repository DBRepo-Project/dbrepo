const axios = require('axios/dist/browser/axios.cjs')

export function listTables (token, containerId, databaseId) {
  return axios.get(`/api/container/${containerId}/database/${databaseId}/table`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}

export function createTable (token, containerId, databaseId, payload) {
  return axios.post(`/api/container/${containerId}/database/${databaseId}/table`, payload, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}

export function dataImport (token, containerId, databaseId, tableId, payload) {
  return axios.post(`/api/container/${containerId}/database/${databaseId}/table/${tableId}/data/import`, payload, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}
