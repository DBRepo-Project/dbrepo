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
