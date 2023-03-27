const axios = require('axios/dist/browser/axios.cjs')

export function createDatabase (token, containerId) {
  const payload = {
    name: null,
    is_public: true
  }
  return axios.post(`/api/container/${containerId}/database`, payload, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}
