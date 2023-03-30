const axios = require('axios/dist/browser/axios.cjs')

export function createDatabase (token, container) {
  const payload = {
    name: container.name,
    is_public: container.is_public ? container.is_public : true
  }
  return axios.post(`/api/container/${container.id}/database`, payload, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}

export function modifyVisibility (token, containerId, databaseId, isPublic) {
  const payload = {
    is_public: isPublic
  }
  return axios.put(`/api/container/${containerId}/database/${databaseId}/visibility`, payload, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}

export function findDatabase (token, containerId, databaseId) {
  return axios.get(`/api/container/${containerId}/database/${databaseId}`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}
