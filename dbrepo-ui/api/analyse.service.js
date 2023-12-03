import api, { displayError } from '@/api'

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
          displayError('Failed to load analysis', error)
          reject(error)
        })
    })
  }
}

export default new AnalyseService()
