import Vue from 'vue'
import api from '@/api'

class AnalyseService {
  determineDataTypes (filename, separator) {
    return new Promise((resolve, reject) => {
      const payload = {
        filename,
        separator
      }
      api.post('/api/analyse/determinedt', payload, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const analysis = response.data
          console.debug('response analysis', analysis)
          resolve(analysis)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load analysis', error)
          Vue.$toast.error(`[${code}] Failed to load analysis: ${message}`)
          reject(error)
        })
    })
  }
}

export default new AnalyseService()
