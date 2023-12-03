import axios from 'axios'
import { displayError } from '@/api/index'

class MiddlewareService {
  buildQuery (data) {
    return new Promise((resolve, reject) => {
      axios.post('/server-middleware/query/build', data, { headers: { 'Content-Type': 'application/json' } })
        .then((response) => {
          const file = response.data
          console.debug('response query', file)
          resolve(file)
        })
        .catch((error) => {
          displayError('Failed to build query', error)
          reject(error)
        })
    })
  }
}

export default new MiddlewareService()
