const axios = require('axios/dist/browser/axios.cjs')

export function determineDataTypes (token, filepath) {
  const payload = {
    filepath
  }
  return axios.post('/api/analyse/determinedt', payload, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}
